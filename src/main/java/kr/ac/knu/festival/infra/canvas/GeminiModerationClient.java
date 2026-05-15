package kr.ac.knu.festival.infra.canvas;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
public class GeminiModerationClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String PATH = "/v1beta/models/{model}:generateContent";

    /** connect 5s — TCP/TLS 연결까지 허용 시간. */
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    /** read 10s — Gemini 응답 수신까지 허용 시간. */
    private static final int READ_TIMEOUT_MS = 10_000;

    /** 재시도 횟수: 첫 시도 + 1회 재시도 = 총 2회 attempt. */
    private static final int MAX_ATTEMPTS = 2;
    /** 재시도 backoff: 300ms. */
    private static final long RETRY_BACKOFF_MS = 300L;

    private static final String METRIC_FAIL_OPEN = "festival.canvas.moderation.fail_open";

    private final GeminiProperties properties;
    private final RestClient restClient;
    private final MeterRegistry meterRegistry;

    public GeminiModerationClient(
            GeminiProperties properties,
            RestClient.Builder restClientBuilder,
            MeterRegistry meterRegistry) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MS);
        this.restClient = restClientBuilder
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Gemini 검열 결과 판정.
     *
     * <p>응답 텍스트가 trim 후 대소문자 무시 "REJECT"와 완전 일치할 때만 부적절(false)로 본다.
     * "PROJECT", "REJECTED", "REJECTION" 등 부분 일치는 모두 적절(true)로 처리한다.
     * 응답 구조 변형/네트워크 오류/NPE/IOOBE 등 모든 예외는 fail-open(true)으로 흡수한다.
     *
     * @return true = 적절(게시 유지), false = 부적절(거부)
     */
    public boolean isAppropriate(String message) {
        if (!StringUtils.hasText(properties.apiKey())) {
            log.debug("Gemini API key 미설정 — 검열 건너뜀");
            return true;
        }

        String prompt = properties.moderationPrompt() + "\n\n메시지: " + message;
        GeminiRequest request = new GeminiRequest(
                List.of(new Content(List.of(new Part(prompt)))),
                new GenerationConfig(0.1, 10)
        );

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                GeminiResponse response = restClient.post()
                        .uri(PATH + "?key={key}", properties.model(), properties.apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(GeminiResponse.class);

                String text = extractText(response);
                if (text == null) {
                    log.warn("[gemini-fail-open] reason=empty-or-null-response attempt={}", attempt);
                    incrementFailOpen("PARSE_ERROR");
                    return true;
                }

                String normalized = text.trim();
                log.debug("Gemini 검열 결과: {} attempt={}", normalized, attempt);
                // 정확히 "REJECT" (대소문자 무시) 일 때만 거부. 부분 일치 차단.
                return !normalized.equalsIgnoreCase("REJECT");

            } catch (HttpServerErrorException ex) {
                if (attempt == 0) {
                    log.debug("[gemini-retry] reason=SERVER_ERROR status={} attempt={}",
                            ex.getStatusCode(), attempt);
                    if (!sleepBackoff()) {
                        break;
                    }
                    continue;
                }
                log.warn("[gemini-fail-open] retry exhausted: {} attempt={}", ex.toString(), attempt);
                incrementFailOpen("SERVER_ERROR");
                return true;
            } catch (ResourceAccessException ex) {
                if (attempt == 0) {
                    log.debug("[gemini-retry] reason=TIMEOUT attempt={}", attempt);
                    if (!sleepBackoff()) {
                        break;
                    }
                    continue;
                }
                log.warn("[gemini-fail-open] retry exhausted: {} attempt={}", ex.toString(), attempt);
                incrementFailOpen("TIMEOUT");
                return true;
            } catch (Exception ex) {
                log.warn("[gemini-fail-open] non-retryable: {}", ex.toString());
                incrementFailOpen("OTHER");
                return true;
            }
        }
        incrementFailOpen("OTHER");
        return true;
    }

    /**
     * 재시도 backoff sleep. 인터럽트 발생 시 false 반환하여 즉시 fail-open 으로 전환한다.
     */
    private boolean sleepBackoff() {
        try {
            Thread.sleep(RETRY_BACKOFF_MS);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void incrementFailOpen(String reason) {
        Counter.builder(METRIC_FAIL_OPEN)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Gemini 응답 트리에서 첫 번째 텍스트를 안전하게 추출.
     * 응답 스키마 변경/누락 대비 NPE·IndexOutOfBoundsException 흡수.
     *
     * @return 텍스트 또는 null
     */
    private String extractText(GeminiResponse response) {
        try {
            if (response == null) {
                return null;
            }
            List<Candidate> candidates = response.candidates();
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }
            Candidate first = candidates.get(0);
            if (first == null || first.content() == null) {
                return null;
            }
            List<Part> parts = first.content().parts();
            if (parts == null || parts.isEmpty()) {
                return null;
            }
            Part part = parts.get(0);
            if (part == null) {
                return null;
            }
            return part.text();
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            log.warn("[gemini-fail-open] reason=schema-mismatch detail={}", e.getMessage());
            return null;
        }
    }

    // ---- Request DTOs ----

    private record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {}

    private record Content(List<Part> parts) {}

    private record Part(String text) {}

    private record GenerationConfig(double temperature, int maxOutputTokens) {}

    // ---- Response DTOs ----

    private record GeminiResponse(List<Candidate> candidates) {}

    private record Candidate(Content content) {}
}

package kr.ac.knu.festival.infra.canvas;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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

    private final GeminiProperties properties;
    private final RestClient restClient;

    public GeminiModerationClient(GeminiProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
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

        try {
            GeminiResponse response = restClient.post()
                    .uri(PATH + "?key={key}", properties.model(), properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);

            String text = extractText(response);
            if (text == null) {
                log.warn("[gemini-fail-open] reason=empty-or-null-response");
                return true;
            }

            String normalized = text.trim();
            log.debug("Gemini 검열 결과: {}", normalized);
            // 정확히 "REJECT" (대소문자 무시) 일 때만 거부. 부분 일치 차단.
            return !normalized.equalsIgnoreCase("REJECT");

        } catch (Exception e) {
            log.warn("[gemini-fail-open] reason={} message={}", e.getClass().getSimpleName(), e.getMessage());
            return true;
        }
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

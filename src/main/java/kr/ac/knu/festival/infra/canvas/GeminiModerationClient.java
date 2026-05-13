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

    private final GeminiProperties properties;
    private final RestClient restClient;

    public GeminiModerationClient(GeminiProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3_000);
        requestFactory.setReadTimeout(5_000);
        this.restClient = restClientBuilder
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * @return true = 적절 (게시 유지), false = 부적절 (삭제)
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

            if (response == null
                    || response.candidates() == null
                    || response.candidates().isEmpty()) {
                log.warn("Gemini 응답이 비어있음 — 검열 통과 처리");
                return true;
            }

            String text = response.candidates().getFirst()
                    .content().parts().getFirst().text()
                    .trim().toUpperCase();
            log.debug("Gemini 검열 결과: {}", text);
            return !text.contains("REJECT");

        } catch (Exception e) {
            log.warn("Gemini API 호출 실패 — 검열 통과 처리: {}", e.getMessage());
            return true;
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

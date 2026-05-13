package kr.ac.knu.festival.infra.canvas;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        String model,
        String moderationPrompt
) {}

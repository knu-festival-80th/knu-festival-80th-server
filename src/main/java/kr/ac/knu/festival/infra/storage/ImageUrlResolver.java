package kr.ac.knu.festival.infra.storage;

import kr.ac.knu.festival.global.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageUrlResolver {

    private final AppProperties appProperties;

    /**
     * DB에 저장된 값(상대 경로 또는 절대 URL)을 클라이언트에 내려보낼 완전한 URL로 변환한다.
     * - null/blank → null
     * - http(s):// 로 시작 → 그대로 (외부 URL 호환)
     * - 그 외 → publicBaseUrl + path
     */
    public String toPublicUrl(String stored) {
        if (stored == null) return null;
        String trimmed = stored.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        String base = stripTrailingSlash(appProperties.getPublicBaseUrl());
        String suffix = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
        return base + suffix;
    }

    private static String stripTrailingSlash(String url) {
        if (url == null || url.isEmpty()) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

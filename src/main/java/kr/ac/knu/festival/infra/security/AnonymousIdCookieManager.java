package kr.ac.knu.festival.infra.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class AnonymousIdCookieManager {

    private static final String COOKIE_NAME = "ANON_ID";
    private static final Duration COOKIE_MAX_AGE = Duration.ofDays(14);

    /**
     * SHA-256 MessageDigest 는 thread-safe 하지 않으므로 매 호출 인스턴스화 대신 ThreadLocal 로 캐싱한다.
     * 사용 직전 반드시 {@link MessageDigest#reset()} 을 호출해 이전 상태를 비운다.
     */
    private static final ThreadLocal<MessageDigest> SHA256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    });

    @Value("${cookie.secure:true}")
    private boolean cookieSecure;

    public String getOrCreateHashedAnonymousId(HttpServletRequest request, HttpServletResponse response) {
        String anonymousId = findCookieValue(request);
        if (anonymousId == null || anonymousId.isBlank()) {
            anonymousId = UUID.randomUUID().toString();
            ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, anonymousId)
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(COOKIE_MAX_AGE)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return hash(anonymousId);
    }

    public String getHashedAnonymousId(HttpServletRequest request) {
        String anonymousId = findCookieValue(request);
        if (anonymousId == null || anonymousId.isBlank()) {
            return null;
        }
        return hash(anonymousId);
    }

    private String findCookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String hash(String value) {
        try {
            MessageDigest digest = SHA256.get();
            digest.reset();
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash anonymous id", e);
        }
    }
}

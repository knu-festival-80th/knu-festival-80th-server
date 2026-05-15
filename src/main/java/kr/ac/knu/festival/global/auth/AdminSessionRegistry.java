package kr.ac.knu.festival.global.auth;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 부스 운영진 세션을 부스 ID 별로 추적해 비밀번호 변경 시 일괄 만료를 지원한다.
 * <p>
 * - 로그인 직후 {@link #register(Long, HttpSession)} 으로 등록.
 * - 부스 비번 변경 시 {@link #invalidateAllForBooth(Long)} 으로 해당 부스의 모든 세션 invalidate.
 * - 세션 만료/만료자 정리 시 자동으로 추적 맵에서 제거 (HttpSessionListener).
 * <p>
 * SUPER_ADMIN 은 boothId 가 null 이라 등록 대상이 아니다 (정책상 슈퍼 비번 변경 시에는 별도 처리 불요).
 */
@Slf4j
@Component
public class AdminSessionRegistry implements HttpSessionListener {

    private static final String SESSION_BOOTH_ID_KEY = "adminSessionRegistry.boothId";

    private final Map<Long, Set<HttpSession>> sessionsByBoothId = new ConcurrentHashMap<>();

    public void register(Long boothId, HttpSession session) {
        if (boothId == null || session == null) {
            return;
        }
        session.setAttribute(SESSION_BOOTH_ID_KEY, boothId);
        sessionsByBoothId
                .computeIfAbsent(boothId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(session);
    }

    public void invalidateAllForBooth(Long boothId) {
        if (boothId == null) {
            return;
        }
        Set<HttpSession> sessions = sessionsByBoothId.remove(boothId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (HttpSession session : sessions) {
            try {
                session.invalidate();
            } catch (IllegalStateException ignored) {
                // 이미 만료된 세션이면 무시
            } catch (Exception e) {
                log.warn("Session invalidate failed for boothId={}: {}", boothId, e.getMessage());
            }
        }
    }

    private void unregister(HttpSession session) {
        if (session == null) {
            return;
        }
        Object stored;
        try {
            stored = session.getAttribute(SESSION_BOOTH_ID_KEY);
        } catch (IllegalStateException ignored) {
            return;
        }
        if (!(stored instanceof Long boothId)) {
            return;
        }
        Set<HttpSession> sessions = sessionsByBoothId.get(boothId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                sessionsByBoothId.remove(boothId, sessions);
            }
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        unregister(se.getSession());
    }
}

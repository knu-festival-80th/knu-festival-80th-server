package kr.ac.knu.festival.global.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SessionAuthFilter extends OncePerRequestFilter {

    public static final String SESSION_ADMIN_KEY = "adminInfo";

    private final AdminSessionRegistry adminSessionRegistry;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object principal = session.getAttribute(SESSION_ADMIN_KEY);
            if (principal instanceof AdminInfo adminInfo) {
                var authentication = new UsernamePasswordAuthenticationToken(
                        adminInfo,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + adminInfo.role()))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                // BOOTH_ADMIN 세션은 비번 변경 시 일괄 만료가 필요하므로 추적 대상으로 등록.
                // register 는 Set 기반이라 idempotent. SUPER_ADMIN 은 boothId 가 null 이라 자동으로 무시된다.
                if (!adminInfo.isSuperAdmin() && adminInfo.boothId() != null) {
                    adminSessionRegistry.register(adminInfo.boothId(), session);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}

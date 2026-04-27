package kr.ac.knu.festival.presentation.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kr.ac.knu.festival.application.auth.AuthService;
import kr.ac.knu.festival.global.auth.AdminInfo;
import kr.ac.knu.festival.global.auth.SessionAuthFilter;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.auth.controller.docs.AuthControllerDocs;
import kr.ac.knu.festival.presentation.auth.dto.request.LoginRequest;
import kr.ac.knu.festival.presentation.auth.dto.response.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/auth")
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;

    @Override
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        LoginResponse result = authService.login(request);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(SessionAuthFilter.SESSION_ADMIN_KEY,
                new AdminInfo(result.role(), result.boothId()));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(ApiResponse.success());
    }
}

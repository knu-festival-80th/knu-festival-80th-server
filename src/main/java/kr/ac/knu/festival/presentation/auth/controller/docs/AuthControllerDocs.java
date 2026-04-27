package kr.ac.knu.festival.presentation.auth.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.auth.dto.request.LoginRequest;
import kr.ac.knu.festival.presentation.auth.dto.response.LoginResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "인증", description = "관리자 로그인/로그아웃 (세션 기반)")
public interface AuthControllerDocs {

    @Operation(summary = "관리자 로그인",
            description = "boothId 가 null 이면 최고 관리자(SUPER_ADMIN), 있으면 해당 부스 관리자(BOOTH_ADMIN). 성공 시 세션 쿠키(JSESSIONID) 발급.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "비밀번호 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "부스 없음")
    })
    ResponseEntity<ApiResponse<LoginResponse>> login(LoginRequest request,
                                                     @Parameter(hidden = true) HttpServletRequest httpRequest);

    @Operation(summary = "관리자 로그아웃", description = "세션을 무효화합니다.")
    ResponseEntity<ApiResponse<Void>> logout(@Parameter(hidden = true) HttpServletRequest httpRequest);
}

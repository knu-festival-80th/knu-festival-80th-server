package kr.ac.knu.festival.application.auth;

import kr.ac.knu.festival.domain.booth.entity.Booth;
import kr.ac.knu.festival.domain.booth.repository.BoothRepository;
import kr.ac.knu.festival.global.auth.AdminInfo;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import kr.ac.knu.festival.presentation.auth.dto.request.LoginRequest;
import kr.ac.knu.festival.presentation.auth.dto.response.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    /**
     * 부스가 없거나 비번이 비어있을 때도 응답 시간을 정규화하기 위해 1회 검증하는 더미 BCrypt hash.
     * 실제 비번과는 절대 매칭되지 않는다.
     */
    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$abcdefghijklmnopqrstuOcw/U4Imk3hQ7yMlEjuzG8mSdcG8ZWmu";

    private final BoothRepository boothRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.master-password:${ADMIN_MASTER_PASSWORD}}")
    private String masterPassword;

    public LoginResponse login(LoginRequest request) {
        if (request.boothId() == null) {
            return loginAsSuperAdmin(request.password());
        }
        return loginAsBoothAdmin(request.boothId(), request.password());
    }

    private LoginResponse loginAsSuperAdmin(String rawPassword) {
        if (!constantTimeEquals(masterPassword, rawPassword)) {
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED_USER);
        }
        return new LoginResponse(AdminInfo.ROLE_SUPER_ADMIN, null);
    }

    private LoginResponse loginAsBoothAdmin(Long boothId, String rawPassword) {
        Booth booth = boothRepository.findById(boothId).orElse(null);
        // 부스가 없거나 비번이 비어 있어도 동일한 BCrypt 비용을 지불해 응답 시간 차이로 존재 여부가 노출되지 않게 한다.
        if (booth == null || booth.getAdminPassword() == null) {
            passwordEncoder.matches(rawPassword == null ? "" : rawPassword, DUMMY_BCRYPT_HASH);
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED_USER);
        }
        if (rawPassword == null || !passwordEncoder.matches(rawPassword, booth.getAdminPassword())) {
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED_USER);
        }
        return new LoginResponse(AdminInfo.ROLE_BOOTH_ADMIN, boothId);
    }

    /**
     * 길이가 다르면 즉시 false 를 반환하지만, 같은 길이일 때는 MessageDigest.isEqual 의 상수 시간 비교를 사용해
     * 비번 부분 일치 여부가 응답 시간 측면 채널로 새지 않게 한다.
     */
    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }
}

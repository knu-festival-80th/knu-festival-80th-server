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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

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
        if (!rawPassword.equals(masterPassword)) {
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED_USER);
        }
        return new LoginResponse(AdminInfo.ROLE_SUPER_ADMIN, null);
    }

    private LoginResponse loginAsBoothAdmin(Long boothId, String rawPassword) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));
        if (booth.getAdminPassword() == null || !passwordEncoder.matches(rawPassword, booth.getAdminPassword())) {
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED_USER);
        }
        return new LoginResponse(AdminInfo.ROLE_BOOTH_ADMIN, boothId);
    }
}

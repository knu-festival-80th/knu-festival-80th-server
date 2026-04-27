package kr.ac.knu.festival.global.auth;

import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;

import java.util.Objects;

public record AdminInfo(
        String role,
        Long boothId
) {
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_BOOTH_ADMIN = "BOOTH_ADMIN";

    public boolean isSuperAdmin() {
        return ROLE_SUPER_ADMIN.equals(role);
    }

    public void validateBoothAccess(Long targetBoothId) {
        if (isSuperAdmin()) {
            return;
        }
        if (!Objects.equals(boothId, targetBoothId)) {
            throw new BusinessException(BusinessErrorCode.ACCESS_DENIED);
        }
    }

    public void requireSuperAdmin() {
        if (!isSuperAdmin()) {
            throw new BusinessException(BusinessErrorCode.ACCESS_DENIED);
        }
    }
}

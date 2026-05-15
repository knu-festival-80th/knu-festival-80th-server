package kr.ac.knu.festival.application.auth;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import kr.ac.knu.festival.domain.booth.entity.Booth;
import kr.ac.knu.festival.domain.booth.repository.BoothRepository;
import kr.ac.knu.festival.global.auth.AdminInfo;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import kr.ac.knu.festival.presentation.auth.dto.request.LoginRequest;
import kr.ac.knu.festival.presentation.auth.dto.response.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
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

    private static final String FAILURE_REASON_UNKNOWN_BOOTH = "UNKNOWN_BOOTH";
    private static final String FAILURE_REASON_BAD_PASSWORD = "BAD_PASSWORD";
    private static final String FAILURE_REASON_NO_PASSWORD = "NO_PASSWORD";
    private static final String FAILURE_REASON_BAD_MASTER = "BAD_MASTER";

    private final BoothRepository boothRepository;
    private final PasswordEncoder passwordEncoder;
    private final MeterRegistry meterRegistry;

    private Counter superAdminSuccessCounter;
    private Counter boothAdminSuccessCounter;
    private Counter unknownBoothFailureCounter;
    private Counter badPasswordFailureCounter;
    private Counter noPasswordFailureCounter;
    private Counter badMasterFailureCounter;

    @Value("${admin.master-password:${ADMIN_MASTER_PASSWORD}}")
    private String masterPassword;

    @PostConstruct
    void initMetrics() {
        this.superAdminSuccessCounter = Counter.builder("festival.auth.success")
                .tag("role", AdminInfo.ROLE_SUPER_ADMIN)
                .register(meterRegistry);
        this.boothAdminSuccessCounter = Counter.builder("festival.auth.success")
                .tag("role", AdminInfo.ROLE_BOOTH_ADMIN)
                .register(meterRegistry);
        this.unknownBoothFailureCounter = Counter.builder("festival.auth.failure")
                .tag("reason", FAILURE_REASON_UNKNOWN_BOOTH)
                .register(meterRegistry);
        this.badPasswordFailureCounter = Counter.builder("festival.auth.failure")
                .tag("reason", FAILURE_REASON_BAD_PASSWORD)
                .register(meterRegistry);
        this.noPasswordFailureCounter = Counter.builder("festival.auth.failure")
                .tag("reason", FAILURE_REASON_NO_PASSWORD)
                .register(meterRegistry);
        this.badMasterFailureCounter = Counter.builder("festival.auth.failure")
                .tag("reason", FAILURE_REASON_BAD_MASTER)
                .register(meterRegistry);
    }

    public LoginResponse login(LoginRequest request) {
        if (request.boothId() == null) {
            return loginAsSuperAdmin(request.password());
        }
        return loginAsBoothAdmin(request.boothId(), request.password());
    }

    private LoginResponse loginAsSuperAdmin(String rawPassword) {
        if (!constantTimeEquals(masterPassword, rawPassword)) {
            badMasterFailureCounter.increment();
            log.warn("[auth-failure] type={} boothId={} reason={}",
                    "SUPER_ADMIN", null, FAILURE_REASON_BAD_MASTER);
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED_USER);
        }
        superAdminSuccessCounter.increment();
        return new LoginResponse(AdminInfo.ROLE_SUPER_ADMIN, null);
    }

    private LoginResponse loginAsBoothAdmin(Long boothId, String rawPassword) {
        Booth booth = boothRepository.findById(boothId).orElse(null);
        // 부스가 없거나 비번이 비어 있어도 동일한 BCrypt 비용을 지불해 응답 시간 차이로 존재 여부가 노출되지 않게 한다.
        if (booth == null) {
            passwordEncoder.matches(rawPassword == null ? "" : rawPassword, DUMMY_BCRYPT_HASH);
            unknownBoothFailureCounter.increment();
            log.warn("[auth-failure] type={} boothId={} reason={}",
                    "BOOTH_ADMIN", boothId, FAILURE_REASON_UNKNOWN_BOOTH);
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED_USER);
        }
        if (booth.getAdminPassword() == null) {
            passwordEncoder.matches(rawPassword == null ? "" : rawPassword, DUMMY_BCRYPT_HASH);
            noPasswordFailureCounter.increment();
            log.warn("[auth-failure] type={} boothId={} reason={}",
                    "BOOTH_ADMIN", boothId, FAILURE_REASON_NO_PASSWORD);
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED_USER);
        }
        if (rawPassword == null || !passwordEncoder.matches(rawPassword, booth.getAdminPassword())) {
            badPasswordFailureCounter.increment();
            log.warn("[auth-failure] type={} boothId={} reason={}",
                    "BOOTH_ADMIN", boothId, FAILURE_REASON_BAD_PASSWORD);
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED_USER);
        }
        boothAdminSuccessCounter.increment();
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

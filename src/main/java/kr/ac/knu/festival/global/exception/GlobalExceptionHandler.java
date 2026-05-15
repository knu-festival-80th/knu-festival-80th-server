package kr.ac.knu.festival.global.exception;

import jakarta.validation.ConstraintViolationException;
import kr.ac.knu.festival.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("BusinessException: {} - {}", errorCode.getCode(), e.getMessage());
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(BusinessErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(BusinessErrorCode.INVALID_INPUT_VALUE, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        return ResponseEntity.status(BusinessErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(BusinessErrorCode.INVALID_INPUT_VALUE, e.getMessage()));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e) {
        return ResponseEntity.status(BusinessErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(BusinessErrorCode.INVALID_INPUT_VALUE, e.getMessage()));
    }

    // 잘못된 enum 값 등으로 Jackson 역직렬화가 실패하는 경우 catch-all 500 대신 400 으로 응답.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.status(BusinessErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(BusinessErrorCode.INVALID_INPUT_VALUE, "요청 본문을 해석할 수 없습니다."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(BusinessErrorCode.RESOURCE_NOT_FOUND.getStatus())
                .body(ApiResponse.error(BusinessErrorCode.RESOURCE_NOT_FOUND));
    }

    // 401 — Spring Security 인증 실패
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException e) {
        log.warn("AuthenticationException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(BusinessErrorCode.UNAUTHORIZED_USER));
    }

    // 403 — Spring Security 인가 실패
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("AccessDeniedException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(BusinessErrorCode.ACCESS_DENIED));
    }

    // 413 — multipart 업로드 크기 초과
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("MaxUploadSizeExceededException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(BusinessErrorCode.PAYLOAD_TOO_LARGE));
    }

    // 409 — DB unique constraint 위반 (matching, waiting 등)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String msg = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
        log.warn("DataIntegrityViolationException: {}", msg);
        BusinessErrorCode code;
        if (msg != null && msg.contains("uk_matching_instagram_day")) {
            code = BusinessErrorCode.MATCHING_DUPLICATE_REGISTRATION;
        } else if (msg != null && msg.contains("uk_matching_phone_day")) {
            code = BusinessErrorCode.MATCHING_DUPLICATE_PHONE;
        } else {
            code = BusinessErrorCode.DUPLICATE_RESOURCE;
        }
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.error(code));
    }

    // 409 — JPA optimistic lock 실패
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockFailure(OptimisticLockingFailureException e) {
        log.warn("OptimisticLockingFailureException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(BusinessErrorCode.DUPLICATE_RESOURCE, "동시 변경이 감지되었습니다. 다시 시도해주세요."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(BusinessErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.error(BusinessErrorCode.INTERNAL_SERVER_ERROR));
    }
}

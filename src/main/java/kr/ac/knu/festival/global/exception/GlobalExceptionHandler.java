package kr.ac.knu.festival.global.exception;

import jakarta.validation.ConstraintViolationException;
import kr.ac.knu.festival.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(BusinessErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.error(BusinessErrorCode.INTERNAL_SERVER_ERROR));
    }
}

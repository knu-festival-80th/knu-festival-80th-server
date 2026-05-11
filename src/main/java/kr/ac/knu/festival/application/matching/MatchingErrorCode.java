package kr.ac.knu.festival.application.matching;

import kr.ac.knu.festival.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MatchingErrorCode implements ErrorCode {

    MATCHING_RESULT_RATE_LIMITED(
            HttpStatus.TOO_MANY_REQUESTS,
            "M001",
            "매칭 결과 조회 실패 횟수가 너무 많습니다. 잠시 후 다시 시도해주세요."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}

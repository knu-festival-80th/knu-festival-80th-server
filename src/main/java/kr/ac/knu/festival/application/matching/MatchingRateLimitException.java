package kr.ac.knu.festival.application.matching;

import kr.ac.knu.festival.global.exception.BusinessException;

public class MatchingRateLimitException extends BusinessException {

    public MatchingRateLimitException() {
        super(MatchingErrorCode.MATCHING_RESULT_RATE_LIMITED);
    }
}

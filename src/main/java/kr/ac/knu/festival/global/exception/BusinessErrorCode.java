package kr.ac.knu.festival.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BusinessErrorCode implements ErrorCode {

    /*
     * 400 BAD_REQUEST
     */
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    MISSING_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C002", "필수 입력값이 누락되었습니다."),
    BOOTH_HAS_ACTIVE_WAITINGS(HttpStatus.BAD_REQUEST, "B003", "대기 중인 팀이 있어 부스를 삭제할 수 없습니다."),
    INVALID_WAITING_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "W001", "허용되지 않는 대기 상태 전환입니다."),
    INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "W002", "전화번호 형식이 올바르지 않습니다."),
    POSTIT_OUT_OF_BOARD(HttpStatus.BAD_REQUEST, "CP005", "보드 경계를 벗어난 위치입니다."),
    POSTIT_IN_BLOCKED_AREA(HttpStatus.BAD_REQUEST, "CP006", "포스트잇을 배치할 수 없는 영역입니다."),
    MATCHING_ALREADY_MATCHED(HttpStatus.BAD_REQUEST, "M001", "이미 매칭된 상태에서는 취소할 수 없습니다."),
    WAITING_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "W007", "최대 3건까지만 대기 등록이 가능합니다."),
    WAITING_NAME_MISMATCH(HttpStatus.BAD_REQUEST, "W008", "이미 등록된 전화번호의 예약자명과 일치하지 않습니다."),

    /*
     * 401 UNAUTHORIZED
     */
    UNAUTHORIZED_USER(HttpStatus.UNAUTHORIZED, "C003", "인증되지 않은 사용자입니다."),
    PHONE_VERIFICATION_FAILED(HttpStatus.UNAUTHORIZED, "W003", "전화번호 본인 확인에 실패했습니다."),

    /*
     * 403 FORBIDDEN
     */
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C004", "접근 권한이 없습니다."),
    WAITING_REGISTRATION_CLOSED(HttpStatus.FORBIDDEN, "W004", "현재 대기 접수가 중단되었습니다."),
    MATCHING_REGISTRATION_CLOSED(HttpStatus.FORBIDDEN, "M002", "현재 매칭 신청이 중단되었습니다."),

    /*
     * 404 NOT_FOUND
     */
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C404", "요청한 리소스를 찾을 수 없습니다."),
    BOOTH_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "부스를 찾을 수 없습니다."),
    WAITING_NOT_FOUND(HttpStatus.NOT_FOUND, "W005", "대기 정보를 찾을 수 없습니다."),
    CANVAS_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "CP001", "롤링페이퍼 문항을 찾을 수 없습니다."),
    CANVAS_BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "CP002", "롤링페이퍼 보드를 찾을 수 없습니다."),
    CANVAS_POSTIT_NOT_FOUND(HttpStatus.NOT_FOUND, "CP003", "포스트잇을 찾을 수 없습니다."),

    /*
     * 409 CONFLICT
     */
    DUPLICATE_WAITING(HttpStatus.CONFLICT, "W006", "동일 부스에 이미 대기 중인 전화번호입니다."),
    CANVAS_BOARD_FULL(HttpStatus.CONFLICT, "CP007", "보드가 가득 찼습니다. 다른 보드를 이용해주세요."),
    POSTIT_POSITION_CONFLICT(HttpStatus.CONFLICT, "CP008", "해당 위치에 이미 포스트잇이 있습니다."),
    MATCHING_DUPLICATE_REGISTRATION(HttpStatus.CONFLICT, "M003", "이미 매칭 신청이 완료된 인스타그램 ID입니다."),

    /*
     * 500 INTERNAL_SERVER_ERROR
     */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C500", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

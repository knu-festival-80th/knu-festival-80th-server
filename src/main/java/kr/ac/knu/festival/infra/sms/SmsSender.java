package kr.ac.knu.festival.infra.sms;

public interface SmsSender {

    /**
     * Sends an SMS asynchronously. Implementations are expected to handle
     * retries internally and report success/failure via the returned flag.
     *
     * @param phoneNumber 수신자 전화번호 (복호화된 평문)
     * @param message     메시지 본문
     * @return 발송 성공 여부
     */
    boolean send(String phoneNumber, String message);
}

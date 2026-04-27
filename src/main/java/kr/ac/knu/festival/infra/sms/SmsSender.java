package kr.ac.knu.festival.infra.sms;

public interface SmsSender {

    /**
     * SMS 를 발송한다. 비동기 컨텍스트에서 호출되더라도 블로킹 없이 빠르게 반환해야 하며,
     * 구현체는 다음 타임아웃을 강제해야 한다 (NFR-AVAIL-02 / TS-SMS-01).
     * <ul>
     *     <li>connect timeout: 3 초</li>
     *     <li>read timeout: 5 초</li>
     *     <li>전체 시도 타임아웃: 10 초</li>
     * </ul>
     * 타임아웃·게이트웨이 오류를 호출자에게 예외로 전파하지 말고 boolean 으로만 결과를 반환해야 한다.
     *
     * @param phoneNumber 수신자 전화번호 (복호화된 평문)
     * @param message     메시지 본문
     * @return 발송 성공 여부
     */
    boolean send(String phoneNumber, String message);
}

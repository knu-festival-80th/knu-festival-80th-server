package kr.ac.knu.festival.infra.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("test")
public class StubSmsSender implements SmsSender {
    @Override
    public boolean send(String phoneNumber, String message) {
        log.info("[SMS-STUB] to={} message={}", maskPhoneNumber(phoneNumber), message);
        return true;
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return phoneNumber.substring(0, phoneNumber.length() - 4) + "****";
    }
}

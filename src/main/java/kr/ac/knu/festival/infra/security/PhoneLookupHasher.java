package kr.ac.knu.festival.infra.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * 전화번호 중복 검사를 위한 결정적 lookup 해시 생성기.
 * 같은 평문은 항상 같은 해시를 만들기 때문에 DB 인덱스·중복 검사에 사용 가능하다.
 * (개인정보 표시는 {@link PhoneNumberEncryptor} 의 암호문을 사용한다.)
 */
@Slf4j
@Component
public class PhoneLookupHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final String secretKey;
    private SecretKeySpec keySpec;

    public PhoneLookupHasher(@Value("${phone.lookup-hash-key:${PHONE_LOOKUP_HASH_KEY:}}") String secretKey) {
        this.secretKey = secretKey;
    }

    @PostConstruct
    void init() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "PHONE_LOOKUP_HASH_KEY 가 설정되지 않았습니다. 평문 전화번호 dedup 키는 필수 환경변수입니다.");
        }
        this.keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String hash(String plainPhoneNumber) {
        if (plainPhoneNumber == null) {
            throw new IllegalArgumentException("phone number is null");
        }
        String normalized = plainPhoneNumber.replaceAll("\\D", "");
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(keySpec);
            byte[] digest = mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute phone lookup hash", e);
        }
    }
}

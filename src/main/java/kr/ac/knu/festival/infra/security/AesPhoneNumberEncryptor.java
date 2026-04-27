package kr.ac.knu.festival.infra.security;

import jakarta.annotation.PostConstruct;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Slf4j
@Component
public class AesPhoneNumberEncryptor implements PhoneNumberEncryptor {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private final String secretKey;
    private SecretKeySpec keySpec;

    public AesPhoneNumberEncryptor(@Value("${phone.encryption-key:${PHONE_ENCRYPTION_KEY:}}") String secretKey) {
        this.secretKey = secretKey;
    }

    @PostConstruct
    void init() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "PHONE_ENCRYPTION_KEY 가 설정되지 않았습니다. 전화번호 암호화 키는 필수 환경변수입니다.");
        }
        try {
            byte[] key = MessageDigest.getInstance("SHA-256").digest(secretKey.getBytes(StandardCharsets.UTF_8));
            this.keySpec = new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize phone number encryptor", e);
        }
    }

    @Override
    public String encrypt(String plainPhoneNumber) {
        if (plainPhoneNumber == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainPhoneNumber.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Phone number encryption failed", e);
            throw new BusinessException(BusinessErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public String decrypt(String encryptedPhoneNumber) {
        if (encryptedPhoneNumber == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedPhoneNumber);
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTES);
            byte[] cipherText = Arrays.copyOfRange(combined, IV_LENGTH_BYTES, combined.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Phone number decryption failed");
            return null;
        }
    }
}

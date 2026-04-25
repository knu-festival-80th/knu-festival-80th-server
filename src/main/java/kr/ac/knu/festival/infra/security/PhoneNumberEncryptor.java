package kr.ac.knu.festival.infra.security;

public interface PhoneNumberEncryptor {

    String encrypt(String plainPhoneNumber);

    String decrypt(String encryptedPhoneNumber);

    default boolean matchesLast4(String encryptedPhoneNumber, String last4Digits) {
        if (last4Digits == null || last4Digits.length() != 4) {
            return false;
        }
        String decrypted = decrypt(encryptedPhoneNumber);
        if (decrypted == null) {
            return false;
        }
        String digitsOnly = decrypted.replaceAll("\\D", "");
        if (digitsOnly.length() < 4) {
            return false;
        }
        return digitsOnly.endsWith(last4Digits);
    }
}

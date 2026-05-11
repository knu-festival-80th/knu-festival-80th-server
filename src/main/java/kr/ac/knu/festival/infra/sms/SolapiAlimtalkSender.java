package kr.ac.knu.festival.infra.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@Primary
@Profile("!test")
public class SolapiAlimtalkSender implements SmsSender {

    private static final String API_URL = "https://api.solapi.com/messages/v4/send";
    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private final SolapiProperties properties;
    private final RestClient restClient;

    public SolapiAlimtalkSender(SolapiProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public boolean send(String phoneNumber, String message) {
        try {
            String date = ISO_FORMATTER.format(Instant.now());
            String salt = UUID.randomUUID().toString();
            String signature = generateSignature(date, salt);

            Map<String, Object> body = Map.of(
                    "message", Map.of(
                            "to", phoneNumber,
                            "from", properties.senderNumber(),
                            "text", message,
                            "type", "ATA",
                            "kakaoOptions", Map.of(
                                    "pfId", properties.pfId()
                            )
                    )
            );

            String authorization = "HMAC-SHA256 apiKey=%s, date=%s, salt=%s, signature=%s"
                    .formatted(properties.apiKey(), date, salt, signature);

            restClient.post()
                    .uri(API_URL)
                    .header("Authorization", authorization)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[ALIMTALK] 발송 성공. to={}", maskPhone(phoneNumber));
            return true;
        } catch (Exception e) {
            log.warn("[ALIMTALK] 발송 실패. to={}, error={}", maskPhone(phoneNumber), e.getMessage());
            return false;
        }
    }

    private String generateSignature(String date, String salt) {
        try {
            String data = date + salt;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.apiSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 서명 생성 실패", e);
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return phone.substring(0, phone.length() - 4) + "****";
    }
}

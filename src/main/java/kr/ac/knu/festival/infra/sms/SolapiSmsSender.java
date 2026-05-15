package kr.ac.knu.festival.infra.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
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
public class SolapiSmsSender implements SmsSender {

    private static final String API_URL = "https://api.solapi.com/messages/v4/send";
    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 5_000;
    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MS = 200L;

    private final SolapiProperties properties;
    private final RestClient restClient;

    public SolapiSmsSender(SolapiProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public boolean send(String phoneNumber, String message) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String date = ISO_FORMATTER.format(Instant.now());
                String salt = UUID.randomUUID().toString();
                String signature = generateSignature(date, salt);

                Map<String, Object> body = Map.of(
                        "message", Map.of(
                                "to", phoneNumber,
                                "from", properties.senderNumber(),
                                "text", message
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

                log.info("[SMS] 발송 성공. to={}, attempt={}", maskPhone(phoneNumber), attempt);
                return true;
            } catch (HttpClientErrorException e) {
                // 4xx 는 잘못된 요청이라 재시도해도 의미 없음 → 즉시 실패.
                log.warn("[SMS] 발송 실패(4xx). to={}, status={}, attempt={}",
                        maskPhone(phoneNumber), e.getStatusCode(), attempt);
                return false;
            } catch (HttpServerErrorException e) {
                if (!shouldRetry(attempt)) {
                    log.warn("[SMS] 발송 실패(5xx, 재시도 한도 초과). to={}, status={}, attempt={}",
                            maskPhone(phoneNumber), e.getStatusCode(), attempt);
                    return false;
                }
                log.info("[SMS] 5xx 응답, 재시도 예정. to={}, status={}, attempt={}",
                        maskPhone(phoneNumber), e.getStatusCode(), attempt);
                sleepBackoff(attempt);
            } catch (ResourceAccessException e) {
                if (!shouldRetry(attempt)) {
                    log.warn("[SMS] 네트워크 실패(재시도 한도 초과). to={}, error={}, attempt={}",
                            maskPhone(phoneNumber), e.getMessage(), attempt);
                    return false;
                }
                log.info("[SMS] 네트워크 실패, 재시도 예정. to={}, error={}, attempt={}",
                        maskPhone(phoneNumber), e.getMessage(), attempt);
                sleepBackoff(attempt);
            } catch (Exception e) {
                // 기타 예외(서명 생성 실패 등)는 재시도해도 동일 결과라 즉시 실패.
                HttpStatusCode status = null;
                log.warn("[SMS] 발송 실패. to={}, error={}, status={}, attempt={}",
                        maskPhone(phoneNumber), e.getMessage(), status, attempt);
                return false;
            }
        }
        return false;
    }

    private boolean shouldRetry(int attempt) {
        return attempt < MAX_ATTEMPTS;
    }

    private void sleepBackoff(int attempt) {
        long delay = BASE_BACKOFF_MS * (1L << (attempt - 1));
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
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

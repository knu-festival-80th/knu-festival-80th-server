package kr.ac.knu.festival.infra.sms;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "solapi")
public record SolapiProperties(
        String apiKey,
        String apiSecret,
        String pfId,
        String senderNumber
) {
}

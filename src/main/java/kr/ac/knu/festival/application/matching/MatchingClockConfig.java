package kr.ac.knu.festival.application.matching;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class MatchingClockConfig {

    @Bean
    @ConditionalOnMissingBean(name = "matchingClock")
    public Clock matchingClock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}

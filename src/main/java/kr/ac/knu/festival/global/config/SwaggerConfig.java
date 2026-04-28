package kr.ac.knu.festival.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KNU 80th Festival API")
                        .version("v1")
                        .description("2026 경북대학교 80주년 대동제 웹앱 백엔드 API — 세션 기반 인증 (JSESSIONID 쿠키)"));
    }
}

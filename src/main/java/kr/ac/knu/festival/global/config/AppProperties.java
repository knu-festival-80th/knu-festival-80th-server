package kr.ac.knu.festival.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * 외부에 노출되는 베이스 URL. 로컬은 http://localhost:8080,
     * 운영은 ingress 도메인. DB에 저장된 상대 경로를 완전한 URL로 만들 때 사용.
     */
    private String publicBaseUrl = "http://localhost:8080";

    private final Upload upload = new Upload();

    @Getter
    @Setter
    public static class Upload {
        /**
         * 업로드된 파일을 저장할 로컬 디렉터리. 컨테이너 환경에서는 볼륨 마운트 경로,
         * IDE 실행 시에는 워킹 디렉터리 기준 상대 경로.
         */
        private String baseDir = "./uploads";

        /**
         * 업로드된 파일이 노출되는 URL 프리픽스. 정적 리소스 핸들러와 응답 URL 모두에 사용.
         */
        private String urlPrefix = "/uploads";
    }
}

package kr.ac.knu.festival.global.config;

import kr.ac.knu.festival.global.auth.CurrentAdminResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AppProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentAdminResolver currentAdminResolver;
    private final AppProperties appProperties;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentAdminResolver);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String urlPrefix = stripTrailingSlash(appProperties.getUpload().getUrlPrefix());
        Path baseDir = Path.of(appProperties.getUpload().getBaseDir()).toAbsolutePath().normalize();
        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations("file:" + baseDir + "/")
                .setCachePeriod(3600);
    }

    private static String stripTrailingSlash(String url) {
        if (url == null || url.isEmpty()) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

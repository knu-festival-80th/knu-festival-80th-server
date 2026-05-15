package kr.ac.knu.festival.global.config;

import kr.ac.knu.festival.global.auth.CurrentAdminResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AppProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private static final String IMAGES_SUBDIR = "images";

    private final CurrentAdminResolver currentAdminResolver;
    private final AppProperties appProperties;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentAdminResolver);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String urlPrefix = stripTrailingSlash(appProperties.getUpload().getUrlPrefix());
        Path imagesDir = Path.of(appProperties.getUpload().getBaseDir())
                .toAbsolutePath().normalize().resolve(IMAGES_SUBDIR);
        registry.addResourceHandler(urlPrefix + "/" + IMAGES_SUBDIR + "/**")
                .addResourceLocations("file:" + imagesDir + "/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).immutable());
    }

    private static String stripTrailingSlash(String url) {
        if (url == null || url.isEmpty()) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

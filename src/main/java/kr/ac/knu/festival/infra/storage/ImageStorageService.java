package kr.ac.knu.festival.infra.storage;

import jakarta.annotation.PostConstruct;
import kr.ac.knu.festival.global.config.AppProperties;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final String IMAGES_SUBDIR = "images";

    private final AppProperties appProperties;

    private Path imagesDir;
    private String imagesUrlPrefix;

    @PostConstruct
    void init() throws IOException {
        Path baseDir = Path.of(appProperties.getUpload().getBaseDir()).toAbsolutePath().normalize();
        this.imagesDir = baseDir.resolve(IMAGES_SUBDIR);
        Files.createDirectories(this.imagesDir);
        String prefix = stripTrailingSlash(appProperties.getUpload().getUrlPrefix());
        this.imagesUrlPrefix = prefix + "/" + IMAGES_SUBDIR;
        log.info("Image upload dir initialized: {} (url prefix={})", imagesDir, imagesUrlPrefix);
    }

    /**
     * 이미지 파일을 디스크에 저장하고 URL-relative 경로(e.g. /uploads/images/{uuid}.png)를 반환한다.
     * 반환 경로는 DB 저장용. 응답에는 ImageUrlResolver가 완전한 URL로 변환해서 내려준다.
     */
    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.INVALID_INPUT_VALUE, "이미지 파일이 비어 있습니다.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException(
                    BusinessErrorCode.INVALID_INPUT_VALUE,
                    "이미지 파일 용량은 최대 " + (MAX_BYTES / (1024 * 1024)) + "MB 까지 허용됩니다."
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(
                    BusinessErrorCode.INVALID_INPUT_VALUE,
                    "허용되지 않는 이미지 형식입니다. (jpg, png, webp, gif 만 가능)"
            );
        }

        String extension = resolveExtension(file.getOriginalFilename(), contentType);
        String filename = UUID.randomUUID() + "." + extension;
        Path target = imagesDir.resolve(filename);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to write upload to {}", target, e);
            throw new BusinessException(BusinessErrorCode.INTERNAL_SERVER_ERROR, "이미지 저장에 실패했습니다.");
        }

        return imagesUrlPrefix + "/" + filename;
    }

    private String resolveExtension(String originalFilename, String contentType) {
        String ext = null;
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                ext = originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
            }
        }
        if (ext == null || !ALLOWED_EXTENSIONS.contains(ext)) {
            ext = switch (contentType.toLowerCase(Locale.ROOT)) {
                case "image/jpeg" -> "jpg";
                case "image/png" -> "png";
                case "image/webp" -> "webp";
                case "image/gif" -> "gif";
                default -> throw new BusinessException(
                        BusinessErrorCode.INVALID_INPUT_VALUE,
                        "허용되지 않는 이미지 형식입니다."
                );
            };
        }
        return "jpeg".equals(ext) ? "jpg" : ext;
    }

    private static String stripTrailingSlash(String url) {
        if (url == null || url.isEmpty()) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

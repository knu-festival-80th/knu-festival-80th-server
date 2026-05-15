package kr.ac.knu.festival.infra.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

/**
 * 업로드 이미지 디렉토리에서 일정 시간(24h) 이상 경과한 파일을 주기적으로 정리한다.
 * - DB 상의 이미지 URL 과 파일 생성 시점이 분리될 수 있으므로 보수적으로 생성 시각 기준만 적용한다.
 * - 디렉토리 자체는 절대 삭제하지 않는다.
 * - 예외는 모두 catch 하여 스케줄러 자체가 죽지 않도록 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageStorageCleanupScheduler {

    private static final long RETENTION_HOURS = 24L;

    private final ImageStorageService imageStorageService;

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupOldFiles() {
        Path imagesDir = imageStorageService.getImagesDir();
        if (imagesDir == null || !Files.isDirectory(imagesDir)) {
            return;
        }
        Instant threshold = Instant.now().minus(Duration.ofHours(RETENTION_HOURS));
        long deleted = 0L;
        try (Stream<Path> stream = Files.walk(imagesDir, 1)) {
            for (Path file : (Iterable<Path>) stream::iterator) {
                if (file.equals(imagesDir)) {
                    continue;
                }
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                try {
                    BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                    if (attrs.creationTime().toInstant().isBefore(threshold)) {
                        if (Files.deleteIfExists(file)) {
                            deleted++;
                        }
                    }
                } catch (IOException e) {
                    log.warn("[storage-cleanup] failed to inspect/delete file={}", file, e);
                }
            }
        } catch (IOException e) {
            log.warn("[storage-cleanup] failed to walk dir={}", imagesDir, e);
            return;
        } catch (Exception e) {
            log.warn("[storage-cleanup] unexpected error", e);
            return;
        }
        log.info("[storage-cleanup] deleted={} threshold={}h", deleted, RETENTION_HOURS);
    }
}

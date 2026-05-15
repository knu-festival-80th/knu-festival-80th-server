package kr.ac.knu.festival.infra.booth;

import jakarta.persistence.EntityManager;
import kr.ac.knu.festival.domain.booth.entity.Booth;
import kr.ac.knu.festival.domain.booth.repository.BoothRepository;
import kr.ac.knu.festival.global.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoothDataInitializer implements CommandLineRunner {

    private record BoothSeed(String name, String department, String menuImage) {}

    private static final List<BoothSeed> BOOTHS = List.of(
            new BoothSeed("경북대일본인유학생회", "사회과학대학 정치외교학과", "1.png"),
            new BoothSeed("식물생명과학전공", "농업생명과학대학 응용생명과학부 식물생명과학전공", "2.png"),
            new BoothSeed("약학대학", "약학과", null),
            new BoothSeed("경북대학교 생물학과", "생물학과", null),
            new BoothSeed("생공 zoo점", "생명공학부", "5.png"),
            new BoothSeed("지형학회 돌띠", "지리학과", "6.png"),
            new BoothSeed("나는 술로", "식품공학부 식품생물공학전공", "7.png"),
            new BoothSeed("자연과학대학 통계학과", "통계학과", null),
            new BoothSeed("스마트모빌리티공학과", "스마트모빌리티공학과", "9.png"),
            new BoothSeed("프린세스 케미핑", "화학과", "10.png"),
            new BoothSeed("전기항공(전기공학과)", "전기공학과", "11.jpg"),
            new BoothSeed("청춘캠퍼스", "에너지공학부", "12.jpeg"),
            new BoothSeed("만취통증의학과", "의예과", "13.png"),
            new BoothSeed("해적왕이 될 ai", "IT대학 전자공학부 인공지능전공", "14.png"),
            new BoothSeed("부르마블 세계주막", "중어중문학과, 불어불문학과, 독어독문학과, 노어노문학과, 일어일문학과", "15.png"),
            new BoothSeed("라스트댄스", "화학과", null),
            new BoothSeed("경북대학교 통계학과 등산동아리", "자연과학대학 통계학과", "17.jpg"),
            new BoothSeed("청춘은 지금 수확중", "농업생명과학대학 자율학부", "18.png"),
            new BoothSeed("경북대학교 사회과학대학 정치외교학과", "정치외교학과", "19.png"),
            new BoothSeed("일醉월장(경북대학교 한문학과 주막)", "경북대학교 한문학과", "20.png"),
            new BoothSeed("사범대학 미래교육동아리 '미동'", "일반사회교육과", "21.png"),
            new BoothSeed("금속재료공학과", "금속재료공학과", "22.jpg"),
            new BoothSeed("(경)통행(정)금지구역", "경제통상학부, 행정학부", "23.png"),
            new BoothSeed("jin-心 : 진심", "지질학과", null),
            new BoothSeed("IT대학 학생회", "전자공학부", null),
            new BoothSeed("고고인류학과", "고고인류학과", "26.png"),
            new BoothSeed("지구시스템과학부", "자연과학대학 지구시스템과학부", "27.png"),
            new BoothSeed("수리수리마수리", "수학과", "28.jpg"),
            new BoothSeed("\"불\"리학과", "물리학과", "29.png"),
            new BoothSeed("모여봐요 수의대의 숲", "수의학과", "30.png"),
            new BoothSeed("경북대학교 사범대학 사이 학생회", "수학교육과", "31.png"),
            new BoothSeed("[우주공학부 X 첨단기술융합대학 자율학부 1] ERROR 404", null, null),
            new BoothSeed("꽃마을 포차", "식물의학과", null),
            new BoothSeed("우주라이크", null, null),
            new BoothSeed("농업토목공학과:농또목", null, null),
            new BoothSeed("경북대학교 베트남 유학생회", null, null),
            new BoothSeed("컨텐츠 동아리", null, "37.png"),
            new BoothSeed("경북대학교 몽골유학생회", "경제통상학부", "38.png"),
            new BoothSeed("생협", null, null),
            new BoothSeed("신망애", null, null),
            new BoothSeed("사주도령", null, null),
            new BoothSeed("경청", null, null),
            new BoothSeed("미술학과", null, null),
            new BoothSeed("레불루션", null, null),
            new BoothSeed("학생과 장학복지팀", null, null),
            new BoothSeed("중국 유학생회", null, null),
            new BoothSeed("소프트웨어 교육원", null, null),
            new BoothSeed("대학원 홍보 모니터링단", null, null),
            new BoothSeed("진로취업과", null, null),
            new BoothSeed("안전관리총괄본부", null, null),
            new BoothSeed("교수학습센터", null, null)
    );

    private final BoothRepository boothRepository;
    private final EntityManager entityManager;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public void run(String... args) {
        long count = boothRepository.count();
        if (count == BOOTHS.size() && !hasMissingMenuImages()) return;

        if (count > 0) {
            log.info("Clearing existing booth data. currentCount={}, expectedCount={}", count, BOOTHS.size());
            entityManager.createNativeQuery("DELETE FROM waiting").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM booth").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE booth AUTO_INCREMENT = 1").executeUpdate();
        }

        Path imagesDir = Path.of(appProperties.getUpload().getBaseDir(), "images");
        try {
            Files.createDirectories(imagesDir);
        } catch (IOException e) {
            log.warn("Failed to create upload directory: {}", imagesDir, e);
        }

        int imageCount = 0;
        for (BoothSeed seed : BOOTHS) {
            String menuImageUrl = null;
            if (seed.menuImage() != null) {
                String targetFileName = "booth-menu-" + seed.menuImage();
                menuImageUrl = copyMenuImage(seed.menuImage(), imagesDir.resolve(targetFileName));
                if (menuImageUrl != null) imageCount++;
            }
            boothRepository.save(
                    Booth.createBooth(seed.name(), null, null, menuImageUrl, null, seed.department(), null)
            );
        }
        log.info("Booth seed data initialized. boothCount={}, menuImageCount={}", BOOTHS.size(), imageCount);
    }

    private boolean hasMissingMenuImages() {
        Path imagesDir = Path.of(appProperties.getUpload().getBaseDir(), "images");
        return BOOTHS.stream()
                .filter(s -> s.menuImage() != null)
                .anyMatch(s -> !Files.exists(imagesDir.resolve("booth-menu-" + s.menuImage())));
    }

    private String copyMenuImage(String resourceName, Path target) {
        ClassPathResource resource = new ClassPathResource("booth-menus/" + resourceName);
        try (InputStream is = resource.getInputStream()) {
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/images/" + target.getFileName();
        } catch (IOException e) {
            log.warn("Failed to copy menu image: {}", resourceName, e);
            return null;
        }
    }
}

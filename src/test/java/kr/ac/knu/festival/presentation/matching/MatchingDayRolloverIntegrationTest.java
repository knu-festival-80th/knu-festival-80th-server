package kr.ac.knu.festival.presentation.matching;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ac.knu.festival.domain.matching.entity.MatchingGender;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.repository.MatchingParticipantRepository;
import kr.ac.knu.festival.domain.matching.repository.MatchingServiceStateRepository;
import kr.ac.knu.festival.infra.security.PhoneLookupHasher;
import kr.ac.knu.festival.infra.security.PhoneNumberEncryptor;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MatchingDayRolloverIntegrationTest.FixedClockConfig.class)
class MatchingDayRolloverIntegrationTest {

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock matchingClock() {
            // KST 2026-05-21 13:00 — day-2 신청창. 5/20 신청 데이터가 이미 있다고 가정한 상태에서 5/21 재신청 가능 여부 확인.
            return Clock.fixed(Instant.parse("2026-05-21T04:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MatchingParticipantRepository matchingParticipantRepository;

    @Autowired
    private MatchingServiceStateRepository matchingServiceStateRepository;

    @Autowired
    private PhoneLookupHasher phoneLookupHasher;

    @Autowired
    private PhoneNumberEncryptor phoneNumberEncryptor;

    @BeforeEach
    void setUp() {
        matchingParticipantRepository.deleteAll();
        matchingServiceStateRepository.deleteAll();
    }

    @Test
    void sameInstagramIdMayRegisterOnEachFestivalDay() throws Exception {
        // 5/20 에 이미 신청·매칭 완료된 기록을 직접 심는다.
        MatchingParticipant day1 = MatchingParticipant.create(
                "repeat_user",
                LocalDate.parse("2026-05-20"),
                MatchingGender.MALE,
                phoneLookupHasher.hash("01011112222"),
                phoneNumberEncryptor.encrypt("01011112222")
        );
        day1.matchWith("yesterday_partner");
        matchingParticipantRepository.save(day1);

        // 5/21 13:00 시점에 동일 ID 가 다시 신청 → 성공
        MatchingCreateRequest request = new MatchingCreateRequest(
                "repeat_user", MatchingGender.MALE, "01011112222"
        );
        mockMvc.perform(post("/matchings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.festivalDay").value("2026-05-21"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        assertThat(matchingParticipantRepository
                .findByInstagramIdAndFestivalDay("repeat_user", LocalDate.parse("2026-05-20"))).isPresent();
        assertThat(matchingParticipantRepository
                .findByInstagramIdAndFestivalDay("repeat_user", LocalDate.parse("2026-05-21"))).isPresent();
    }
}

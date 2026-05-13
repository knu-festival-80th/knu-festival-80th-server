package kr.ac.knu.festival.presentation.matching;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ac.knu.festival.domain.matching.entity.MatchingGender;
import kr.ac.knu.festival.domain.matching.entity.MatchingOperationStatus;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;
import kr.ac.knu.festival.domain.matching.entity.MatchingServiceState;
import kr.ac.knu.festival.domain.matching.repository.MatchingParticipantRepository;
import kr.ac.knu.festival.domain.matching.repository.MatchingServiceStateRepository;
import kr.ac.knu.festival.infra.security.PhoneLookupHasher;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
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
@org.springframework.context.annotation.Import(MatchingRegistrationIntegrationTest.FixedClockConfig.class)
class MatchingRegistrationIntegrationTest {

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock matchingClock() {
            return Clock.fixed(Instant.parse("2026-05-20T03:00:00Z"), ZoneId.of("Asia/Seoul")); // KST 12:00
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

    @BeforeEach
    void setUp() {
        matchingParticipantRepository.deleteAll();
        matchingServiceStateRepository.deleteAll();
    }

    @Test
    void registerStoresPhoneLookupHashAndFestivalDay() throws Exception {
        MatchingCreateRequest request = new MatchingCreateRequest(
                "KNU.Student_01",
                MatchingGender.MALE,
                "01012345678"
        );

        mockMvc.perform(post("/matchings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.instagramId").value("knu.student_01"))
                .andExpect(jsonPath("$.data.festivalDay").value("2026-05-20"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        MatchingParticipant saved = matchingParticipantRepository
                .findByInstagramIdAndFestivalDay("knu.student_01", LocalDate.parse("2026-05-20"))
                .orElseThrow();
        assertThat(saved.getGender()).isEqualTo(MatchingGender.MALE);
        assertThat(saved.getStatus()).isEqualTo(MatchingParticipantStatus.PENDING);
        assertThat(saved.getPhoneLookupHash()).isEqualTo(phoneLookupHasher.hash("01012345678"));
        assertThat(saved.getPhoneEncrypted()).isNotBlank();
    }

    @Test
    void rejectDuplicateRegistrationOnSameDay() throws Exception {
        MatchingCreateRequest request = new MatchingCreateRequest(
                "dup_user",
                MatchingGender.MALE,
                "01011112222"
        );
        mockMvc.perform(post("/matchings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/matchings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("M003"));
    }

    @Test
    void rejectRegistrationWhenServiceIsPaused() throws Exception {
        MatchingServiceState pausedState = MatchingServiceState.defaultOpen();
        pausedState.changeStatus(MatchingOperationStatus.PAUSED, "일시중단", "Paused");
        matchingServiceStateRepository.save(pausedState);

        MatchingCreateRequest request = new MatchingCreateRequest(
                "paused_user",
                MatchingGender.FEMALE,
                "01099998888"
        );

        mockMvc.perform(post("/matchings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        assertThat(matchingParticipantRepository
                .existsByInstagramIdAndFestivalDay("paused_user", LocalDate.parse("2026-05-20"))).isFalse();
    }

    @Test
    void rejectInvalidPhoneFormat() throws Exception {
        MatchingCreateRequest request = new MatchingCreateRequest(
                "bad_phone",
                MatchingGender.MALE,
                "1234"
        );

        mockMvc.perform(post("/matchings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
    }
}

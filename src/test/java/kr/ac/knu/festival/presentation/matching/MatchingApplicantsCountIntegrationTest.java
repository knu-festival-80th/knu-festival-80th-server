package kr.ac.knu.festival.presentation.matching;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ac.knu.festival.domain.matching.entity.MatchingGender;
import kr.ac.knu.festival.domain.matching.repository.MatchingParticipantRepository;
import kr.ac.knu.festival.domain.matching.repository.MatchingServiceStateRepository;
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
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MatchingApplicantsCountIntegrationTest.FixedClockConfig.class)
class MatchingApplicantsCountIntegrationTest {

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

    @BeforeEach
    void setUp() {
        matchingParticipantRepository.deleteAll();
        matchingServiceStateRepository.deleteAll();
    }

    @Test
    void countReflectsGenderSplitAfterRegistrations() throws Exception {
        register("male_a", MatchingGender.MALE, "01011111111");
        register("male_b", MatchingGender.MALE, "01022222222");
        register("female_a", MatchingGender.FEMALE, "01033333333");

        mockMvc.perform(get("/matchings/applicants/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.festivalDay").value("2026-05-20"))
                .andExpect(jsonPath("$.data.malePendingCount").value(2))
                .andExpect(jsonPath("$.data.femalePendingCount").value(1))
                .andExpect(jsonPath("$.data.totalPendingCount").value(3));
    }

    private void register(String id, MatchingGender gender, String phone) throws Exception {
        MatchingCreateRequest request = new MatchingCreateRequest(id, gender, phone);
        mockMvc.perform(post("/matchings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}

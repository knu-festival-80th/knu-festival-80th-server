package kr.ac.knu.festival.presentation.matching;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ac.knu.festival.domain.matching.entity.MatchingGender;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.repository.MatchingParticipantRepository;
import kr.ac.knu.festival.domain.matching.repository.MatchingServiceStateRepository;
import kr.ac.knu.festival.infra.security.PhoneLookupHasher;
import kr.ac.knu.festival.infra.security.PhoneNumberEncryptor;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingAuthRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MatchingResultOpenIntegrationTest.FixedClockConfig.class)
class MatchingResultOpenIntegrationTest {

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock matchingClock() {
            // KST 2026-05-20 22:30 — 결과창 열림
            return Clock.fixed(Instant.parse("2026-05-20T13:30:00Z"), ZoneId.of("Asia/Seoul"));
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
    void showMatchingResultAfterOpenTime() throws Exception {
        LocalDate day = LocalDate.parse("2026-05-20");
        MatchingParticipant male = MatchingParticipant.create(
                "male_user",
                day,
                MatchingGender.MALE,
                phoneLookupHasher.hash("01011112222"),
                phoneNumberEncryptor.encrypt("01011112222")
        );
        MatchingParticipant female = MatchingParticipant.create(
                "female_user",
                day,
                MatchingGender.FEMALE,
                phoneLookupHasher.hash("01033334444"),
                phoneNumberEncryptor.encrypt("01033334444")
        );
        male.matchWith("female_user");
        female.matchWith("male_user");
        matchingParticipantRepository.save(male);
        matchingParticipantRepository.save(female);

        MatchingAuthRequest request = new MatchingAuthRequest("male_user", "01011112222");

        mockMvc.perform(post("/matchings/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultOpen").value(true))
                .andExpect(jsonPath("$.data.pickedInstagramId").value("female_user"))
                .andExpect(jsonPath("$.data.instagramProfileUrl").value("https://instagram.com/female_user"));
    }

    @Test
    void rejectResultLookupWithWrongPhone() throws Exception {
        LocalDate day = LocalDate.parse("2026-05-20");
        MatchingParticipant male = MatchingParticipant.create(
                "another_male",
                day,
                MatchingGender.MALE,
                phoneLookupHasher.hash("01055556666"),
                phoneNumberEncryptor.encrypt("01055556666")
        );
        matchingParticipantRepository.save(male);

        MatchingAuthRequest wrong = new MatchingAuthRequest("another_male", "01099999999");

        mockMvc.perform(post("/matchings/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrong)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("C003"));
    }
}

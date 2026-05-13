package kr.ac.knu.festival.presentation.matching;

import kr.ac.knu.festival.domain.matching.entity.MatchingGender;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.repository.MatchingParticipantRepository;
import kr.ac.knu.festival.domain.matching.repository.MatchingServiceStateRepository;
import kr.ac.knu.festival.infra.security.PhoneLookupHasher;
import kr.ac.knu.festival.infra.security.PhoneNumberEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(UnmatchedParticipantsIntegrationTest.FixedClockConfig.class)
class UnmatchedParticipantsIntegrationTest {

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock matchingClock() {
            // KST 2026-05-20 12:00 — 신청창 안, 결과창 아님
            return Clock.fixed(Instant.parse("2026-05-20T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }

    @Autowired
    private MockMvc mockMvc;

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
    void hideUnmatchedParticipantsBeforeResultOpenTime() throws Exception {
        MatchingParticipant participant = MatchingParticipant.create(
                "hidden_unmatched",
                LocalDate.parse("2026-05-20"),
                MatchingGender.MALE,
                phoneLookupHasher.hash("01012345678"),
                phoneNumberEncryptor.encrypt("01012345678")
        );
        participant.markUnmatched();
        matchingParticipantRepository.save(participant);

        mockMvc.perform(get("/matchings/unmatched"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultOpen").value(false))
                .andExpect(jsonPath("$.data.participants").isEmpty());
    }
}

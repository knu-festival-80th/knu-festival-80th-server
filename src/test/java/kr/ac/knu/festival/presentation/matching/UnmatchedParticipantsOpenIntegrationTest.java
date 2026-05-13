package kr.ac.knu.festival.presentation.matching;

import kr.ac.knu.festival.domain.matching.entity.MatchingGender;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.repository.MatchingParticipantRepository;
import kr.ac.knu.festival.domain.matching.repository.MatchingServiceStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "matching.result-open-at=2026-05-01T22:00:00+09:00")
@AutoConfigureMockMvc
class UnmatchedParticipantsOpenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MatchingParticipantRepository matchingParticipantRepository;

    @Autowired
    private MatchingServiceStateRepository matchingServiceStateRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        matchingParticipantRepository.deleteAll();
        matchingServiceStateRepository.deleteAll();
    }

    @Test
    void showUnmatchedParticipantsAfterResultOpenTime() throws Exception {
        MatchingParticipant participant = MatchingParticipant.create(
                "open_unmatched",
                MatchingGender.FEMALE,
                passwordEncoder.encode("1234"),
                "EN"
        );
        participant.markUnmatched();
        matchingParticipantRepository.save(participant);

        mockMvc.perform(get("/matchings/unmatched"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultOpen").value(true))
                .andExpect(jsonPath("$.data.participants[0].instagramId").value("open_unmatched"))
                .andExpect(jsonPath("$.data.participants[0].instagramProfileUrl").value("https://instagram.com/open_unmatched"));
    }
}

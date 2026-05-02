package kr.ac.knu.festival.presentation.matching;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ac.knu.festival.domain.matching.entity.MatchingGender;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.repository.MatchingParticipantRepository;
import kr.ac.knu.festival.domain.matching.repository.MatchingServiceStateRepository;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingAuthRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "matching.result-open-at=2026-05-01T22:00:00+09:00")
@AutoConfigureMockMvc
class MatchingResultOpenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void showMatchingResultAfterOpenTime() throws Exception {
        MatchingParticipant male = MatchingParticipant.create(
                "male_user",
                MatchingGender.MALE,
                passwordEncoder.encode("1234"),
                "KR"
        );
        MatchingParticipant female = MatchingParticipant.create(
                "female_user",
                MatchingGender.FEMALE,
                passwordEncoder.encode("1234"),
                "KR"
        );
        male.matchWith("female_user");
        female.matchWith("male_user");
        matchingParticipantRepository.save(male);
        matchingParticipantRepository.save(female);

        MatchingAuthRequest request = new MatchingAuthRequest("male_user", "1234");

        mockMvc.perform(post("/api/v1/matchings/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultOpen").value(true))
                .andExpect(jsonPath("$.data.matchedInstagramId").value("female_user"))
                .andExpect(jsonPath("$.data.instagramProfileUrl").value("https://instagram.com/female_user"));
    }
}

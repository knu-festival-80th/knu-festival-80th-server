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

@SpringBootTest
@AutoConfigureMockMvc
class MatchingResultRateLimitIntegrationTest {

    private static final String CLIENT_IP = "203.0.113.10";

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
        matchingParticipantRepository.save(MatchingParticipant.create(
                "rate_limit_user",
                MatchingGender.MALE,
                passwordEncoder.encode("1234"),
                "KR"
        ));
    }

    @Test
    void limitResultLookupAfterRepeatedFailuresFromSameIp() throws Exception {
        MatchingAuthRequest wrongPassword = new MatchingAuthRequest("rate_limit_user", "wrong");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/v1/matchings/result")
                            .with(request -> {
                                request.setRemoteAddr(CLIENT_IP);
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongPassword)))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/v1/matchings/result")
                        .with(request -> {
                            request.setRemoteAddr(CLIENT_IP);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPassword)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("M001"));
    }
}

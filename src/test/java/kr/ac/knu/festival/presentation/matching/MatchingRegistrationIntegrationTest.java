package kr.ac.knu.festival.presentation.matching;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ac.knu.festival.domain.matching.entity.MatchingGender;
import kr.ac.knu.festival.domain.matching.entity.MatchingOperationStatus;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;
import kr.ac.knu.festival.domain.matching.entity.MatchingServiceState;
import kr.ac.knu.festival.domain.matching.repository.MatchingParticipantRepository;
import kr.ac.knu.festival.domain.matching.repository.MatchingServiceStateRepository;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingAuthRequest;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MatchingRegistrationIntegrationTest {

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
    void registerMatchingParticipant() throws Exception {
        MatchingCreateRequest request = new MatchingCreateRequest(
                "KNU.Student_01",
                MatchingGender.MALE,
                "1234"
        );

        mockMvc.perform(post("/matchings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.instagramId").value("knu.student_01"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        MatchingParticipant saved = matchingParticipantRepository.findById("knu.student_01").orElseThrow();
        assertThat(saved.getGender()).isEqualTo(MatchingGender.MALE);
        assertThat(saved.getStatus()).isEqualTo(MatchingParticipantStatus.PENDING);
        assertThat(passwordEncoder.matches("1234", saved.getPassword())).isTrue();
    }

    @Test
    void rejectRegistrationWhenMatchingIsPaused() throws Exception {
        MatchingServiceState pausedState = MatchingServiceState.defaultOpen();
        pausedState.changeStatus(MatchingOperationStatus.PAUSED, "일시중단", "Paused");
        matchingServiceStateRepository.save(pausedState);

        MatchingCreateRequest request = new MatchingCreateRequest(
                "paused_user",
                MatchingGender.FEMALE,
                "1234"
        );

        mockMvc.perform(post("/matchings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        assertThat(matchingParticipantRepository.existsById("paused_user")).isFalse();
    }

    @Test
    void hideMatchingResultBeforeOpenTime() throws Exception {
        MatchingParticipant male = MatchingParticipant.create(
                "male_user",
                MatchingGender.MALE,
                passwordEncoder.encode("1234")
        );
        MatchingParticipant female = MatchingParticipant.create(
                "female_user",
                MatchingGender.FEMALE,
                passwordEncoder.encode("1234")
        );
        male.matchWith("female_user");
        female.matchWith("male_user");
        matchingParticipantRepository.save(male);
        matchingParticipantRepository.save(female);

        MatchingAuthRequest request = new MatchingAuthRequest("male_user", "1234");

        mockMvc.perform(post("/matchings/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultOpen").value(false))
                .andExpect(jsonPath("$.data.matchedInstagramId").doesNotExist());
    }
}

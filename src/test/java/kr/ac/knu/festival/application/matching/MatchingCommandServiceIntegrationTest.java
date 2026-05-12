package kr.ac.knu.festival.application.matching;

import kr.ac.knu.festival.domain.matching.entity.MatchingGender;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;
import kr.ac.knu.festival.domain.matching.repository.MatchingParticipantRepository;
import kr.ac.knu.festival.domain.matching.repository.MatchingServiceStateRepository;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingCreateRequest;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingJobResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MatchingCommandServiceIntegrationTest {

    @Autowired
    private MatchingCommandService matchingCommandService;

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
    void matchOnlyEarlyParticipantsAndMarkLateMajorityGenderAsUnmatched() throws Exception {
        register("male_1", MatchingGender.MALE);
        register("male_2", MatchingGender.MALE);
        register("male_3", MatchingGender.MALE);
        register("female_1", MatchingGender.FEMALE);
        register("female_2", MatchingGender.FEMALE);

        MatchingJobResponse response = matchingCommandService.runMatchingJob();

        assertThat(response.matchedPairCount()).isEqualTo(2);
        assertThat(response.unmatchedCount()).isEqualTo(1);

        List<MatchingParticipant> males = matchingParticipantRepository.findAll().stream()
                .filter(participant -> participant.getGender() == MatchingGender.MALE)
                .filter(participant -> participant.getStatus() == MatchingParticipantStatus.MATCHED)
                .toList();
        List<MatchingParticipant> females = matchingParticipantRepository.findAll().stream()
                .filter(participant -> participant.getGender() == MatchingGender.FEMALE)
                .filter(participant -> participant.getStatus() == MatchingParticipantStatus.MATCHED)
                .toList();
        MatchingParticipant lateMale = matchingParticipantRepository.findById("male_3").orElseThrow();

        assertThat(males).hasSize(2);
        assertThat(females).hasSize(2);
        assertThat(lateMale.getStatus()).isEqualTo(MatchingParticipantStatus.UNMATCHED);
        assertThat(lateMale.getMatchedId()).isNull();
        assertThat(males).allSatisfy(male -> assertThat(male.getMatchedId()).startsWith("female_"));
        assertThat(females).allSatisfy(female -> assertThat(female.getMatchedId()).startsWith("male_"));
    }

    private void register(String instagramId, MatchingGender gender) throws Exception {
        matchingCommandService.register(new MatchingCreateRequest(instagramId, gender, "1234"));
        Thread.sleep(5);
    }
}

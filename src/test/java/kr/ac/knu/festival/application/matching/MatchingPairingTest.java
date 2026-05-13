package kr.ac.knu.festival.application.matching;

import kr.ac.knu.festival.domain.matching.entity.MatchingGender;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingPairingTest {

    private static final LocalDate DAY = LocalDate.parse("2026-05-20");

    @Test
    void noOneIsMatchedWhenOneSideIsEmpty() {
        List<MatchingParticipant> males = participants("m", 5, MatchingGender.MALE, 0);
        List<MatchingParticipant> females = List.of();

        MatchingPairing.Result result = new MatchingPairing(new Random(0)).pair(males, females);

        assertThat(result.matched()).isEmpty();
        assertThat(result.unmatched()).hasSize(5);
    }

    @Test
    void oneVsOneFallsBackToMutualMatch() {
        List<MatchingParticipant> males = participants("m", 1, MatchingGender.MALE, 0);
        List<MatchingParticipant> females = participants("f", 1, MatchingGender.FEMALE, 0);

        MatchingPairing.Result result = new MatchingPairing(new Random(0)).pair(males, females);

        // picker 행 2개 (남자→여자, 여자→남자)
        assertThat(result.matched()).hasSize(2);
        assertThat(result.unmatched()).isEmpty();
        // 양방향이라 서로 가리킴
        Map<String, String> pick = pickMap(result);
        assertThat(pick.get("m_0")).isEqualTo("f_0");
        assertThat(pick.get("f_0")).isEqualTo("m_0");
    }

    @Test
    void firstComeFirstServedCutoff() {
        // 남자 5명, 여자 3명. 매칭은 3쌍, 남자 후순위 2명은 미매칭.
        List<MatchingParticipant> males = participants("m", 5, MatchingGender.MALE, 0);
        List<MatchingParticipant> females = participants("f", 3, MatchingGender.FEMALE, 0);

        MatchingPairing.Result result = new MatchingPairing(new Random(42)).pair(males, females);

        // matched 행 = picker 행 = 3*2 = 6
        assertThat(result.matched()).hasSize(6);
        // 남자 m_3, m_4 만 unmatched (created 가 가장 늦은 두 명)
        Set<String> unmatchedIds = new HashSet<>();
        for (MatchingParticipant p : result.unmatched()) unmatchedIds.add(p.getInstagramId());
        assertThat(unmatchedIds).containsExactlyInAnyOrder("m_3", "m_4");
    }

    @RepeatedTest(20)
    void derangementGuaranteesNoMutualPick() {
        int n = 5;
        List<MatchingParticipant> males = participants("m", n, MatchingGender.MALE, 0);
        List<MatchingParticipant> females = participants("f", n, MatchingGender.FEMALE, 0);

        MatchingPairing.Result result = new MatchingPairing(new Random()).pair(males, females);

        Map<String, String> pick = pickMap(result);
        // 모든 남녀가 한 명씩 픽
        assertThat(pick).hasSize(2 * n);
        // 남자가 뽑힌 여자 분포: 모두 서로 다른 여자
        Set<String> femalesPicked = new HashSet<>();
        Set<String> malesPicked = new HashSet<>();
        for (var e : pick.entrySet()) {
            if (e.getKey().startsWith("m_")) femalesPicked.add(e.getValue());
            if (e.getKey().startsWith("f_")) malesPicked.add(e.getValue());
        }
        assertThat(femalesPicked).hasSize(n);
        assertThat(malesPicked).hasSize(n);

        // 서로 뽑는 페어가 없어야 한다
        for (var e : pick.entrySet()) {
            String picker = e.getKey();
            String picked = e.getValue();
            String pickedsPick = pick.get(picked);
            assertThat(pickedsPick)
                    .as("%s picks %s — %s must NOT pick %s back", picker, picked, picked, picker)
                    .isNotEqualTo(picker);
        }
    }

    private Map<String, String> pickMap(MatchingPairing.Result result) {
        Map<String, String> map = new HashMap<>();
        for (MatchingPairing.MatchedPair pair : result.matched()) {
            map.put(pair.picker().getInstagramId(), pair.picked().getInstagramId());
        }
        return map;
    }

    private List<MatchingParticipant> participants(String prefix, int count, MatchingGender gender, long createdOffsetSeconds) {
        List<MatchingParticipant> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            MatchingParticipant p = MatchingParticipant.create(
                    prefix + "_" + i, DAY, gender, "hash_" + prefix + i, "enc_" + prefix + i);
            setField(p, "id", (long) (gender == MatchingGender.MALE ? i + 1 : 100 + i + 1));
            // createdAt 을 i 순으로 증가시켜 선착순 결정성을 확보
            setField(p, "createdAt", LocalDateTime.of(2026, 5, 20, 11, 0, 0).plusSeconds(createdOffsetSeconds + i));
            list.add(p);
        }
        return list;
    }

    private void setField(Object target, String name, Object value) {
        try {
            Field f = findField(target.getClass(), name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        Class<?> c = cls;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}

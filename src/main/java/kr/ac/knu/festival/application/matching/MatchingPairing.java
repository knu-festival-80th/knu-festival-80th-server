package kr.ac.knu.festival.application.matching;

import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * 매칭 페어링 알고리즘.
 *
 * 정책:
 *  1) 선착순 컷오프 — 남녀 PENDING 을 created_at ASC 로 정렬, N = min(남,여) 명만 매칭에 참여시키고 나머지는 미매칭.
 *  2) 교란 순열(derangement) — 남자 i 는 여자 σ(i) 를 뽑고, 여자 σ(i) 는 남자 π(i) 를 뽑되 π(i) ≠ i 보장.
 *     서로 뽑는 페어가 절대 나오지 않는다.
 *  3) N == 1 예외 — 교란이 불가능하므로 1:1 양방향 매칭으로 fallback.
 */
public final class MatchingPairing {

    private static final Comparator<MatchingParticipant> BY_CREATED_THEN_ID =
            Comparator.comparing(MatchingParticipant::getCreatedAt)
                    .thenComparing(MatchingParticipant::getId);

    private final Random random;

    public MatchingPairing(Random random) {
        this.random = random;
    }

    public Result pair(List<MatchingParticipant> males, List<MatchingParticipant> females) {
        List<MatchingParticipant> sortedMales = sortedCopy(males);
        List<MatchingParticipant> sortedFemales = sortedCopy(females);
        int n = Math.min(sortedMales.size(), sortedFemales.size());

        List<MatchedPair> matched = new ArrayList<>();
        List<MatchingParticipant> unmatched = new ArrayList<>();

        if (n == 0) {
            unmatched.addAll(sortedMales);
            unmatched.addAll(sortedFemales);
            return new Result(matched, unmatched);
        }

        List<MatchingParticipant> activeMales = sortedMales.subList(0, n);
        List<MatchingParticipant> activeFemales = sortedFemales.subList(0, n);

        if (n == 1) {
            matched.add(new MatchedPair(activeMales.get(0), activeFemales.get(0)));
            matched.add(new MatchedPair(activeFemales.get(0), activeMales.get(0)));
        } else {
            int[] sigma = randomPermutation(n);
            int[] pi = randomDerangement(n);
            for (int i = 0; i < n; i++) {
                matched.add(new MatchedPair(activeMales.get(i), activeFemales.get(sigma[i])));
            }
            for (int i = 0; i < n; i++) {
                matched.add(new MatchedPair(activeFemales.get(sigma[i]), activeMales.get(pi[i])));
            }
        }

        unmatched.addAll(sortedMales.subList(n, sortedMales.size()));
        unmatched.addAll(sortedFemales.subList(n, sortedFemales.size()));
        return new Result(matched, unmatched);
    }

    private List<MatchingParticipant> sortedCopy(List<MatchingParticipant> input) {
        List<MatchingParticipant> copy = new ArrayList<>(input);
        copy.sort(BY_CREATED_THEN_ID);
        return copy;
    }

    private int[] randomPermutation(int n) {
        int[] p = new int[n];
        for (int i = 0; i < n; i++) p[i] = i;
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = p[i]; p[i] = p[j]; p[j] = tmp;
        }
        return p;
    }

    // 균등 분포 derangement: 거부 샘플링. 평균 1/e ≈ 36% 확률로 한 번에 성공.
    private int[] randomDerangement(int n) {
        while (true) {
            int[] p = randomPermutation(n);
            boolean fixedPointFound = false;
            for (int i = 0; i < n; i++) {
                if (p[i] == i) { fixedPointFound = true; break; }
            }
            if (!fixedPointFound) return p;
        }
    }

    public record MatchedPair(MatchingParticipant picker, MatchingParticipant picked) {}

    public record Result(List<MatchedPair> matched, List<MatchingParticipant> unmatched) {}
}

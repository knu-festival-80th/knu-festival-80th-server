package kr.ac.knu.festival.infra.canvas;

import kr.ac.knu.festival.domain.canvas.entity.CanvasBoard;
import kr.ac.knu.festival.domain.canvas.entity.CanvasBoardQuestion;
import kr.ac.knu.festival.domain.canvas.repository.CanvasBoardQuestionRepository;
import kr.ac.knu.festival.domain.canvas.repository.CanvasBoardRepository;
import kr.ac.knu.festival.domain.canvas.repository.CanvasPostitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CanvasDataInitializer implements CommandLineRunner {

    private static final int BOARDS_PER_QUESTION = 20;
    private static final int MAX_NOTE_COUNT = 100;

    private record QuestionSeed(String content, String description, int boardVariant) {}

    private static final List<QuestionSeed> QUESTIONS = List.of(
            new QuestionSeed("80주년 축하 메시지", "경북대 80주년을 위한 축하의 한마디", 1),
            new QuestionSeed("축제 공연 후기", "축제를 빛내준 아티스트에게 응원의 한마디!", 2),
            new QuestionSeed("주막/부스 후기", "방문한 부스, 주막, 먹거리 등 체험에 대한 실시간 후기", 3),
            new QuestionSeed("함께 온 사람에게", "오늘 축제를 함께한 너에게 전하고 싶은 말", 4),
            new QuestionSeed("경북대 대나무숲", "어떤 말이든 좋아요! 내 마음을 자유롭게 남겨보는 공간", 5)
    );

    private final CanvasBoardQuestionRepository questionRepository;
    private final CanvasBoardRepository boardRepository;
    private final CanvasPostitRepository postitRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<CanvasBoardQuestion> existing = questionRepository.findAllByOrderByOrderIndexAsc();

        if (existing.isEmpty()) {
            seedAll();
            return;
        }

        if (matchesSeed(existing)) return;

        log.info("Canvas question seed mismatch detected — clearing and re-seeding");
        postitRepository.deleteAllInBatch();
        boardRepository.deleteAllInBatch();
        questionRepository.deleteAllInBatch();
        seedAll();
    }

    private boolean matchesSeed(List<CanvasBoardQuestion> existing) {
        if (existing.size() != QUESTIONS.size()) return false;
        Map<Integer, CanvasBoardQuestion> byOrder = existing.stream()
                .collect(Collectors.toMap(CanvasBoardQuestion::getOrderIndex, q -> q));
        for (int i = 0; i < QUESTIONS.size(); i++) {
            QuestionSeed seed = QUESTIONS.get(i);
            CanvasBoardQuestion q = byOrder.get(i + 1);
            if (q == null) return false;
            if (!seed.content().equals(q.getContent())) return false;
            if (!seed.description().equals(q.getDescription())) return false;
            if (seed.boardVariant() != q.getBoardVariant()) return false;
        }
        return true;
    }

    private void seedAll() {
        for (int i = 0; i < QUESTIONS.size(); i++) {
            QuestionSeed seed = QUESTIONS.get(i);
            CanvasBoardQuestion question = questionRepository.save(
                    CanvasBoardQuestion.create(seed.content(), seed.description(), i + 1, seed.boardVariant())
            );
            for (int j = 1; j <= BOARDS_PER_QUESTION; j++) {
                boardRepository.save(CanvasBoard.create(question, MAX_NOTE_COUNT));
            }
        }
        log.info("Canvas seed complete: {} questions, {} boards each", QUESTIONS.size(), BOARDS_PER_QUESTION);
    }
}

package kr.ac.knu.festival.infra.canvas;

import kr.ac.knu.festival.domain.canvas.entity.CanvasBoard;
import kr.ac.knu.festival.domain.canvas.entity.CanvasBoardQuestion;
import kr.ac.knu.festival.domain.canvas.repository.CanvasBoardQuestionRepository;
import kr.ac.knu.festival.domain.canvas.repository.CanvasBoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CanvasDataInitializer implements CommandLineRunner {

    private static final int BOARDS_PER_QUESTION = 20;
    private static final int MAX_NOTE_COUNT = 100;
    private static final int BOARD_VARIANT_COUNT = 2;

    private record QuestionSeed(String content, String description) {}

    private static final List<QuestionSeed> QUESTIONS = List.of(
            new QuestionSeed("오늘의 기분을 작성해보세요", "지금 축제를 즐기는 마음을 남겨요"),
            new QuestionSeed("가장 맛있었던 주막 음식은?", "가장 맛있었던 메뉴를 공유해요"),
            new QuestionSeed("가장 재밌었던 공연은?", "기억에 남는 무대를 기록해요"),
            new QuestionSeed("오늘 가장 기억에 남는 순간은?", "가장 기억에 남는 장면을 남겨요"),
            new QuestionSeed("경북대학교 80주년에 남기고 싶은 말", "경북대학교 80주년 축하 메시지를 남겨요")
    );

    private final CanvasBoardQuestionRepository questionRepository;
    private final CanvasBoardRepository boardRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (questionRepository.count() > 0) return;

        for (int i = 0; i < QUESTIONS.size(); i++) {
            QuestionSeed seed = QUESTIONS.get(i);
            CanvasBoardQuestion question = questionRepository.save(
                    CanvasBoardQuestion.create(seed.content(), seed.description(), i + 1)
            );
            for (int j = 1; j <= BOARDS_PER_QUESTION; j++) {
                boardRepository.save(CanvasBoard.create(question, (j - 1) % BOARD_VARIANT_COUNT + 1, MAX_NOTE_COUNT));
            }
        }
    }
}

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

    private record QuestionSeed(String content, String description, int boardVariant) {}

    private static final List<QuestionSeed> QUESTIONS = List.of(
            new QuestionSeed("오늘의 기분", "오늘 가장 즐거웠던 순간, 기분을 남기는 공간", 1),
            new QuestionSeed("80주년 축하 메시지", "경북대 80주년, 축하의 한마디를 남겨주세요", 2),
            new QuestionSeed("축제 공연 후기", "축제를 빛내준 아티스트에게 응원의 한마디를 남겨주세요", 3),
            new QuestionSeed("주막/부스 후기", "방문한 부스, 주막, 먹거리 등 체험에 대한 후기를 남겨주세요", 4),
            new QuestionSeed("함께 온 사람에게", "오늘 축제를 함께한 너에게 전하고 싶은 말", 5),
            new QuestionSeed("대동제 피드백", "내년에는 대동제가 이렇게 바뀌었으면 좋겠어요!", 6)
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
                    CanvasBoardQuestion.create(seed.content(), seed.description(), i + 1, seed.boardVariant())
            );
            for (int j = 1; j <= BOARDS_PER_QUESTION; j++) {
                boardRepository.save(CanvasBoard.create(question, MAX_NOTE_COUNT));
            }
        }
    }
}

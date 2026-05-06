package kr.ac.knu.festival.application.canvas;

import kr.ac.knu.festival.domain.canvas.entity.CanvasPostit;
import kr.ac.knu.festival.domain.canvas.repository.CanvasPostitRepository;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import kr.ac.knu.festival.presentation.canvas.dto.request.CanvasPostitCreateRequest;
import kr.ac.knu.festival.presentation.canvas.dto.response.CanvasPostitCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class CanvasCommandService {

    private static final int BOARD_SIZE = 2000;
    private static final Set<Integer> VALID_SIZES = Set.of(104, 120, 136);

    private final CanvasPostitRepository canvasPostitRepository;

    public CanvasPostitCreateResponse createPostit(CanvasPostitCreateRequest request) {
        validateSize(request.width(), request.height());

        int currentZone = canvasPostitRepository.findMaxZoneNumber();
        List<CanvasPostit> existingPostits = canvasPostitRepository.findAllByZoneNumberOrderByIdAsc(currentZone);

        int[] adjusted = adjustPosition(
                request.positionX(), request.positionY(),
                request.width(), request.height(),
                existingPostits
        );

        CanvasPostit postit = CanvasPostit.createCanvasPostit(
                request.nickname(),
                request.message(),
                request.color(),
                adjusted[0],
                adjusted[1],
                request.width(),
                request.height()
        );

        canvasPostitRepository.save(postit);
        postit.assignZone();

        return CanvasPostitCreateResponse.fromEntity(postit);
    }

    public void deletePostit(Long postitId) {
        CanvasPostit postit = canvasPostitRepository.findById(postitId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CANVAS_POSTIT_NOT_FOUND));
        canvasPostitRepository.delete(postit);
    }

    private void validateSize(int width, int height) {
        if (!VALID_SIZES.contains(width) || !VALID_SIZES.contains(height)) {
            throw new BusinessException(BusinessErrorCode.INVALID_POSTIT_SIZE);
        }
    }

    private int[] adjustPosition(int x, int y, int width, int height, List<CanvasPostit> existingPostits) {
        if (noneFullyCovered(existingPostits, x, y, width, height)) {
            return new int[]{clamp(x, width), clamp(y, height)};
        }

        int[] offsets = {width / 4, width / 2};
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

        for (int offset : offsets) {
            for (int[] dir : directions) {
                int nx = clamp(x + dir[0] * offset, width);
                int ny = clamp(y + dir[1] * offset, height);
                if (noneFullyCovered(existingPostits, nx, ny, width, height)) {
                    return new int[]{nx, ny};
                }
            }
        }

        return new int[]{clamp(x, width), clamp(y, height)};
    }

    private boolean noneFullyCovered(List<CanvasPostit> postits, int x, int y, int width, int height) {
        return postits.stream().noneMatch(p -> fullyCovered(p, x, y, width, height));
    }

    private boolean fullyCovered(CanvasPostit existing, int x, int y, int width, int height) {
        return existing.getPositionX() >= x
                && existing.getPositionY() >= y
                && existing.getPositionX() + existing.getWidth() <= x + width
                && existing.getPositionY() + existing.getHeight() <= y + height;
    }

    private int clamp(int value, int size) {
        return Math.max(0, Math.min(value, BOARD_SIZE - size));
    }
}
package kr.ac.knu.festival.application.canvas;

import kr.ac.knu.festival.domain.canvas.repository.CanvasPostitRepository;
import kr.ac.knu.festival.presentation.canvas.dto.response.CanvasPostitResponse;
import kr.ac.knu.festival.presentation.canvas.dto.response.ZoneSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CanvasQueryService {

    private final CanvasPostitRepository canvasPostitRepository;

    public List<CanvasPostitResponse> getPostits(Integer zone) {
        int resolvedZone = (zone != null) ? zone : canvasPostitRepository.findMaxZoneNumber();
        return canvasPostitRepository.findAllByZoneNumberOrderByIdAsc(resolvedZone)
                .stream()
                .map(CanvasPostitResponse::fromEntity)
                .toList();
    }

    public List<ZoneSummaryResponse> getZoneSummaries() {
        return canvasPostitRepository.countByZone()
                .stream()
                .map(row -> new ZoneSummaryResponse(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }
}
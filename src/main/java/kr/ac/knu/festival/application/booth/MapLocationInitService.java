package kr.ac.knu.festival.application.booth;

import kr.ac.knu.festival.domain.booth.entity.Booth;
import kr.ac.knu.festival.domain.booth.entity.MapLocation;
import kr.ac.knu.festival.domain.booth.entity.MapLocationType;
import kr.ac.knu.festival.domain.booth.repository.BoothRepository;
import kr.ac.knu.festival.domain.booth.repository.MapLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MapLocationInitService {

    private static final long TAVERN_MAX_ID = 38;

    private final BoothRepository boothRepository;
    private final MapLocationRepository mapLocationRepository;

    public int initializeMapLocations() {
        List<Booth> booths = boothRepository.findAll();
        int created = 0;
        for (Booth booth : booths) {
            if (booth.getMapLocation() != null) continue;

            MapLocationType type = booth.getId() <= TAVERN_MAX_ID
                    ? MapLocationType.TAVERN
                    : MapLocationType.BOOTH;
            MapLocation ml = mapLocationRepository.save(MapLocation.of(null, null, type));
            booth.assignMapLocation(ml);
            created++;
        }
        return created;
    }
}

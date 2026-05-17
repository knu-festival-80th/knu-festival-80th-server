package kr.ac.knu.festival.domain.booth.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MapLocationType {

    TAVERN("#ff3d3d"),
    BOOTH("#15ccb1"),
    STAGE("#8B5CF6");

    private final String defaultColor;
}

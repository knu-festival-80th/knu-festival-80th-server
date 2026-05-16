package kr.ac.knu.festival.domain.booth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.ac.knu.festival.global.base.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "map_location")
public class MapLocation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "map_location_id")
    private Long id;

    @Column(name = "x_ratio", precision = 8, scale = 7)
    private BigDecimal xRatio;

    @Column(name = "y_ratio", precision = 8, scale = 7)
    private BigDecimal yRatio;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private MapLocationType type;

    @Column(name = "color", length = 20)
    private String color;

    public String getEffectiveColor() {
        return color != null ? color : type.getDefaultColor();
    }

    public static MapLocation of(BigDecimal xRatio, BigDecimal yRatio, MapLocationType type) {
        return MapLocation.builder()
                .xRatio(xRatio)
                .yRatio(yRatio)
                .type(type)
                .color(type.getDefaultColor())
                .build();
    }

    public void updateCoordinates(BigDecimal xRatio, BigDecimal yRatio) {
        if (xRatio != null) this.xRatio = xRatio;
        if (yRatio != null) this.yRatio = yRatio;
    }
}

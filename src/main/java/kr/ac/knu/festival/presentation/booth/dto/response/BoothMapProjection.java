package kr.ac.knu.festival.presentation.booth.dto.response;

import java.math.BigDecimal;

/**
 * 부스 지도 응답 전용 Spring Data JPA interface projection.
 * - 지도 뷰는 좌표/이름/식별자만 필요해 컬럼 단위로 잘라 SELECT 한다.
 * - getter 이름은 {@link kr.ac.knu.festival.domain.booth.entity.Booth} 의 property 명과 1:1 매칭되어야 한다.
 */
public interface BoothMapProjection {
    Long getId();
    String getName();
    BigDecimal getXRatio();
    BigDecimal getYRatio();
}

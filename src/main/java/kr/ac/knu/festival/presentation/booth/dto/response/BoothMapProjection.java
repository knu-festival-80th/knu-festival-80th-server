package kr.ac.knu.festival.presentation.booth.dto.response;

import java.math.BigDecimal;

/**
 * 부스 지도 응답 전용 JPQL constructor projection.
 * - 지도 뷰는 좌표/이름/식별자만 필요해 컬럼 단위로 잘라 SELECT 한다.
 * - {@link kr.ac.knu.festival.domain.booth.repository.BoothRepository#findAllProjectedBy()} 의 JPQL constructor expression 으로 인스턴스화된다.
 */
public record BoothMapProjection(Long id, String name, BigDecimal xRatio, BigDecimal yRatio) {
}

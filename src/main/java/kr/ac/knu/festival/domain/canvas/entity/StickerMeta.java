package kr.ac.knu.festival.domain.canvas.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * colorId별 스티커 크기 메타데이터 (보드 논리 캔버스 852px 기준, width 고정 80).
 * 높이 비율은 프론트 SVG 원본 크기 기반.
 */
@Getter
@RequiredArgsConstructor
public enum StickerMeta {
    STICKER_1(1, 80.0, 80.0 * 249.0 / 271.0),  // red
    STICKER_2(2, 80.0, 80.0 * 270.0 / 274.0),  // yellow
    STICKER_3(3, 80.0, 80.0 * 361.0 / 253.0),  // green
    STICKER_4(4, 80.0, 80.0 * 204.0 / 326.0),  // blue
    STICKER_5(5, 80.0, 80.0),                   // purple (259/259 = 1)
    STICKER_6(6, 80.0, 80.0);                   // pink   (271/271 = 1)

    private final int colorId;
    private final double width;
    private final double height;

    public static StickerMeta of(int colorId) {
        for (StickerMeta meta : values()) {
            if (meta.colorId == colorId) return meta;
        }
        throw new IllegalArgumentException("Invalid colorId: " + colorId);
    }
}

package kr.ac.knu.festival.presentation.upload.dto.response;

/**
 * 이미지 업로드 응답.
 * - path: DB에 저장할 상대 경로 (e.g. /uploads/images/{uuid}.jpg)
 * - url: 클라이언트가 즉시 표시할 수 있는 완전한 URL
 *
 * 클라이언트는 path를 가지고 booth/menu 의 imageUrl 필드에 다시 PUT/POST 한다.
 */
public record ImageUploadResponse(
        String url,
        String path
) {}

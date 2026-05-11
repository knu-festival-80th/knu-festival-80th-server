package kr.ac.knu.festival.presentation.upload.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ac.knu.festival.global.auth.AdminInfo;
import kr.ac.knu.festival.global.auth.CurrentAdmin;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.infra.storage.ImageStorageService;
import kr.ac.knu.festival.infra.storage.ImageUrlResolver;
import kr.ac.knu.festival.presentation.upload.dto.response.ImageUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Image Upload", description = "이미지 업로드 (관리자 전용 범용 엔드포인트)")
@RestController
@RequestMapping("/admin/uploads")
@RequiredArgsConstructor
public class ImageUploadController {

    private final ImageStorageService imageStorageService;
    private final ImageUrlResolver imageUrlResolver;

    @Operation(
            summary = "이미지 업로드",
            description = """
                    부스/메뉴/메뉴판 등 모든 이미지에 공용으로 사용하는 범용 엔드포인트.
                    multipart/form-data 의 file 필드로 파일을 업로드한다.
                    응답으로 받은 path 를 booth.imageUrl, booth.menuBoardImageUrl,
                    menu.imageUrl 등에 다시 PUT/POST 해서 연결한다.
                    """
    )
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
            @CurrentAdmin AdminInfo admin,
            @RequestParam("file") MultipartFile file
    ) {
        String path = imageStorageService.save(file);
        String url = imageUrlResolver.toPublicUrl(path);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(new ImageUploadResponse(url, path)));
    }
}

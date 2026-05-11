package kr.ac.knu.festival.presentation.canvas.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ac.knu.festival.global.auth.AdminInfo;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.canvas.dto.request.CanvasPostitCreateRequest;
import kr.ac.knu.festival.presentation.canvas.dto.response.CanvasPostitCreateResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "Canvas Command", description = "보드 생성 (슈퍼 관리자) / 포스트잇 생성 (사용자) / 포스트잇 삭제 (슈퍼 관리자)")
public interface CanvasCommandControllerDocs {

    @Operation(summary = "포스트잇 생성", description = "boardId·colorId·메시지·좌표(0~100 상대좌표, 중심점)를 입력해 롤링페이퍼 보드에 포스트잇을 추가합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 / 보드 경계 벗어남 / 금지 영역"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "보드 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "보드 가득 참 / 해당 위치에 포스트잇 충돌")
    })
    ResponseEntity<ApiResponse<CanvasPostitCreateResponse>> createPostit(
            CanvasPostitCreateRequest request
    );

    @Operation(summary = "보드 생성 (슈퍼 관리자)", description = "새 롤링페이퍼 보드를 생성합니다. boardVariant는 boardId 홀짝으로 자동 결정됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "슈퍼 관리자 권한 필요")
    })
    ResponseEntity<ApiResponse<Long>> createBoard(
            @Parameter(hidden = true) AdminInfo admin
    );

    @Operation(summary = "포스트잇 삭제 (슈퍼 관리자)", description = "부적절한 포스트잇을 소프트 딜리트합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "슈퍼 관리자 권한 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "포스트잇 없음")
    })
    ResponseEntity<ApiResponse<Void>> deletePostit(
            @Parameter(hidden = true) AdminInfo admin,
            Long postitId
    );
}

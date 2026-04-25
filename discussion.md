# 프로젝트 코드 컨벤션 가이드

## 목차
1. [패키지 구조](#1-패키지-구조)
2. [CQRS-lite 패턴](#2-cqrs-lite-패턴)
3. [Entity 작성 방식](#3-entity-작성-방식)
4. [DTO 작성 방식](#4-dto-작성-방식)
5. [Controller 작성 방식](#5-controller-작성-방식)
6. [예외 처리](#6-예외-처리)
7. [환경변수 관리](#7-환경변수-관리)
8. [네이밍 컨벤션](#8-네이밍-컨벤션)

---

## 1. 패키지 구조

```
src/main/java/{base-package}/
├── application/          # 유스케이스 (Service)
│   └── {domain}/
│       ├── {Domain}CommandService.java
│       └── {Domain}QueryService.java
├── presentation/         # 컨트롤러, DTO
│   └── {domain}/
│       ├── controller/
│       │   ├── {Domain}CommandController.java
│       │   ├── {Domain}QueryController.java
│       │   └── docs/
│       │       ├── {Domain}CommandControllerDocs.java
│       │       └── {Domain}QueryControllerDocs.java
│       └── dto/
│           ├── request/
│           └── response/
├── domain/               # Entity, Repository
│   └── {domain}/
│       ├── entity/
│       └── repository/
├── infra/                # 외부 시스템 연동 (S3, Redis 등)
└── global/               # 공통 컴포넌트
    ├── auth/
    ├── base/
    ├── config/
    └── exception/
```

`application`, `presentation`, `domain`은 내부를 도메인별 디렉토리로 구성한다.
`infra`, `global`은 도메인 구분 없이 기능별로 구성한다.

---

## 2. CQRS-lite 패턴

서비스 클래스는 **쓰기(Command)** 와 **읽기(Query)** 로 분리한다.

### CommandService — 생성 / 수정 / 삭제

```java
@Service
@RequiredArgsConstructor
@Transactional
public class NoticeCommandService {

    private final NoticeRepository noticeRepository;

    public NoticeResponse createNotice(NoticeCreateRequest request, Long memberId) {
        Notice notice = Notice.createNotice(request.title(), request.content(), memberId);
        return NoticeResponse.fromEntity(noticeRepository.save(notice));
    }

    public void deleteNotice(Long noticeId, Long memberId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.NOTICE_NOT_FOUND));
        validateAuthor(notice, memberId);
        noticeRepository.delete(notice);
    }

    private void validateAuthor(Notice notice, Long memberId) {
        if (!notice.getMemberId().equals(memberId)) {
            throw new BusinessException(BusinessErrorCode.ACCESS_DENIED);
        }
    }
}
```

### QueryService — 조회

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeQueryService {

    private final NoticeRepository noticeRepository;

    public List<NoticeListResponse> getNotices() {
        return noticeRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(NoticeListResponse::fromEntity)
                .toList();
    }

    public NoticeDetailResponse getNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.NOTICE_NOT_FOUND));
        return NoticeDetailResponse.fromEntity(notice);
    }
}
```

**규칙 요약**
- CommandService: 클래스 레벨 `@Transactional` (쓰기 트랜잭션)
- QueryService: 클래스 레벨 `@Transactional(readOnly = true)` (읽기 전용)
- 트랜잭션은 Service 레이어에서만 관리 — Controller에 `@Transactional` 금지
- `open-in-view: false` 설정 유지 → 지연 로딩은 Service 내에서 해결

---

## 3. Entity 작성 방식

```java
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "notice")
@SoftDelete(columnName = "deleted_at", strategy = SoftDeleteType.TIMESTAMP)
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoticeType type;

    @Column(name = "member_id")
    private Long memberId;

    // 정적 팩터리 메서드로 생성
    public static Notice createNotice(String title, String content, NoticeType type, Long memberId) {
        return Notice.builder()
                .title(title)
                .content(content)
                .type(type)
                .memberId(memberId)
                .build();
    }

    // 상태 변경 메서드
    public void updateNotice(String title, String content) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (content != null && !content.isBlank()) {
            this.content = content;
        }
    }
}
```

**규칙 요약**
- `@NoArgsConstructor(access = PROTECTED)`, `@AllArgsConstructor(access = PRIVATE)` 고정
- 직접 생성자 호출 금지 → 정적 팩터리 메서드(`createXxx`, `of`) 사용
- setter 미사용 → 상태 변경은 Entity 내부 메서드를 통해서만 수행
- 모든 Entity는 `BaseTimeEntity` 상속 (`createdAt`, `updatedAt` 자동 관리)
- 소프트 딜리트가 필요한 Entity에 `@SoftDelete(columnName = "deleted_at", strategy = SoftDeleteType.TIMESTAMP)` 적용

### BaseTimeEntity

```java
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

---

## 4. DTO 작성 방식

Request, Response 모두 **Java Record** 를 사용한다.

### Request

```java
public record NoticeCreateRequest(
        @NotBlank String title,
        @NotBlank String content,
        @NotBlank String type,
        LostFoundDetailRequest lostFoundDetail  // 중첩 record 활용
) {
    public record LostFoundDetailRequest(
            String foundPlace,
            String foundItem
    ) {}
}
```

### Response

```java
public record NoticeResponse(
        Long noticeId,
        String title,
        String content,
        String type
) {
    // Entity로부터 생성하는 정적 팩터리 메서드
    public static NoticeResponse fromEntity(Notice notice) {
        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getType().name()
        );
    }
}
```

**규칙 요약**
- 중첩 record로 계층적 구조 표현
- Response에는 반드시 `fromEntity()` 정적 팩터리 메서드 정의
- Request DTO는 `presentation/{domain}/dto/request/`, Response DTO는 `presentation/{domain}/dto/response/`에 위치

---

## 5. Controller 작성 방식

### CommandController

```java
@RestController
@RequestMapping("/admin/v1/notices")
@RequiredArgsConstructor
public class NoticeCommandController implements NoticeCommandControllerDocs {

    private final NoticeCommandService noticeCommandService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<NoticeResponse>> createNotice(
            @MemberId Long memberId,
            @RequestBody @Valid NoticeCreateRequest request
    ) {
        NoticeResponse result = noticeCommandService.createNotice(request, memberId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result));
    }

    @Override
    @DeleteMapping("/{notice-id}")
    public ResponseEntity<ApiResponse<?>> deleteNotice(
            @MemberId Long memberId,
            @PathVariable("notice-id") Long noticeId
    ) {
        noticeCommandService.deleteNotice(noticeId, memberId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
```

### QueryController

```java
@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class NoticeQueryController implements NoticeQueryControllerDocs {

    private final NoticeQueryService noticeQueryService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<List<NoticeListResponse>>> getNotices() {
        return ResponseEntity.ok(ApiResponse.success(noticeQueryService.getNotices()));
    }

    @Override
    @GetMapping("/{notice-id}")
    public ResponseEntity<ApiResponse<NoticeDetailResponse>> getNotice(
            @PathVariable("notice-id") Long noticeId
    ) {
        return ResponseEntity.ok(ApiResponse.success(noticeQueryService.getNotice(noticeId)));
    }
}
```

### Docs 인터페이스 (Swagger)

Swagger 어노테이션은 컨트롤러 구현체에 직접 작성하지 않고, `*Docs` 인터페이스에 분리한다.

```java
@Tag(name = "공지사항 Command", description = "공지사항 등록/수정/삭제 API")
public interface NoticeCommandControllerDocs {

    @Operation(summary = "공지사항 등록", description = "공지사항을 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 입력값"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    ResponseEntity<ApiResponse<NoticeResponse>> createNotice(
            @Parameter(hidden = true) Long memberId,
            NoticeCreateRequest request
    );
}
```

**규칙 요약**
- Command/Query 컨트롤러 분리
- 일반 사용자 API: `/api/v1/{resource}`, 관리자 API: `/admin/v1/{resource}`
- 모든 응답: `ResponseEntity<ApiResponse<T>>`
- Path variable: kebab-case로 작성, 이름 명시 (`@PathVariable("notice-id")`)
- Swagger 어노테이션은 `*Docs` 인터페이스에만 작성

---

## 6. 예외 처리

### 예외 던지기

```java
// Service에서 예외 발생
Notice notice = noticeRepository.findById(noticeId)
        .orElseThrow(() -> new BusinessException(BusinessErrorCode.NOTICE_NOT_FOUND));
```

### BusinessErrorCode 추가 방법

```java
@Getter
@RequiredArgsConstructor
public enum BusinessErrorCode implements ErrorCode {

    /*
     * 400 BAD_REQUEST
     */
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    MISSING_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C002", "필수 입력값이 누락되었습니다."),

    /*
     * 401 UNAUTHORIZED
     */
    UNAUTHORIZED_USER(HttpStatus.UNAUTHORIZED, "C003", "인증되지 않은 사용자입니다."),

    /*
     * 403 FORBIDDEN
     */
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C004", "접근 권한이 없습니다."),

    /*
     * 404 NOT_FOUND
     */
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "C101", "공지사항을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

**규칙 요약**
- HTTP 상태별로 주석 블록을 구분하여 에러 코드 추가
- 에러 코드(`C001` 등)는 팀 내 담당 범위를 정해 중복 방지

---

## 7. 환경변수 관리

### 구조

```
.env              ← 실제 값 (gitignore)
.env.example      ← 키 목록만 (레포 포함)
application.yml   ← 플레이스홀더로 주입
```

### .env

```
DB_URL=jdbc:mysql://localhost:3306/mydb
DB_USERNAME=root
DB_PASSWORD=secret
JWT_SECRET=my-jwt-secret
DDL_AUTO=validate
```

### .env.example

```
DB_URL=
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=
DDL_AUTO=
```

### application.yml

```yaml
spring:
  datasource:
    url: ${DB_URL}                           # 필수 — 값 없으면 앱 시작 실패
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: ${DDL_AUTO:none}             # 선택 — 없으면 기본값 none 사용

jwt:
  secret: ${JWT_SECRET}
  access-token-expiration: ${JWT_ACCESS_TOKEN_EXPIRATION:3600000}
```

### .gitignore

```
.env
```

**규칙 요약**
- 필수 환경변수: `${KEY}` — 미설정 시 애플리케이션 시작 실패
- 선택 환경변수: `${KEY:default}` — 미설정 시 기본값 사용
- `.env.example`을 항상 최신 상태로 유지하여 온보딩 비용 절감

---

## 8. 네이밍 컨벤션

| 대상 | 규칙 | 예시 |
|------|------|------|
| 클래스 | PascalCase | `NoticeCommandService` |
| 메서드 / 변수 | camelCase | `createNotice`, `noticeId` |
| 상수 | UPPER_SNAKE_CASE | `S3_FOLDER` |
| DB 테이블 / 컬럼 | snake_case | `notice`, `notice_id`, `created_at` |
| URL 경로 | kebab-case | `/api/v1/notices/{notice-id}` |
| 환경변수 | UPPER_SNAKE_CASE | `DB_URL`, `JWT_SECRET` |
| 패키지 | lowercase | `kr.co.example.domain.notice` |

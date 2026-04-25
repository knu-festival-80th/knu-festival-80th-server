# 코드 컨벤션

## 패키지 구조
```
application/{domain}/   → CommandService, QueryService
presentation/{domain}/  → controller/, controller/docs/, dto/request/, dto/response/
domain/{domain}/        → entity/, repository/
infra/                  → 외부 시스템 연동
global/                 → 공통 컴포넌트 (auth, base, config, exception)
```

## CQRS-lite
- 서비스는 `{Domain}CommandService` / `{Domain}QueryService`로 분리
- CommandService: 클래스 레벨 `@Transactional`
- QueryService: 클래스 레벨 `@Transactional(readOnly = true)`
- 트랜잭션은 Service 레이어에서만 관리 (Controller 금지)

## Entity
- 어노테이션: `@Entity @Getter @Builder @NoArgsConstructor(PROTECTED) @AllArgsConstructor(PRIVATE) @Table`
- 소프트 딜리트: `@SoftDelete(columnName = "deleted_at", strategy = SoftDeleteType.TIMESTAMP)`
- 모든 Entity는 `BaseTimeEntity` 상속
- 생성은 정적 팩터리 메서드(`createXxx`, `of`)로, setter 미사용
- 상태 변경은 Entity 내부 메서드로

## DTO
- Request/Response 모두 Java Record 사용
- Response에는 `fromEntity()` 정적 팩터리 메서드 정의

## Controller
- Command/Query 컨트롤러 분리
- Swagger 어노테이션은 `*Docs` 인터페이스에만 작성, 컨트롤러는 이를 구현
- 일반 사용자 API: `/api/v1/{resource}`, 관리자 API: `/admin/v1/{resource}`
- 모든 응답: `ResponseEntity<ApiResponse<T>>`
- Path variable: kebab-case, 이름 명시 (`@PathVariable("notice-id")`)

## 예외 처리
- `throw new BusinessException(BusinessErrorCode.XXX)` 형태로 던지기
- `BusinessErrorCode` enum에 HTTP 상태별로 그룹화하여 추가

## 환경변수
- 값은 `.env`에 저장, `.gitignore`에 등록
- `application.yml`에서 `${KEY}` (필수) / `${KEY:default}` (선택) 형태로 주입
- `.env.example`을 레포에 포함하여 필요한 키 목록 관리

## 네이밍
- 클래스: PascalCase / 메서드·변수: camelCase / 상수: UPPER_SNAKE_CASE
- DB 컬럼·테이블: snake_case / URL: kebab-case / 환경변수: UPPER_SNAKE_CASE

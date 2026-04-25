# 기술/개발 명세서 (TS)

> **프로젝트**: 2026 경북대학교 80주년 대동제 웹앱 서비스 (백엔드)  
> **버전**: v1.1  
> **최종 수정일**: 2026-04-25  
> **목적**: Verification 기준 문서 — "구현이 명세를 충족하는가?"

---

### 변경 이력

| 버전 | 날짜 | 변경 내용 | 변경자 |
|------|------|-----------|--------|
| v1.0 | 2026-04-25 | 초안 작성 | - |
| v1.1 | 2026-04-25 | 대기열 스키마 재설계, 인덱스 추가, 캐싱/WebSocket 설계 보완, 일정 제거 | - |

---

## 1. 기술 스택

| 영역 | 기술 | 버전 |
|------|------|------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.x |
| Build | Gradle (Groovy DSL) | 8.x |
| ORM | Spring Data JPA + Hibernate 6 | - |
| DB | MySQL | 8.0+ |
| Cache | Redis (Lettuce) | 7.0+ |
| WebSocket | Spring WebSocket + STOMP | - |
| 인증 | Spring Security + JWT | - |
| API 문서 | SpringDoc OpenAPI (Swagger UI) | 2.x |
| 파일 저장 | AWS S3 (또는 호환 스토리지) | - |
| SMS | 외부 SMS API (알리고 등) | - |
| 배포 | Docker + GitHub Actions | - |
| 모니터링 | Spring Actuator + Slack Webhook | - |

---

## 2. 프로젝트 구조

```
src/main/java/kr/ac/knu/festival/
├── application/
│   ├── festival/
│   ├── booth/
│   ├── performance/
│   ├── notice/
│   ├── waiting/
│   ├── review/
│   ├── canvas/
│   ├── matching/
│   ├── photo/
│   ├── feed/
│   └── member/
├── presentation/
│   ├── festival/
│   ├── booth/
│   ├── performance/
│   ├── notice/
│   ├── waiting/
│   ├── review/
│   ├── canvas/
│   ├── matching/
│   ├── photo/
│   ├── feed/
│   └── member/
├── domain/
│   ├── festival/
│   ├── booth/
│   ├── performance/
│   ├── notice/
│   ├── waiting/
│   ├── review/
│   ├── canvas/
│   ├── matching/
│   ├── photo/
│   ├── feed/
│   └── member/
├── infra/
│   ├── s3/
│   ├── sms/
│   └── redis/
└── global/
    ├── auth/
    ├── base/
    ├── config/
    └── exception/
```

각 도메인은 CLAUDE.md 컨벤션에 따라 `CommandService/QueryService`, `CommandController/QueryController + Docs`, `entity/`, `repository/`, `dto/request/`, `dto/response/`로 구성한다.

---

## 3. DB 스키마 설계

### 3.1 festival (축제 정보)

```sql
CREATE TABLE festival (
    festival_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(200) NOT NULL,
    start_date   DATETIME NOT NULL,
    end_date     DATETIME NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'BEFORE',
    created_at   DATETIME NOT NULL,
    updated_at   DATETIME NOT NULL
);
-- status: BEFORE, IN_PROGRESS, ENDED
```

### 3.2 booth (부스/주막)

```sql
CREATE TABLE booth (
    booth_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    location_lat    DECIMAL(10, 7),
    location_lng    DECIMAL(10, 7),
    like_count      INT DEFAULT 0,
    image_url       VARCHAR(500),
    is_waiting_open BOOLEAN DEFAULT TRUE,
    created_at      DATETIME NOT NULL,
    updated_at      DATETIME NOT NULL,
    deleted_at      DATETIME
);
```

### 3.3 menu (메뉴)

```sql
CREATE TABLE menu (
    menu_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    booth_id    BIGINT NOT NULL,
    name        VARCHAR(100) NOT NULL,
    price       INT NOT NULL,
    image_url   VARCHAR(500),
    description TEXT,
    is_sold_out BOOLEAN DEFAULT FALSE,
    created_at  DATETIME NOT NULL,
    updated_at  DATETIME NOT NULL,
    deleted_at  DATETIME,
    FOREIGN KEY (booth_id) REFERENCES booth(booth_id)
);
```

### 3.4 performance (공연)

```sql
CREATE TABLE performance (
    performance_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title          VARCHAR(200) NOT NULL,
    artist         VARCHAR(200),
    venue          VARCHAR(200),
    venue_lat      DECIMAL(10, 7),
    venue_lng      DECIMAL(10, 7),
    start_time     DATETIME NOT NULL,
    end_time       DATETIME NOT NULL,
    description    TEXT,
    image_url      VARCHAR(500),
    created_at     DATETIME NOT NULL,
    updated_at     DATETIME NOT NULL,
    deleted_at     DATETIME,
    INDEX idx_performance_time (start_time, end_time)
);
```

### 3.5 notice (공지사항)

```sql
CREATE TABLE notice (
    notice_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(100) NOT NULL,
    content     TEXT NOT NULL,
    notice_type VARCHAR(20) NOT NULL DEFAULT 'GENERAL',
    is_pinned   BOOLEAN DEFAULT FALSE,
    expires_at  DATETIME,
    member_id   BIGINT,
    created_at  DATETIME NOT NULL,
    updated_at  DATETIME NOT NULL,
    deleted_at  DATETIME,
    FOREIGN KEY (member_id) REFERENCES member(member_id),
    INDEX idx_notice_type_pinned (notice_type, is_pinned)
);
-- notice_type: EMERGENCY, GENERAL, WEATHER, SHUTTLE
```

### 3.6 waiting (대기열)

```sql
CREATE TABLE waiting (
    waiting_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    booth_id       BIGINT NOT NULL,
    waiting_number INT NOT NULL,
    sort_order     INT NOT NULL,
    name           VARCHAR(50) NOT NULL,
    party_size     INT NOT NULL,
    phone_number   VARCHAR(100) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    sms_sent       BOOLEAN DEFAULT FALSE,
    called_at      DATETIME,
    entered_at     DATETIME,
    created_at     DATETIME NOT NULL,
    updated_at     DATETIME NOT NULL,
    deleted_at     DATETIME,
    FOREIGN KEY (booth_id) REFERENCES booth(booth_id),
    INDEX idx_waiting_booth_status (booth_id, status),
    INDEX idx_waiting_booth_sort (booth_id, sort_order)
);
-- status: WAITING, CALLED, ENTERED, SKIPPED, CANCELLED
-- phone_number: 암호화된 값 저장 (VARCHAR(100)으로 여유 확보)
-- sort_order: 대기열 순서 관리 (중간 삽입/순서 변경용)
```

### 3.7 review (리뷰)

```sql
CREATE TABLE review (
    review_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    booth_id   BIGINT NOT NULL,
    nickname   VARCHAR(30) NOT NULL,
    password   VARCHAR(100) NOT NULL,
    content    TEXT NOT NULL,
    rating     INT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME,
    FOREIGN KEY (booth_id) REFERENCES booth(booth_id),
    CONSTRAINT chk_rating CHECK (rating BETWEEN 1 AND 5)
);
-- password: 해시 저장 (본인 삭제용)
```

### 3.8 canvas_element (캔버스 요소)

```sql
CREATE TABLE canvas_element (
    element_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    element_type VARCHAR(20) NOT NULL,
    data         JSON NOT NULL,
    position_x   DOUBLE NOT NULL,
    position_y   DOUBLE NOT NULL,
    session_id   VARCHAR(100) NOT NULL,
    created_at   DATETIME NOT NULL,
    updated_at   DATETIME NOT NULL,
    INDEX idx_canvas_position (position_x, position_y)
);
-- element_type: DRAW, TEXT, STICKER
-- session_id: 사용자 식별용 임시 토큰 (스티커 개수 제한 등에 사용)
```

### 3.9 matching_participant (매칭)

```sql
CREATE TABLE matching_participant (
    instagram_id VARCHAR(100) PRIMARY KEY,
    gender       VARCHAR(10) NOT NULL,
    password     VARCHAR(100) NOT NULL,
    nationality  VARCHAR(10) DEFAULT 'KR',
    matched_id   VARCHAR(100),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at   DATETIME NOT NULL,
    updated_at   DATETIME NOT NULL,
    INDEX idx_matching_status_gender (status, gender)
);
-- status: PENDING, MATCHED, UNMATCHED
-- password: BCrypt 해시 저장
```

### 3.10 feed (피드)

```sql
CREATE TABLE feed (
    feed_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_url    VARCHAR(500) NOT NULL,
    caption      TEXT,
    instagram_id VARCHAR(100),
    like_count   INT DEFAULT 0,
    created_at   DATETIME NOT NULL,
    updated_at   DATETIME NOT NULL,
    deleted_at   DATETIME,
    INDEX idx_feed_like_count (like_count DESC)
);
```

### 3.11 member (관리자)

```sql
CREATE TABLE member (
    member_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20) NOT NULL DEFAULT 'BOOTH_ADMIN',
    booth_id   BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (booth_id) REFERENCES booth(booth_id)
);
-- role: SUPER_ADMIN, BOOTH_ADMIN
-- BOOTH_ADMIN은 booth_id로 담당 부스 연결
-- password: BCrypt 해시 저장
```

### 3.12 photo_asset (포토부스 에셋)

```sql
CREATE TABLE photo_asset (
    asset_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_type VARCHAR(20) NOT NULL,
    name       VARCHAR(100) NOT NULL,
    image_url  VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME
);
-- asset_type: FRAME, STICKER
```

---

## 4. 인증/인가 아키텍처

```
[Request]
    │
    ▼
JwtAuthenticationFilter
    │
    ├── /api/** → 통과 (인증 불필요)
    │
    └── /admin/** → JWT 검증
            │
            ├── 유효 → SecurityContext에 인증 정보 설정
            │         │
            │         ├── SUPER_ADMIN → 모든 /admin/** 접근 가능
            │         └── BOOTH_ADMIN → 담당 부스 리소스만 접근 가능
            │
            └── 무효 → 401 Unauthorized
```

- Access Token: Authorization 헤더 (`Bearer {token}`)
- Refresh Token: Redis에 저장, 로그아웃 시 삭제
- `@MemberId` 커스텀 어노테이션으로 인증된 관리자 ID 주입
- BOOTH_ADMIN의 부스 소유권 검증: 요청 대상 booth-id와 관리자의 담당 booth-id 일치 확인

---

## 5. 캐싱 전략

| 대상 | Redis 자료구조 | 갱신 전략 |
|------|---------------|-----------|
| 부스 랭킹 | Sorted Set (`booth:ranking`) | 좋아요 시 ZINCRBY, 조회 시 ZREVRANGE |
| 부스 좋아요 수 | Sorted Set 스코어로 통합 관리 | 주기적 DB 동기화 |
| 공연 목록 | String (`performance:list`) TTL 5분 | 변경 시 키 삭제 |
| 대기열 현황 | String (`waiting:{booth-id}:count`) TTL 10초 | 변경 시 키 삭제 |
| 축제 상태 | String (`festival:info`) TTL 1분 | 변경 시 키 삭제 |
| Refresh Token | String (`refresh:{member-id}`) TTL 7일 | 로그아웃 시 삭제 |

---

## 6. WebSocket 설계

### 캔버스 실시간 통신

```
Endpoint: /ws/canvas
Protocol: STOMP over WebSocket

Subscribe:
  /topic/canvas/{region-id}  → 해당 영역의 실시간 업데이트 수신

Send:
  /app/canvas/draw     → 드로잉 데이터 전송
  /app/canvas/text     → 텍스트 데이터 전송
  /app/canvas/sticker  → 스티커 데이터 전송
```

**설계 원칙**
- 데이터 포맷: JSON (STOMP 프로토콜 호환, 바이너리 전환은 성능 병목 확인 후 검토)
- 영역 분할: 캔버스를 그리드로 분할, 각 그리드를 `region-id`로 식별
- 클라이언트가 화면 이동 시 구독 토픽을 동적으로 변경
- Redis Pub/Sub: 멀티 인스턴스 환경에서 메시지 브로드캐스트
- 재연결: 클라이언트 자동 재연결 시 REST API(`GET /api/v1/canvas/elements`)로 현재 상태 복구
- 영속화: 인메모리 버퍼에 쌓았다가 주기적으로 DB 배치 INSERT

---

## 7. 외부 서비스 연동

### TS-SMS-01: SMS 발송

- 대기 호출 시 SMS API를 통해 알림 발송
- 비동기 처리 (별도 스레드풀, 무제한 스레드 생성 방지)
- 타임아웃: 연결 3초, 읽기 5초
- 발송 실패 시 재시도 (최대 3회), 실패 시 `sms_sent=false` 유지 → 관리자 재발송 가능
- 발송 성공 시 `sms_sent=true` 업데이트

### TS-S3-01: 파일 업로드

- 이미지 업로드 경로: `{domain}/{yyyy-MM-dd}/{uuid}.{ext}`
- 허용 확장자: jpg, png, webp
- 최대 파일 크기: 10MB
- 업로드 후 CDN URL 반환
- 피드에 연결되지 않은 임시 파일: 24시간 후 정리

---

## 8. 배치 처리

### 매칭 일괄 실행

- 트리거: 관리자 수동 실행 또는 스케줄러
- 로직:
  1. `PENDING` 상태의 참가자를 성별로 분류
  2. 각 그룹을 셔플 후 1:1 매칭
  3. 매칭된 쌍: `MATCHED`로 변경
  4. 미매칭 인원: `UNMATCHED`로 변경
- 멱등성: 이미 MATCHED 상태인 참가자는 제외

### 좋아요 카운트 DB 동기화

- Redis Sorted Set의 스코어를 주기적으로 DB에 반영
- 변경된 부스만 선택적으로 업데이트

### 대기열 자동 SKIP 처리

- 호출(CALLED) 후 5분 경과 시 자동으로 SKIPPED 상태 전환
- 구현 방식: 스케줄러 주기 실행 또는 관리자 수동 처리

---

## 9. 환경 설정

### .env.example

```
# Database
DB_URL=jdbc:mysql://localhost:3306/knu_festival
DB_USERNAME=
DB_PASSWORD=
DDL_AUTO=update

# JWT
JWT_SECRET=
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# AWS S3
S3_BUCKET=
AWS_REGION=ap-northeast-2
AWS_ACCESS_KEY=
AWS_SECRET_KEY=

# CDN
CDN_BASE_URL=

# SMS
SMS_API_KEY=
SMS_SENDER_NUMBER=

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000

# Profile
SPRING_PROFILES_ACTIVE=local
```

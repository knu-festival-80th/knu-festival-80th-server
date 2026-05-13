# 기능 명세서 (FS: Functional Specification)

> **프로젝트**: 2026 경북대학교 80주년 대동제 웹앱 서비스 (백엔드)  
> **버전**: v1.12
> **최종 수정일**: 2026-05-13  
> **목적**: 백엔드가 제공해야 할 API와 비즈니스 로직을 기능 단위로 정의한다.

---

### 변경 이력

| 버전 | 날짜 | 변경 내용 | 변경자 |
|------|------|-----------|--------|
| v1.0 | 2026-04-25 | 초안 작성 | - |
| v1.1 | 2026-04-25 | 대기열 현장 전용 재설계, API 보안/설계 이슈 반영, 누락 API 추가 | - |
| v1.2 | 2026-04-27 | 대기열을 웹 온라인 등록 모델로 전환 (현장 태블릿 전제 폐기), 본인 취소 API 추가, 어뷰즈 방지 규칙 보강 | - |
| v1.3 | 2026-04-27 | 관리자 인증을 부스별 비밀번호 + 세션 기반으로 단순화, member 도메인 제거, 부스 비밀번호 변경 API 추가, 모든 admin API에 소유권 검증 적용 | - |
| v1.4 | 2026-05-03 | 관리자 API 경로를 권한 단위로 분리: 슈퍼 전용은 `/admin/v1/super/**`, 슈퍼+부스 공통은 `/admin/v1/booth/**`. 구현된 booth/menu/waiting 엔드포인트 경로 갱신, SecurityConfig path 매처 추가 (※ v1.5 에서 root 기준으로 다시 통합) | - |
| v1.5 | 2026-05-03 | 배포 환경 ingress(`/festival/api` strip)에 맞춰 모든 API 경로를 root 기준으로 재매핑. `/api/v1/`·`/admin/v1/` prefix 제거, super/booth path 분리도 단일 `/admin/` 으로 통합(슈퍼 전용은 HTTP 메서드+path 조합으로 SecurityConfig 에서 분기). 미구현 엔드포인트 path 표기도 동일 컨벤션으로 정정 | - |
| v1.6 | 2026-05-11 | 솔라피 알림톡 연동, 자동스킵 10분, 예약 제한(전체 3건+이름 검증), 입장확정 시 타부스 자동취소, 내 예약 전체 조회 API 추가 | lsmin3388 |
| v1.7 | 2026-05-12 | canvas(롤링페이퍼) 도메인 기능 명세 추가 — REST API 기반 포스트잇 보드로 재설계 (WebSocket 제거) | milk-stone |
| v1.8 | 2026-05-13 | 3.8절 canvas API 전면 개편 — 문항(Question) 기반 구조 도입, zone→board 전환, colorId/0~100 좌표계 반영 | milk-stone |
| v1.9 | 2026-05-13 | 3.9절 matching(인스타팅) 전면 개편 — 일별 윈도우(매일 11–21 신청 / 22–익11 결과), phone 기반 인증, nationality·cancel 제거, applicants/count(성별 분리) 엔드포인트 신설 | - |
| v1.10 | 2026-05-13 | 3.9절 매칭 알고리즘 변경 — 성비 70% pause 폐기, 선착순 컷오프 + 교란 순열로 전환. 결과 응답 `matchedInstagramId` → `pickedInstagramId` (의미: "내가 뽑은 상대") | - |
| v1.11 | 2026-05-13 | 3.8절 canvas API 경로 정정 — `/api/v1/canvas`·`/admin/v1/canvas` → `/canvas`·`/admin/canvas` (v1.5 root-prefix 컨벤션과 어긋난 누락분 정리) | - |
| v1.11.1 | 2026-05-13 | BR-AUTH-03 표기 정정 — 옛 `/api/**` 잔존 표현 → `/admin/**` 외 (root-prefix 컨벤션) | - |
| v1.12 | 2026-05-13 | 3.8절 BR-RP-09 추가 — Gemini AI 비동기 내용 검열 규칙 신설 | milk-stone |

---

## 1. 문서 개요

사용자 요구사항(01-requirements-spec.md)을 **백엔드 API 관점**으로 변환한 문서이다.  
각 기능에 대해 API 엔드포인트, 비즈니스 규칙을 정의한다.

---

## 2. 도메인 목록

| 도메인 | 설명 | 관련 요구사항 |
|--------|------|---------------|
| `festival` | 축제 일정/상태 정보 | REQ-HOM-01 |
| `booth` | 주막/부스 정보 및 랭킹 | REQ-HOM-02, REQ-USR-01 |
| `performance` | 공연 정보 | REQ-HOM-03 |
| `notice` | 공지사항 (유형별) | REQ-HOM-04 |
| `waiting` | 주막 대기열 관리 (웹 온라인 등록) | REQ-USR-02 |
| `review` | 주막 리뷰 | REQ-USR-01 |
| `canvas` | 롤링페이퍼 (실시간 캔버스) | REQ-USR-03 |
| `matching` | 두근두근 인스타팅 | REQ-USR-04 |
| `photo` | 웹 포토부스 | REQ-USR-05 |
| `feed` | 호반우스타그램 피드 | REQ-USR-05 |
| `auth` | 관리자 인증 (부스별 비밀번호 + 세션) | REQ-ADM-03 |

---

## 3. 기능 명세

### 3.1 축제 정보 (festival)

#### FS-HOM-01: 축제 일정/상태

**API 목록**

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/festival/info` | 축제 일정 및 현재 상태 조회 | 불필요 |
| PATCH | `/admin/festival/info` | 축제 일정 수정 | 슈퍼 관리자 |

**응답 포함 항목**
- 축제 시작/종료 일시
- 현재 상태 (`BEFORE` / `IN_PROGRESS` / `ENDED`)

---

### 3.2 부스/주막 (booth)

#### FS-HOM-02: 부스 목록 및 랭킹

**API 목록**

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/booths` | 부스 목록 조회 | 불필요 |
| GET | `/booths/{booth-id}` | 부스 상세 조회 (메뉴, 대기 현황 포함) | 불필요 |
| GET | `/booths/map` | 지도용 부스 목록 (좌표 + 이름만, 경량) | 불필요 |
| POST | `/booths/{booth-id}/likes` | 부스 좋아요 | 불필요 |
| DELETE | `/booths/{booth-id}/likes` | 부스 좋아요 취소 | 불필요 |
| GET | `/admin/booths` | 관리자용 부스 목록 조회 | 관리자 (booth+) |
| POST | `/admin/booths` | 부스 등록 | 슈퍼 관리자 |
| PUT | `/admin/booths/{booth-id}` | 부스 정보 수정 | 관리자 (booth+) |
| DELETE | `/admin/booths/{booth-id}` | 부스 삭제 | 슈퍼 관리자 |

**Query Parameters** (GET `/booths`)
- `sort`: `likes` (기본) / `waiting-asc` — 정렬 기준

**비즈니스 규칙**
- BR-BOOTH-01: 부스 목록 조회 시 좋아요 수 기준 내림차순 정렬 (기본), 대기 적은 순 정렬 옵션 제공
- BR-BOOTH-02: 좋아요는 부스당 1회 제한 (세션 토큰 기반 서버 검증 + 프론트 LocalStorage 병행)
- BR-BOOTH-03: 좋아요 수 집계는 Redis 캐시를 통해 실시간 반영
- BR-BOOTH-04: 부스 상세 조회는 메뉴 목록, 현재 대기팀 수, 좋아요 수를 포함
- BR-BOOTH-05: 부스 삭제 시 대기 중인 팀이 있으면 삭제 불가 (400 응답)

---

### 3.3 메뉴 관리 (booth → menu)

#### FS-ADM-01-MENU: 메뉴 등록/수정/품절 처리

**API 목록**

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/admin/booths/{booth-id}/menus` | 메뉴 목록 조회 | 관리자 (booth+) |
| POST | `/admin/booths/{booth-id}/menus` | 메뉴 등록 | 관리자 (booth+) |
| PUT | `/admin/booths/{booth-id}/menus/{menu-id}` | 메뉴 수정 | 관리자 (booth+) |
| PATCH | `/admin/booths/{booth-id}/menus/{menu-id}/sold-out` | 품절 상태 토글 | 관리자 (booth+) |
| DELETE | `/admin/booths/{booth-id}/menus/{menu-id}` | 메뉴 삭제 | 관리자 (booth+) |

**비즈니스 규칙**
- BR-MENU-01: 메뉴 사진은 S3에 업로드, URL만 DB에 저장
- BR-MENU-02: 품절 상태 변경은 토글 방식
- BR-MENU-03: 부스 관리자는 자신의 담당 부스 메뉴만 관리 가능

---

### 3.4 공연 정보 (performance)

#### FS-HOM-03: 공연 정보 조회

**API 목록**

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/performances` | 전체 공연 목록 조회 | 불필요 |
| GET | `/performances/current` | 현재 진행 중/직후 공연 조회 | 불필요 |
| POST | `/admin/performances` | 공연 등록 | 슈퍼 관리자 |
| PUT | `/admin/performances/{performance-id}` | 공연 수정 | 슈퍼 관리자 |
| DELETE | `/admin/performances/{performance-id}` | 공연 삭제 | 슈퍼 관리자 |

**Query Parameters** (GET `/performances`)
- `date`: `2026-05-01` — 날짜별 필터링

**비즈니스 규칙**
- BR-PERF-01: `/current` 엔드포인트는 서버 시각 기준으로 진행 중인 공연 + 직후 1건 반환
- BR-PERF-02: 공연 목록은 일자별 → 시간순 정렬
- BR-PERF-03: 공연에 무대명, 위치 좌표 포함 (지도 표시용)

---

### 3.5 공지사항 (notice)

#### FS-HOM-04: 공지 관리

**API 목록**

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/notices` | 공지사항 목록 조회 | 불필요 |
| GET | `/notices/{notice-id}` | 공지사항 상세 조회 | 불필요 |
| POST | `/admin/notices` | 공지사항 등록 | 관리자 |
| PUT | `/admin/notices/{notice-id}` | 공지사항 수정 | 관리자 |
| DELETE | `/admin/notices/{notice-id}` | 공지사항 삭제 | 관리자 |

**Query Parameters** (GET `/notices`)
- `type`: `EMERGENCY` / `GENERAL` / `WEATHER` / `SHUTTLE` — 유형 필터

**비즈니스 규칙**
- BR-NOTICE-01: 긴급 공지는 `is_pinned=true`일 때 상단 고정
- BR-NOTICE-02: 최신순 정렬, 고정 공지 우선 표시
- BR-NOTICE-03: `expires_at`이 지난 공지는 목록에서 자동 제외
- BR-NOTICE-04: 공지 유형(notice_type)으로 긴급/일반/날씨/셔틀 등 구분

---

### 3.6 주막 대기열 관리 (waiting) — 웹 온라인 등록

> 손님은 축제 웹앱에서 직접 대기 등록·현황 조회·내 대기 조회·본인 취소까지 모두 수행한다. 별도 현장 태블릿은 두지 않는다.

#### FS-USR-02-USER: 손님 화면 (웹)

**API 목록**

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/booths/{booth-id}/waitings` | 대기 등록 (웹) | 불필요 |
| GET | `/booths/{booth-id}/waitings/status` | 현재 대기 현황 조회 (남은 팀 수) | 불필요 |
| GET | `/waitings/{waiting-id}` | 내 대기 상태 조회 | 전화번호 뒤 4자리 검증 |
| POST | `/waitings/my` | 내 예약 전체 조회 (이름+전화번호) | 불필요 |
| DELETE | `/waitings/{waiting-id}` | 본인 대기 취소 | 전화번호 뒤 4자리 검증 |

**대기 등록 요청**
```
{
  "name": "홍길동",
  "partySize": 4,
  "phoneNumber": "010-1234-5678"
}
```

**대기 등록 응답**
```
{
  "waitingId": 123,
  "waitingNumber": 45,
  "boothName": "컴공 주막",
  "currentWaitingTeams": 12,
  "estimatedWaitMinutes": 30
}
```

#### FS-USR-02-ADMIN: 대기열 관리 (관리자)

**API 목록**

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/admin/booths/{booth-id}/waitings` | 대기팀 목록 조회 | 관리자 (booth+) |
| PATCH | `/admin/waitings/{waiting-id}/call` | 대기팀 호출 (SMS 발송) | 관리자 (booth+) |
| PATCH | `/admin/waitings/{waiting-id}/enter` | 입장 완료 처리 | 관리자 (booth+) |
| PATCH | `/admin/waitings/{waiting-id}/cancel` | 관리자가 대기 취소 | 관리자 (booth+) |
| PATCH | `/admin/waitings/{waiting-id}/skip` | 미방문 건너뛰기 | 관리자 (booth+) |
| POST | `/admin/booths/{booth-id}/waitings/insert` | 대기열 중간 삽입 | 관리자 (booth+) |
| PATCH | `/admin/waitings/{waiting-id}/reorder` | 대기 순서 변경 | 관리자 (booth+) |
| PATCH | `/admin/booths/{booth-id}/waitings/toggle` | 대기 접수 ON/OFF | 관리자 (booth+) |
| POST | `/admin/waitings/{waiting-id}/resend-sms` | SMS 재발송 | 관리자 (booth+) |

**Query Parameters** (GET 대기팀 목록)
- `status`: `WAITING` / `CALLED` / `ENTERED` / `SKIPPED` / `CANCELLED` — 상태 필터

**비즈니스 규칙**
- BR-WAIT-01: 호출(CALL) 시 해당 사용자에게 알림톡 발송 (비동기, 실패해도 상태는 CALLED로 전환)
- BR-WAIT-02: 대기 상태 흐름: `WAITING` → `CALLED` → `ENTERED` 또는 `SKIPPED` 또는 `CANCELLED`
- BR-WAIT-03: 호출 후 10분 내 미방문 시 `SKIPPED` 처리 (관리자 수동 또는 서버 스케줄링)
- BR-WAIT-04: 동일 전화번호로 동일 부스에 중복 대기 등록 불가
- BR-WAIT-05: 관리자가 대기열 중간에 손님을 삽입하거나 순서를 변경할 수 있음
- BR-WAIT-06: 대기 접수 OFF 시 신규 등록 API가 403 반환
- BR-WAIT-07: 부스 관리자는 자신의 담당 부스 대기열만 관리 가능
- BR-WAIT-08: 대기 상태 조회/본인 취소 시 전화번호 뒤 4자리로 본인 확인 (URL 추측 방지)
- BR-WAIT-09: 알림톡 발송 실패 시 관리자 화면에 실패 표시 + 재발송 가능
- BR-WAIT-10: 웹 등록 어뷰즈 방지를 위해 신규 등록 API에 IP 기반 Rate Limiting 적용 (구체적 한도는 운영 단계에서 결정)
- BR-WAIT-11: 본인 취소는 `WAITING` 또는 `CALLED` 상태에서만 허용, 이미 종결된 대기(`ENTERED`/`SKIPPED`/`CANCELLED`)는 취소 불가
- BR-WAIT-12: 전체 부스 합산 활성 대기 최대 3건 제한 (부스당 1건 + 전체 3건)
- BR-WAIT-13: 동일 전화번호로 기존 대기가 있으면 예약자명이 일치해야 추가 등록 가능
- BR-WAIT-14: 입장 확정(ENTERED) 시 해당 전화번호의 다른 부스 활성 대기를 일괄 취소 + 취소 부스명을 통합한 SMS 1건 발송
- BR-WAIT-15: 시간 초과 자동 스킵 시 취소 SMS 발송
- BR-WAIT-16: SMS는 호출/스킵/입장확정 취소 시에만 발송 (등록 확인은 UI로 대체, 비용 절감)

---

### 3.7 리뷰 (review)

#### FS-USR-01-REVIEW: 주막 리뷰 작성/조회

**API 목록**

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/booths/{booth-id}/reviews` | 리뷰 작성 | 불필요 |
| GET | `/booths/{booth-id}/reviews` | 리뷰 목록 조회 | 불필요 |
| DELETE | `/reviews/{review-id}` | 리뷰 본인 삭제 | 비밀번호 검증 |
| DELETE | `/admin/reviews/{review-id}` | 리뷰 삭제 (관리자) | 관리자 |

**비즈니스 규칙**
- BR-REVIEW-01: 리뷰 작성 시 닉네임 + 비밀번호(4자리) + 별점(1~5) + 텍스트 입력
- BR-REVIEW-02: 본인 삭제 시 작성 시 입력한 비밀번호로 검증
- BR-REVIEW-03: 최신순 정렬, 페이징 지원
- BR-REVIEW-04: 악성 리뷰 감지 로직 검토 (추후 MCP 연동)
- BR-REVIEW-05: IP 기반 작성 횟수 제한 (Rate Limiting)

---

### 3.8 롤링페이퍼 캔버스 (canvas)

#### FS-USR-03: 포스트잇 보드

> 방문자가 고정 문항 5개 중 하나를 선택하고, 해당 문항의 보드를 탐색하여 포스트잇을 붙이는 참여형 방명록 기능이다.  
> 인증 불필요. 문항당 보드 20개가 서버 시작 시 사전 생성되며(총 100개), 사용자는 원하는 보드를 골라 포스트잇을 작성한다.

**API 목록**

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/canvas/questions` | 문항 목록 전체 조회 | 불필요 |
| GET | `/canvas/boards?questionId={id}` | 문항별 보드 목록 조회 (포스트잇 수 포함) | 불필요 |
| GET | `/canvas/postits?boardId={id}` | 보드별 포스트잇 목록 조회 | 불필요 |
| POST | `/canvas/postits` | 포스트잇 생성 | 불필요 |
| POST | `/admin/canvas/boards` | 보드 추가 | 슈퍼 관리자 |
| DELETE | `/admin/canvas/postits/{postit-id}` | 포스트잇 삭제 | 슈퍼 관리자 |

**포스트잇 생성 요청**
```json
{
  "boardId": 1,
  "colorId": 3,
  "message": "축제 너무 좋아요!",
  "placement": {
    "x": 37.42,
    "y": 62.15
  }
}
```

**포스트잇 생성 응답**
```json
{
  "canvasPostitId": 51,
  "boardId": 1,
  "boardVariant": 3,
  "colorId": 3,
  "message": "축제 너무 좋아요!",
  "placement": {
    "x": 37.42,
    "y": 62.15
  },
  "createdAt": "2026-05-13T15:30:00"
}
```

**비즈니스 규칙**
- BR-RP-01: 포스트잇 생성 시 인증 불필요 (공개 API)
- BR-RP-02: 메시지 최대 60자, 초과 시 400 반환
- BR-RP-03: 문항 5개는 서버 시작 시 고정값으로 시드 생성 (어드민 수정 불가)
- BR-RP-04: `colorId`는 1~6 (1:red, 2:yellow, 3:green, 4:blue, 5:purple, 6:pink)
- BR-RP-05: 좌표 `x`, `y`는 보드 기준 0~100 상대좌표 (스티커 중심점). 보드 논리 크기 852×852px
- BR-RP-06: 서버는 보드 경계, 중앙 프레임 금지 영역(320×320 + 26px 패딩), 기존 포스트잇 충돌(AABB, collisionScale=0.4)을 검증하고 위반 시 저장 거부
- BR-RP-07: 보드당 최대 포스트잇 수(`maxNoteCount`)는 보드 생성 시 설정 (기본값 100)
- BR-RP-08: 부적절한 포스트잇 삭제는 SUPER_ADMIN만 가능 (소프트 딜리트)
- BR-RP-09: 포스트잇 생성 트랜잭션 커밋 후 Gemini AI가 비동기로 메시지 내용을 검열한다. 부적절 판정 시 소프트 딜리트 처리. API 호출 실패 또는 판단 불가 시 기본값은 통과(APPROVE)로 처리하여 과검열을 방지한다.

---

### 3.9 두근두근 인스타팅 (matching)

#### FS-USR-04: 랜덤 매칭

**운영 일정 (default)**
- 행사일: 2026-05-20, 2026-05-21, 2026-05-22 (KST)
- 매일 신청창: 11:00–21:00
- 매일 매칭 실행창: 21:00–22:00 (자동 스케줄러)
- 매일 결과창: 22:00 ~ 다음날 11:00
- 다음날 11:00 에 신청창 자동 재오픈, 어제 결과창 종료

설정 키 (`application.yml`):
- `matching.festival-days` (CSV LocalDate, 기본 `2026-05-20,2026-05-21,2026-05-22`)
- `matching.registration-open-hour` / `registration-close-hour` (기본 11/21)
- `matching.result-open-hour` / `result-close-hour` (기본 22/11)

**API 목록**

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/matchings` | 매칭 신청 | 불필요 |
| POST | `/matchings/result` | 매칭 결과 조회 (body로 ID+phone) | 불필요 |
| GET | `/matchings/status` | 매칭 서비스 상태 조회 | 불필요 |
| GET | `/matchings/applicants/count` | 현재 신청자 수 (성별 분리) | 불필요 |
| GET | `/matchings/unmatched` | 미매칭 공개 목록 조회 | 불필요 |
| POST | `/admin/matching-jobs` | 일괄 매칭 실행 (Time Drop) | 슈퍼 관리자 |
| PATCH | `/admin/matchings/status` | 매칭 상태 변경 (일시중단/재개) | 슈퍼 관리자 |

**매칭 신청 요청**
```
{
  "instagramId": "user_id",
  "gender": "MALE",
  "phoneNumber": "01012345678"
}
```

**매칭 결과 조회 요청**
```
{
  "instagramId": "user_id",
  "phoneNumber": "01012345678"
}
```

**매칭 결과 조회 응답 예시 (결과창 안)**
```
{
  "instagramId": "user_id",
  "status": "MATCHED",
  "resultOpen": true,
  "pickedInstagramId": "other_id",
  "instagramProfileUrl": "https://instagram.com/other_id",
  "messageKo": "당신이 뽑은 상대가 공개되었습니다.",
  "messageEn": "Your picked partner is open."
}
```

**applicants/count 응답 예시**
```
{
  "festivalDay": "2026-05-20",
  "malePendingCount": 12,
  "femalePendingCount": 9,
  "totalPendingCount": 21
}
```

**비즈니스 규칙**
- BR-MATCH-01: `(instagram_id, festival_day)` 복합 유니크 → 같은 날 1회만 참여, 다음 날 재참여 가능
- BR-MATCH-02: 전화번호는 `phone_lookup_hash`(HmacSHA256)와 `phone_encrypted`(AES-GCM) 두 컬럼으로 분리 저장. 결과 조회는 해시 비교
- BR-MATCH-03: 매칭 결과 인증은 POST body (instagramId + phoneNumber) — URL 노출 방지
- BR-MATCH-04: 결과 조회 brute-force 방지를 위해 IP 단위 Rate Limiting 적용 (5회 실패 → 10분 차단)
- BR-MATCH-05: 21:00–22:00 사이 60초 간격 자동 스케줄러가 그날 PENDING 만 매칭. 관리자 수동 트리거 가능
- BR-MATCH-06: 매칭 알고리즘 — 선착순 컷오프(`created_at` 빠른 N=min(남,여)명만 참가) + 교란 순열(남자 i 는 여자 σ(i) 를 뽑고, 여자 σ(i) 는 남자 π(i) 를 뽑되 π(i)≠i 보장). 컷오프 밖 인원은 `UNMATCHED` 상태로 전환되어 결과창에서 공개. N=1 인 경우 교란 순열이 불가능하므로 1:1 양방향 매칭으로 fallback
- BR-MATCH-07: `pickedInstagramId` 는 "내가 뽑은 상대" 의미. 양방향 짝 보장 없음(상대는 다른 사람 뽑음). 결과창 동안 본인 매칭 결과만 비공개로 조회
- BR-MATCH-08: 결과창 외 시각(11:00 이후, 21:00 직전)에는 결과·미매칭 목록이 모두 hidden 응답
- BR-MATCH-09: 매칭 실행은 PENDING 만 대상으로 멱등. 같은 트랜잭션 안에서 `PESSIMISTIC_WRITE` 락으로 직렬화
- BR-MATCH-10: 신청자 카운트는 그날 PENDING 기준 성별 분리. 결과창 동안에는 해당 결과 일자 기준 표시
- BR-MATCH-11: 매칭 신청 취소 기능은 제공하지 않는다 (UI 게이트로 "신청 후 취소 불가" 안내)
- BR-MATCH-12: 관리자 PATCH `/admin/matchings/status` 로만 PAUSED 진입 가능 (자동 PAUSED 룰 없음). PAUSED 동안 신청 차단

---

### 3.10 웹 포토부스 (photo)

#### FS-USR-05-PHOTO: 사진 업로드 및 에셋 관리

**API 목록**

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/photos` | 사진 업로드 | 불필요 |
| GET | `/photos/frames` | 프레임 목록 조회 | 불필요 |
| GET | `/photos/stickers` | 스티커 목록 조회 | 불필요 |
| POST | `/admin/photos/frames` | 프레임 등록 | 슈퍼 관리자 |
| DELETE | `/admin/photos/frames/{frame-id}` | 프레임 삭제 | 슈퍼 관리자 |
| POST | `/admin/photos/stickers` | 스티커 등록 | 슈퍼 관리자 |
| DELETE | `/admin/photos/stickers/{sticker-id}` | 스티커 삭제 | 슈퍼 관리자 |

**비즈니스 규칙**
- BR-PHOTO-01: 사진은 S3 업로드 후 URL 반환
- BR-PHOTO-02: 허용 확장자: jpg, png, webp / 최대 크기: 10MB
- BR-PHOTO-03: 업로드된 사진이 피드에 연결되지 않으면 일정 시간 후 정리 대상

---

### 3.11 호반우스타그램 피드 (feed)

#### FS-USR-05-FEED: 피드 관리

**API 목록**

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/feeds` | 피드 게시물 등록 | 불필요 |
| GET | `/feeds` | 피드 목록 조회 (페이징) | 불필요 |
| GET | `/feeds/{feed-id}` | 피드 상세 조회 | 불필요 |
| POST | `/feeds/{feed-id}/likes` | 피드 좋아요 | 불필요 |
| DELETE | `/feeds/{feed-id}/likes` | 피드 좋아요 취소 | 불필요 |
| GET | `/feeds/ranking` | 좋아요 랭킹 조회 | 불필요 |
| DELETE | `/admin/feeds/{feed-id}` | 피드 삭제 (관리자) | 관리자 |
| PUT | `/admin/feeds/contest-config` | 콘테스트 설정 (주기, 시작/종료일) | 슈퍼 관리자 |

**Query Parameters** (GET `/feeds`)
- `cursor`: 마지막 feed-id (커서 기반 페이징)
- `size`: 페이지 크기 (기본 20)
- `sort`: `latest` (기본) / `ranking`

**비즈니스 규칙**
- BR-FEED-01: 피드 등록 시 Instagram ID 선택 기입 가능
- BR-FEED-02: 좋아요 중복 방지 (세션 토큰 기반 서버 검증, 부스 좋아요와 동일 전략)
- BR-FEED-03: 콘테스트 기간 내 좋아요 1위 산출 (주기: 관리자 설정)
- BR-FEED-04: 피드 목록은 최신순 기본, 커서 기반 페이징
- BR-FEED-05: 피드 등록 시 IP 기반 Rate Limiting (스팸 방지)

---

### 3.12 관리자 인증 (auth — 부스별 비밀번호)

#### FS-ADM-03: 관리자 인증 (세션 기반)

> 별도 회원 체계 없이 **부스 = 계정** 모델을 사용한다. 부스 생성 시 설정한 비밀번호로 로그인하면 세션 쿠키가 발급된다.

**API 목록**

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/admin/auth/login` | 관리자 로그인 (boothId + password 또는 masterPassword) | 불필요 |
| POST | `/admin/auth/logout` | 로그아웃 (세션 무효화) | 관리자 |
| PATCH | `/admin/booths/{booth-id}/password` | 부스 비밀번호 변경 | 슈퍼 관리자 |

**비즈니스 규칙**
- BR-AUTH-01: 세션 기반 인증 — 로그인 시 서버가 JSESSIONID 쿠키 발급, 프론트는 `credentials: 'include'` 만 세팅
- BR-AUTH-02: `/admin/**` 엔드포인트는 유효한 세션 필수
- BR-AUTH-03: `/admin/**` 외 엔드포인트는 인증 불필요 (공개 API)
- BR-AUTH-04: 관리자 역할: `SUPER_ADMIN` (환경변수 마스터 비밀번호) / `BOOTH_ADMIN` (부스별 비밀번호)
- BR-AUTH-05: BOOTH_ADMIN은 자기 부스의 메뉴·대기열만 관리 가능 (소유권 검증)
- BR-AUTH-06: 로그인 실패 시 Rate Limiting 적용 (brute force 방지, 후속 과제)
- BR-AUTH-07: 로그아웃 시 세션 무효화

---

## 4. 공통 응답 형식

모든 API 응답은 다음 형식을 따른다:

**성공 응답**
```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

**에러 응답**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "C101",
    "message": "공지사항을 찾을 수 없습니다."
  }
}
```

---

## 5. 상태 코드 정책

| 상황 | HTTP Status |
|------|-------------|
| 조회 성공 | 200 OK |
| 생성 성공 | 201 Created |
| 삭제 성공 | 200 OK |
| 입력값 오류 | 400 Bad Request |
| 인증 실패 | 401 Unauthorized |
| 권한 부족 / 기능 비활성 | 403 Forbidden |
| 리소스 없음 | 404 Not Found |
| 중복 리소스 | 409 Conflict |
| 요청 과다 (Rate Limit) | 429 Too Many Requests |
| 서버 오류 | 500 Internal Server Error |

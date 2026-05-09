# 부스 랭킹 기능 정리

> 작성일: 2026-05-06  
> 대상: 부스 목록 정렬, 좋아요 랭킹, 대기열 적은 순 랭킹, SSE 실시간 반영

## 변경 이력

| 버전 | 날짜 | 변경 내용 | 변경자 |
|------|------|----------|--------|
| v1.0 | 2026-05-06 | 최초 작성 | tjddbs992 |
| v1.0.1 | 2026-05-10 | 변경 이력 표 추가 | lsmin3388 |

## 1. 기능 목적

부스 목록을 축제 이용자가 원하는 기준으로 빠르게 확인할 수 있도록 정렬한다. 좋아요 수와 현재 대기팀 수는 트래픽이 몰리는 시간대에도 빠르게 반영하기 위해 Redis를 우선 사용하고, 프론트엔드는 SSE를 통해 랭킹 변경 스냅샷을 실시간으로 받을 수 있다.

## 2. 제공 기능

### 부스 목록 정렬

`GET /booths?sort={sort}` API에서 정렬 기준을 선택할 수 있다.

| sort 값 | 설명 | 정렬 기준 |
|---|---|---|
| `likes` | 좋아요 순 | 좋아요 수 내림차순 → 대기팀 수 오름차순 → 이름 오름차순 |
| `waiting-asc` | 대기 적은 순 | 대기팀 수 오름차순 → 좋아요 수 내림차순 → 이름 오름차순 |
| `name-asc` | 이름순 | 이름 오름차순 |

`sort` 값이 없거나 잘못 들어오면 기본값은 `likes`다.

### 좋아요 / 좋아요 취소

| Method | Endpoint | 설명 |
|---|---|---|
| `POST` | `/booths/{booth-id}/likes` | 부스 좋아요 |
| `DELETE` | `/booths/{booth-id}/likes` | 부스 좋아요 취소 |

일반 사용자는 로그인하지 않는다. 대신 서버가 익명 쿠키를 발급하고, 해당 쿠키 값을 해싱한 식별자로 부스별 좋아요 여부를 Redis Set에 저장한다. 같은 브라우저에서는 같은 부스에 좋아요를 여러 번 눌러도 1회만 반영된다.

### SSE 랭킹 스트림

| Method | Endpoint | 설명 |
|---|---|---|
| `GET` | `/booths/rankings/stream?sort={sort}` | 랭킹 변경 스냅샷 구독 |

SSE 이벤트는 두 종류다.

| 이벤트명 | 설명 |
|---|---|
| `ranking` | 현재 정렬 기준의 랭킹 스냅샷 |
| `heartbeat` | 연결 유지를 위한 ping 이벤트 |

랭킹 스냅샷 응답 구조:

```json
{
  "sort": "likes",
  "items": [
    {
      "boothId": 1,
      "rank": 1,
      "likeCount": 12,
      "currentWaitingTeams": 3
    }
  ]
}
```

SSE 스냅샷은 전체 부스 상세 정보를 매번 보내지 않고, 랭킹 변경에 필요한 최소 정보만 보낸다. 프론트엔드는 최초에 `GET /booths`로 부스 이름, 이미지, 설명 등을 받아두고, SSE의 `boothId`를 기준으로 기존 부스 데이터와 매핑해서 화면 순서를 갱신한다.

## 3. Redis 사용 방식

Redis는 MySQL보다 카운트 증감과 조회가 빠르기 때문에 좋아요 수와 대기팀 수의 실시간성 확보에 사용한다. MySQL은 최종 영속 저장소이고, Redis는 트래픽이 몰릴 때 랭킹 계산에 사용할 빠른 카운터 역할을 한다.

| Redis Key | 자료구조 | 용도 |
|---|---|---|
| `booth:likes` | Sorted Set | 부스별 좋아요 수 저장 |
| `booth:waiting-count` | Sorted Set | 부스별 활성 대기팀 수 저장 |
| `booth:liked:{boothId}` | Set | 해당 부스에 좋아요를 누른 익명 사용자 해시 저장 |

좋아요 등록 시 흐름:

1. 익명 쿠키를 확인하거나 새로 발급한다.
2. `booth:liked:{boothId}` Set에 익명 사용자 해시를 추가한다.
3. 새로 추가된 사용자일 때만 `booth:likes` 점수를 1 증가시킨다.
4. 랭킹 변경 플래그를 세워 SSE 전송 대상이 되도록 한다.

좋아요 취소 시 흐름:

1. 익명 쿠키가 없으면 좋아요 취소는 카운트에 영향을 주지 않는다.
2. `booth:liked:{boothId}` Set에서 익명 사용자 해시를 제거한다.
3. 실제로 제거된 경우에만 `booth:likes` 점수를 1 감소시킨다.
4. 카운트가 음수가 되지 않도록 0 미만이면 0으로 보정한다.

## 4. SSE 전송 정책

좋아요나 대기열 변경이 발생할 때마다 즉시 모든 접속자에게 전송하지 않는다. 변경 발생 시 dirty flag만 표시하고, 스케줄러가 500ms마다 변경 여부를 확인해 최신 스냅샷을 전송한다.

| 항목 | 값 |
|---|---|
| 랭킹 전송 주기 | 500ms |
| heartbeat 주기 | 30초 |
| SSE timeout | 30분 |

이 방식은 짧은 시간에 좋아요 요청이 많이 몰려도 요청 수만큼 SSE를 발행하지 않고, 500ms 단위로 합쳐서 최신 상태만 전송하기 위한 설계다.

## 5. MySQL 동기화와 장애 대응

애플리케이션 시작 시 MySQL의 부스 좋아요 수와 활성 대기팀 수를 Redis에 초기 적재한다.

좋아요 수는 Redis에서 먼저 반영하고, 스케줄러가 10초마다 Redis의 좋아요 카운트를 MySQL에 동기화한다. 따라서 Redis는 실시간 랭킹 기준, MySQL은 영속 저장 기준으로 동작한다.

Redis를 사용할 수 없는 경우에는 가능한 범위에서 MySQL 값을 fallback으로 사용한다. 이 경우 실시간 성능은 떨어질 수 있지만 기본적인 부스 조회와 좋아요 처리는 유지된다.

## 6. 대기열과의 연결 지점

랭킹 기능은 대기열 등록 기능 자체를 소유하지 않는다. 다만 대기 상태가 변경될 때 활성 대기팀 수가 바뀌므로, 대기열 담당 로직에서 다음 상황에 Redis 대기팀 수를 갱신해야 한다.

- 대기 등록
- 대기 취소
- 호출 후 입장 완료
- 미방문 skip
- 관리자 취소

활성 대기 상태는 `WAITING`, `CALLED` 기준이다. 대기팀 수가 바뀌면 랭킹 dirty flag를 표시해서 SSE 구독자에게 최신 랭킹이 전달되도록 한다.

## 7. 관련 코드

| 파일 | 역할 |
|---|---|
| `BoothRankingSort` | 정렬 옵션 파싱 |
| `BoothRankingService` | 부스 목록 정렬과 랭킹 스냅샷 생성 |
| `BoothRankingStreamService` | SSE 구독 관리, 랭킹 브로드캐스트, heartbeat 전송 |
| `BoothRankingRedisRepository` | Redis 좋아요/대기팀 카운트, 좋아요 중복 방지 Set 관리 |
| `BoothRankingWarmup` | 애플리케이션 시작 시 Redis 초기화 |
| `BoothLikeSyncScheduler` | Redis 좋아요 수를 MySQL에 주기적으로 동기화 |
| `AnonymousIdCookieManager` | 로그인 없는 사용자를 위한 익명 쿠키 발급/해싱 |
| `BoothCommandService` | 좋아요/좋아요 취소 처리 |
| `BoothQueryService` | 부스 목록/상세 조회에서 Redis 기준 카운트 반영 |

## 8. 프론트엔드 연동 메모

프론트엔드는 최초 화면 진입 시 `GET /booths?sort=likes` 또는 원하는 정렬 기준으로 부스 목록을 조회한다. 이후 실시간 랭킹이 필요한 화면에서는 `/booths/rankings/stream?sort=likes` 또는 `/booths/rankings/stream?sort=waiting-asc`를 구독한다.

SSE 응답에는 부스 이름과 이미지가 포함되지 않으므로, 프론트엔드는 `boothId`를 key로 기존 부스 목록 데이터와 매핑해야 한다. 랭킹 스냅샷의 `items` 순서가 현재 화면에 표시할 순서다.

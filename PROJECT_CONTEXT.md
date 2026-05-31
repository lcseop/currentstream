# Current Stream — 프로젝트 맥락 정리

이 문서는 앱(`current_stream_app`)과 백엔드(`current-stream-backend`)를 함께 개발하면서 파악한 구조, API, 화면 흐름, 구현 상태를 정리한 것이다.

---

## 1. 저장소 구조

| 폴더 | 역할 |
|------|------|
| `current_stream_app/` | Android 앱 (Java, OkHttp, Firebase Auth) |
| `current-stream-backend/` | Spring Boot REST API, MySQL, JPA |

- 에뮬레이터 기준 API 베이스 URL: `http://10.0.2.2:8080`
- 실기기 테스트 시 PC IP로 `BASE_URL` 변경 필요

---

## 2. 인증 흐름

### Firebase + 서버 로그인

1. 앱: Firebase로 로그인 → `idToken` 발급
2. 앱: `POST /api/user/login` body `{ "idToken": "..." }`
3. 서버: Firebase 토큰 검증 후 `UsersDto` 반환 (`uid`, `name`, `email`, `tag` 등)
4. 앱: `SessionManager`에 저장

### SessionManager (싱글톤)

| 필드 | 용도 |
|------|------|
| `idToken` | Firebase JWT |
| `uid` | 백엔드 사용자 식별자 (API 헤더 `uid`) |
| `tag` | 표시용 태그 (예: `emailprefix#1234`) |
| `currentTeamId` | 앱 전역에서 선택 중인 팀 ID |

### Spring Security

- `/api/user/login`, `/api/user/signup` 외 API는 앱에서 **`uid` 헤더**로 사용자를 구분한다.
- 개발 중 `SecurityConfig`에서 `/api/**`를 `permitAll()`로 열어 두었음 (세션 로그인 없이 uid 헤더만 사용하는 구조).

---

## 3. 공통 API 응답 형식

```json
{
  "responseCode": "select_ok",
  "message": "teams list",
  "responseData": { ... }
}
```

- 성공 코드: `insert_ok`, `update_ok`, `delete_ok`, `select_ok` (앱에서는 `endsWith("_ok")`로 판별)
- 실패 시 HTTP 4xx/5xx 또는 `failed` 등

---

## 4. 주요 API 엔드포인트

### 사용자 (`/api/user`)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/user/login` | idToken → UsersDto |
| POST | `/api/user/signup` | 회원가입 |
| GET | `/api/user/tag?tag=` | tag 존재 확인 (팀 만들기 시 초대용) |

### 팀 (`/api/team`)

| 메서드 | 경로 | 헤더 | 설명 |
|--------|------|------|------|
| GET | `/api/team` | `uid` | 내 팀 목록 → `List<TeamsDto>` |
| POST | `/api/team` | `uid` | 팀 생성 body: `name`, `endDate` (yyyy-MM-dd) |
| GET | `/api/team/invite` | `uid` | 받은 초대 목록 → `List<InviteDto>` |
| POST | `/api/team/invite` | `uid` | 초대 body: `teamId`, `tag` |
| POST | `/api/team/invite/{inviteId}/accept` | `uid` | 초대 수락 |
| POST | `/api/team/invite/{inviteId}/reject` | `uid` | 초대 거절 |

### 목표 (`/api/goal`)

| 메서드 | 경로 | 헤더 | 설명 |
|--------|------|------|------|
| GET | `/api/goal/team/{teamId}` | `uid` | **해당 사용자**의 팀 내 목표 목록 |
| POST | `/api/goal` | `uid` | 목표 생성 |
| PATCH | `/api/goal/{goalId}/status` | `uid` | 상태 변경 |

### 팀 로그 (`/api/team/log`)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/team/log/{teamId}` | 최근 로그 10건 (uid 헤더 없음) |

---

## 5. 백엔드 DTO 요약

### TeamsDto

- `id`, `teamName`, `endDate` (LocalDate → JSON `yyyy-MM-dd`), `leaderId`

### InviteDto / InviteEntity

- `id`, `status` (0 요청 / 1 수락 / 2 거절), `userId`, `teamId`
- **추가 필드**: `teamName`, `inviterName` (초대 생성 시 `TeamsService.inviteUser`에서 저장)

### GoalDto

- `goalText`, `status` (0 진행중, 1 달성, 2 삭제), `remark`, `goalEndDate`, `userId`, `teamId`
- `getGoals(uid, teamId)`는 **로그인 사용자 본인** 목표만 반환

---

## 6. Android 화면·구현 상태

### 액티비티

| 클래스 | 상태 | 역할 |
|--------|------|------|
| `SplashActivity` | 구현됨 | 자동 로그인, login API → uid/tag 저장 → Main |
| `LoginActivity` | 구현됨 | 이메일/구글 로그인, login API, uid/tag 저장 |
| `RegisterActivity` | 구현됨 | 회원가입 + boolean[] 검증 패턴 |
| `MainActivity` | 구현됨 | 팀 없음/있음, 팀 선택, 목표·진행률, 초대 바텀시트 |
| `CreateTeamActivity` | 구현됨 | 팀 생성 + tag 초대 |
| `TeamsActivity` | 스켈레톤 | 레이아웃만 |
| `CreateTeamActivity` | 위와 동일 | |

### MainActivity (`activity_main.xml`)

**헤더**

- `main_team_selector`: 현재 팀명 (클릭 → 팀 목록 바텀시트)
- `main_notification`: 초대 바텀시트

**팀 있음 (`main_content_layout` + SwipeRefresh)**

- D-Day: `TeamsDto.endDate` 기준 `D-n` / `D+n`
- 진행률: 본인 목표 중 status=1 / 전체
- **내 작업**: `main_my_task_tag`에 SessionManager의 **tag** 표시
- `rv_progress` / `rv_complete`: `main_item_my_task.xml`
- 최근 현황: `main_team_list` (팀 로그 API 연동은 아직 미구현 가능성 있음 — 레이아웃만 존재)

**팀 없음 (`empty_layout`)**

- "팀이 없습니다" + `main_no_team_create` → `CreateTeamActivity`
- `main_no_team_logout`

**바텀시트 레이아웃**

- `main_bottom_sheet_teams.xml` + `main_bottom_sheet_team_item.xml`
- `main_bottom_sheet_invite.xml` + `main_bottom_sheet_invite_item.xml`
- 어댑터: `InviteAdapter`, MainActivity 내부 `TeamBottomSheetAdapter`, `GoalAdapter`

**로딩/에러**

- API 실패 시 `CommonDialog`
- 팀 목록 비어 있으면 `showEmptyTeamState()` (SwipeRefresh 숨김, empty_layout 표시)
- `onResume`에서 팀 목록 재조회 (팀 만들기 후 복귀용)

### CreateTeamActivity (`activity_create_team.xml`)

| 기능 | 규칙 |
|------|------|
| 팀 이름 | 2~100자, `boolean[] check[0]` |
| 목표 날짜 | DatePickerDialog, **오늘+7일 이후**만 선택, `check[1]` |
| 만들기 버튼 | `check[0] && check[1]`일 때만 활성화 |
| 팀원 초대 | tag 입력 + Enter → `GET /api/user/tag` 확인 후 리스트 추가 (최대 5명) |
| 만들기 | `POST /api/team` → 성공 시 등록된 tag마다 `POST /api/team/invite` 순차 호출 |

- 리스트 모델: `InviteMember(name, tag)`
- 어댑터: `CreateTeamMemberAdapter`

---

## 7. 네트워킹 (앱)

- **OkHttp** 직접 사용 (Retrofit/Gson 없음, `org.json` 파싱)
- 인증 헤더: `uid: {firebase uid}`
- 초대 수락/거절 POST: body `{}`, `application/json`

---

## 8. DB / 스키마 참고

- `team_invite`에 `team_name`, `inviter_name` 컬럼 추가됨 (`nullable=false`)
- JPA `ddl-auto: update` — 기존 invite 행이 있으면 마이그레이션 이슈 가능
- 사용자 `tag`는 회원가입/최초 로그인 시 `emailprefix#랜덤4자리` 형태로 자동 생성

---

## 9. 테스트 시나리오

1. **백엔드** `8080` 실행, MySQL 연결 확인
2. 앱 로그인 → Main
3. 팀 없음 → `empty_layout` 표시
4. 팀 만들기 → 이름·날짜·(선택) tag 초대 → Main에서 팀 화면
5. 헤더 팀명 탭 → 팀 전환
6. 알림 → 초대 없으면 **빈 바텀시트** (에러 아님), 있으면 수락/거절
7. 다른 계정 tag로 초대 후 수락 → 팀 참여

---

## 10. 아직 미완/확장 가능 영역

- `TeamsActivity` 상세 화면 (멤버별 목표 등)
- Main **최근 현황** (`main_team_list`) ↔ `GET /api/team/log/{teamId}` 연동
- 팀 전체 진행률 (현재는 **본인 목표** 기준 진행률)
- `RegisterActivity`와 동일하게 login 응답 파싱 공통 유틸 분리
- 실기기용 `BASE_URL` 설정 (BuildConfig 등)
- Spring Security를 uid 헤더 검증 필터로 정교화

---

## 11. 관련 파일 빠른 참조

### 앱

```
current_stream_app/app/src/main/java/lee/mjc/current_stream_app/
  MainActivity.java
  CreateTeamActivity.java
  SessionManager.java
  InviteAdapter.java
  CreateTeamMemberAdapter.java
  InviteMember.java
  LoginActivity.java
  SplashActivity.java
```

### 백엔드

```
current-stream-backend/.../models/
  teams/TeamsRestController.java, TeamsService.java, TeamsDto.java
  invite/InviteDto.java, InviteEntity.java
  goal/GoalRestController.java, GoalService.java, GoalDto.java
  users/UsersRestController.java, UsersService.java
  config/SecurityConfig.java
```

---

*마지막 업데이트: 대화 기준으로 Main/CreateTeam/초대/팀 없음 화면·tag 표시까지 반영된 상태.*

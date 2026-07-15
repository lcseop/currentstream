# Current Stream — 프로젝트 맥락 정리

앱(`current_stream_app`)과 백엔드(`current-stream-backend`)의 구조, API, 화면 흐름, **전체 클래스 목록**을 정리한 문서입니다.

> **최신화 기준:** 2026-05-27 — TeamsActivity·팀 로그·SessionHelper·예외처리 보강 등 반영

---

## 1. 저장소 구조

| 폴더 | 역할 |
|------|------|
| `current_stream_app/` | Android 앱 (Java, OkHttp, Firebase Auth) |
| `current-stream-backend/` | Spring Boot REST API, MySQL, JPA |

| 환경 | API 베이스 URL |
|------|----------------|
| 에뮬레이터 | `http://10.0.2.2:8080` |
| EC2 배포 (현재 build.gradle) | `http://3.36.62.169:8080` (`BuildConfig.API_BASE_URL` → `ApiConfig.BASE_URL`) |

---

## 2. 전체 클래스 · 기능 표

구현된 **모든 클래스**와 **하는 일**을 한 줄로 정리한 표입니다.  
패키지: Android `lee.mjc.current_stream_app` / 백엔드 `com.currentstreambackend.currentstreambackend`

### 2-1. Android (35개)

| 클래스 | 분류 | 기능 (간략) |
|--------|------|-------------|
| `CurrentStreamApplication` | Application | 앱 시작 시 전역 설정 (라이트 모드 고정) |
| `SplashActivity` | Activity | Firebase 로그인 여부 확인 후 자동 login API → Main 또는 Login |
| `LoginActivity` | Activity | 이메일·구글 로그인, Firebase 토큰을 서버에 보내 세션 확보 |
| `RegisterActivity` | Activity | Firebase 회원가입 + signup API, 입력값 검증 |
| `MainActivity` | Activity | 메인 화면: 팀 선택, 내 목표, 진행률, 최근 활동, 초대, FAB |
| `CreateTeamActivity` | Activity | 팀 이름·마감일 입력, tag로 멤버 추가 후 팀 생성·초대 API |
| `TeamsActivity` | Activity | 팀 상세: 멤버별 목표 목록, 팀 수정·삭제·탈퇴, FAB |
| `CommonDialog` | Dialog | 확인 버튼 하나짜리 알림창 |
| `ConfirmCancelDialog` | Dialog | 확인/취소 선택 다이얼로그 |
| `AddGoalDialog` | Dialog | 목표 텍스트·비고·마감일 입력 후 목표 생성 API 호출 |
| `GoalDetailDialog` | Dialog | 목표 상세 보기, 완료 처리(상태 변경 API) |
| `InviteMemberDialog` | Dialog | tag로 팀원 검색 후 초대 API 호출 |
| `TeamPickerBottomSheet` | UI | 가입한 팀 목록 바텀시트, 팀 전환 |
| `FabSpeedDialMenu` | UI | FAB 누르면 펼쳐지는 메뉴 (팀 만들기·초대·목표 추가) |
| `MyGoalAdapter` | Adapter | Main 화면 진행 중/완료 목표 RecyclerView |
| `TeamLogAdapter` | Adapter | 최근 활동 로그 목록 + "N분 전" 표시 |
| `TeamMemberAdapter` | Adapter | Teams 화면 멤버·목표 트리, 접기/펼치기 |
| `InviteAdapter` | Adapter | 받은 초대 목록, 수락/거절 버튼 |
| `CreateTeamMemberAdapter` | Adapter | 팀 만들기 화면의 초대 예정 멤버 리스트 |
| `GoalMemberPickerAdapter` | Adapter | 목표 추가 시 담당 멤버 선택 리스트 |
| `GoalItem` | Model | API 목표 JSON을 앱 필드로 담는 그릇 |
| `TeamItem` | Model | API 팀 JSON을 앱 필드로 담는 그릇 |
| `InviteItem` | Model | API 초대 JSON을 앱 필드로 담는 그릇 |
| `TeamLogItem` | Model | 팀 활동 로그 한 줄 (메시지·시각) |
| `TeamMemberItem` | Model | 멤버 정보 + 해당 멤버 목표 목록 |
| `TeamGoalItem` | Model | 팀 전체 목표 한 건 (Teams 화면용) |
| `InviteMember` | Model | 팀 생성 전, 아직 API 안 보낸 초대 대상 임시 저장 |
| `ApiConfig` | Util | 서버 주소 (`BuildConfig.API_BASE_URL`) |
| `ApiHelper` | Util | OkHttp 공통, 응답 성공 판별, uid 헤더, UI 스레드 안전 처리 |
| `SessionManager` | Util | 로그인 정보·현재 팀 ID 메모리 보관 (싱글톤) |
| `SessionHelper` | Util | login API 응답 검증 후 SessionManager에 저장 |
| `DateTimeUtil` | Util | 날짜 파싱, "3분 전", D-day 계산 |
| `ColorUtil` | Util | 멤버 색상 HEX → Android Color 변환 |
| `DialogUiHelper` | Util | 다이얼로그·태그 배지 공통 UI 처리 |
| `TeamUiConstants` | Util | 팀 이름 글자 수 제한 등 UI 상수 |

> `MainActivity` 내부 `TeamBottomSheetAdapter`(private)는 팀 선택 바텀시트 전용 어댑터입니다.

### 2-2. 백엔드 (36개)

| 클래스 | 분류 | 기능 (간략) |
|--------|------|-------------|
| `CurrentStreamBackendApplication` | 진입점 | Spring Boot 앱 실행 |
| `FirebaseConfig` | 설정 | Firebase Admin SDK 초기화 (토큰 검증용) |
| `SecurityConfig` | 설정 | HTTP 보안: CSRF 끔, API 전 경로 허용 |
| `ApiResponse` | common | 모든 API 응답 공통 형식 (코드·메시지·데이터) |
| `ResponseCode` | common | insert_ok / select_ok 등 결과 코드 enum |
| `GlobalExceptionHandler` | common | 예외를 HTTP 상태코드 + 한글 메시지로 변환 |
| `EmailNotVerifiedException` | common | 이메일 미인증 시 던지는 예외 |
| `TokenRequest` | common | login 요청 body (idToken 필드) |
| `UsersRestController` | users | 회원가입·로그인·tag 검색·탈퇴 HTTP API |
| `UsersService` | users | Firebase 토큰 검증, 사용자 생성·조회·탈퇴 처리 |
| `UsersEntity` | users | users DB 테이블 매핑 |
| `UsersDto` | users | 사용자 정보 API 응답 객체 |
| `UsersRepository` | users | uid·tag로 사용자 DB 조회 |
| `UsersInterface` | users | 사용자 엔티티 공통 필드 정의 |
| `TeamsRestController` | teams | 팀·초대·멤버·탈퇴 HTTP API |
| `TeamsService` | teams | 팀 생성/수정/삭제, 초대·수락, 멤버 조회, 색상 배정 |
| `TeamsEntity` | teams | teams DB 테이블 매핑 |
| `TeamsDto` | teams | 팀 정보 API 응답 객체 |
| `TeamsRepository` | teams | 팀 DB CRUD |
| `TeamMemberDto` | teams | 팀 멤버 목록 API 응답 (이름·tag·색·팀장 여부) |
| `InviteEntity` | invite | team_invite DB 테이블 (초대 상태·스냅샷) |
| `InviteDto` | invite | 초대 정보 API 응답 객체 |
| `InviteRepository` | invite | 초대 DB 조회·삭제 |
| `MappingEntity` | mapping | user–team 소속 관계 + 멤버 색상 DB |
| `MappingRepository` | mapping | 팀원 여부 확인, 매핑 삭제 |
| `MappingDto` | mapping | 매핑 보조 DTO |
| `GoalRestController` | goal | 목표 생성·조회·상태변경·삭제 HTTP API |
| `GoalService` | goal | 목표 비즈니스 로직, 권한·입력 검증, 팀 로그 기록 |
| `GoalEntity` | goal | goals DB 테이블 (진행/완료/삭제 상태) |
| `GoalDto` | goal | 목표 정보 API 응답 객체 |
| `GoalRepository` | goal | 팀·유저별 목표 DB 조회·삭제 |
| `TeamLogsRestController` | teamlogs | 팀 최근 활동 로그 조회 API |
| `TeamLogsService` | teamlogs | 로그 저장·조회 (팀원만, 한국 시각) |
| `TeamLogsEntity` | teamlogs | team_logs DB 테이블 |
| `TeamLogsDto` | teamlogs | 로그 API 응답 (메시지·시각·millis) |
| `TeamLogsRepository` | teamlogs | 팀별 최근 10건 조회, 팀 삭제 시 로그 삭제 |

---

## 3. 인증 흐름

1. 앱: Firebase 로그인 → `idToken` 발급
2. 앱: `POST /api/user/login` body `{ "idToken": "..." }`
3. 서버: Firebase Admin SDK로 토큰 검증 → `UsersDto` 반환 (`id`, `uid`, `name`, `email`, `tag`)
4. 앱: `SessionHelper.applyLoginResponse`로 `responseCode`가 `*_ok`이고 `uid`가 있을 때만 `SessionManager`에 저장

### SessionManager (싱글톤)

| 필드 | 용도 |
|------|------|
| `idToken` | Firebase JWT |
| `uid` | Firebase uid — **대부분 API의 `uid` 헤더** |
| `tag` | 표시용 태그 (예: `emailprefix#1234`) |
| `userName` | 닉네임 |
| `userId` | DB `users.id` (팀장 여부·목표 소유자 비교) |
| `currentTeamId` | 앱 전역 선택 팀 ID |

### Spring Security

- `/api/**`는 `permitAll()` — **세션 쿠키 없음**, 앱이 `uid` 헤더로 사용자 식별
- 실제 권한(팀장/멤버)은 각 **Service**에서 검증
- [한계] uid 헤더 위조 가능 → 장기적으로 Firebase 토큰 검증 필터 필요

---

## 4. 공통 API 응답 형식

```json
{
  "responseCode": "select_ok",
  "message": "teams list",
  "responseData": { }
}
```

- 성공: `insert_ok`, `update_ok`, `delete_ok`, `select_ok` — 앱은 `ApiHelper.isSuccess()` (`endsWith("_ok")`)
- 실패: HTTP 4xx/5xx + `GlobalExceptionHandler`가 한글 `message` 반환 (내부 스택 노출 없음)

---

## 5. API 엔드포인트 (전체)

### 사용자 `/api/user`

| 메서드 | 경로 | 헤더 | 설명 |
|--------|------|------|------|
| POST | `/signup` | — | body: `idToken`, `name` |
| POST | `/login` | — | body: `idToken` → UsersDto |
| GET | `/tag?tag=` | — | tag로 사용자 검색 (초대용) |
| DELETE | `/` | `uid` | 회원 탈퇴 (연관 팀·목표·초대 정리) |

### 팀 `/api/team`

| 메서드 | 경로 | 헤더 | 설명 |
|--------|------|------|------|
| GET | `/` | `uid` | 내 팀 목록 |
| POST | `/` | `uid` | 팀 생성 body: `name`, `endDate` |
| PATCH | `/{teamId}` | `uid` | 팀 정보 수정 (팀장) |
| DELETE | `/{teamId}` | `uid` | 팀 삭제 (팀장, 연관 데이터 일괄 삭제) |
| DELETE | `/{teamId}/leave` | `uid` | 팀 탈퇴 (리더 불가, 본인 목표 삭제) |
| GET | `/{teamId}/members` | `uid` | 팀 멤버 목록 |
| GET | `/invite` | `uid` | 받은 초대 (status=0) |
| POST | `/invite` | `uid` | 초대 body: `teamId`, `tag` |
| POST | `/invite/{id}/accept` | `uid` | 초대 수락 |
| POST | `/invite/{id}/reject` | `uid` | 초대 거절 |

### 목표 `/api/goal`

| 메서드 | 경로 | 헤더 | 설명 |
|--------|------|------|------|
| GET | `/team/{teamId}` | `uid` | **본인** 팀 내 목표 |
| GET | `/team/{teamId}/all` | `uid` | 팀 전체 목표 (status≠2, 팀원) |
| POST | `/` | `uid` | 목표 생성 (선택 `targetUserId` — 팀장이 타인 할당) |
| PATCH | `/{goalId}/status` | `uid` | 상태 0/1/2 변경 |
| DELETE | `/{goalId}` | `uid` | 목표 삭제 |

### 팀 로그 `/api/team/log`

| 메서드 | 경로 | 헤더 | 설명 |
|--------|------|------|------|
| GET | `/{teamId}` | **`uid`** | 최근 로그 10건 (**팀원만**) |

---

## 6. 화면 흐름

```
SplashActivity → (자동 로그인) → MainActivity
              → (실패)       → LoginActivity → RegisterActivity

MainActivity
  ├─ 팀 없음 → empty_layout → CreateTeamActivity
  ├─ 팀 pill → TeamPickerBottomSheet
  ├─ 진행률 카드 → TeamsActivity
  ├─ 종 아이콘 → InviteAdapter 바텀시트
  ├─ 최근 활동 → TeamLogAdapter (GET /api/team/log)
  └─ FAB → 팀 만들기 / 초대 / 목표 추가

TeamsActivity
  ├─ 멤버별 목표 (진행/완료 접기)
  ├─ 팀 pill → TeamPickerBottomSheet
  ├─ 설정(팀장) → 팀 수정·삭제·나가기
  └─ FAB → 초대 / 목표 추가
```

---

## 7. DB / 스키마 참고

| 테이블 | 비고 |
|--------|------|
| `users` | uid, name, email, tag |
| `teams` | teamName, endDate, leaderId |
| `mapping` | userId, teamId, userColor |
| `team_invite` | status, teamName·inviterName 스냅샷 |
| `goals` | goalText, remark, status, goalEndDate, userId, teamId |
| `team_logs` | message, createdAt (서버 Asia/Seoul 기준 저장) |

- JPA `ddl-auto: update`
- JDBC `serverTimezone=Asia/Seoul`, Jackson `time-zone: Asia/Seoul`

---

## 8. 네트워킹 (앱)

- **OkHttp** + `org.json` (Retrofit/Gson 없음)
- 인증: 요청 헤더 `uid: {Firebase uid}`
- 비동기: `enqueue` + `runOnUiThread` / `ApiHelper.runOnUiThreadSafe`
- 성공 판별: HTTP 2xx **및** `ApiHelper.isSuccess(responseBody)`

---

## 9. 구현 완료 vs 미완

### 완료

- Main 최근 활동 ↔ 팀 로그 API (`uid` 헤더)
- TeamsActivity 멤버·목표 통합 UI
- 초대 수락/거절, 알림 배지
- SessionHelper, GlobalExceptionHandler, 팀 탈퇴/삭제 시 연관 데이터 정리
- 회원 탈퇴 (Main)
- 팀 이름 앱 검증 2~20자 (`TeamUiConstants`)

### 미완 / 확장 가능

- FCM 푸시 알림 (초대 시)
- Firebase ID 토큰 기반 API 인증 필터 (uid 위조 방지)
- Controller `@Valid` 입력 검증 일괄 적용
- 팀 전체 진행률 (현재 Main 진행률은 본인 목표 기준)

---

## 10. 관련 문서

| 파일 | 내용 |
|------|------|
| `ANDROID_READING_GUIDE.md` | Android 읽기 순서·MainActivity 치트시트·주석 규칙 |
| `DEPLOY.md` | EC2 배포 (있을 경우) |

---

*마지막 업데이트: 2026-05-27 — §2 전체 클래스·기능 표 (Android 35 + 백엔드 36)*

# Current Stream Android — 코드 읽기 가이드

앱 Java 파일 41개를 **한 번에 외우지 말고**, 화면 흐름 → 기능 하나씩 따라가면 됩니다.  
Spring Boot처럼 “레이어” 대신 **Activity(화면) + Dialog + Adapter**로 나눠서 읽으세요.

---

## 1. 화면 흐름 (여기서 시작)

```
SplashActivity
    ├─ 로그인 O → MainActivity
    └─ 로그인 X → LoginActivity → RegisterActivity
                                    ↓
MainActivity ←─────────────────── (가입 후)
    ├─ 팀 없음 → empty_layout / CreateTeamActivity
    ├─ 팀 선택 pill → TeamPickerBottomSheet
    ├─ 진행률 카드 → TeamsActivity
    ├─ 종 아이콘 → InviteAdapter (바텀시트)
    └─ FAB → 팀 만들기 / 팀원 초대 / 목표 추가
TeamsActivity
    ├─ 제목 pill → TeamPickerBottomSheet
    ├─ 설정(팀장) → showEditTeamDialog
    └─ FAB → 초대 / 목표 추가
CreateTeamActivity → 팀 생성 + 초대 tag 검색
```

**AndroidManifest.xml** 에 등록된 Activity가 전부입니다 (6개).

| 순서 | 읽을 파일 | 목적 |
|------|-----------|------|
| 1 | `SessionManager.java` | uid, tag, currentTeamId가 뭔지 |
| 2 | `ApiConfig.java` + `ApiHelper.java` | 서버 주소, HTTP, `select_ok` |
| 3 | `SplashActivity.java` | 앱 켤 때 자동 로그인 |
| 4 | `LoginActivity.java` | Firebase → 서버 login (이미 익숙) |
| 5 | `MainActivity.onCreate()` | 메인 화면 뼈대 |
| 6 | `TeamsActivity.onCreate()` | 팀 상세 뼈대 |

Model(`GoalItem`, `TeamItem` …)은 **JSON 그릇**이라 필드만 훑고 넘어가도 됩니다.

---

## 2. 클래스 41개 — 역할만 구분

| 묶음 | 파일 | 언제 읽나 |
|------|------|-----------|
| **화면** | `Splash`, `Login`, `Register`, `Main`, `Teams`, `CreateTeam` Activity | 항상 먼저 |
| **공통** | `ApiConfig`, `ApiHelper`, `SessionManager`, `CommonDialog`, `DateTimeUtil`, `ColorUtil` | Day 1, 10분 |
| **데이터** | `GoalItem`, `InviteItem`, `TeamItem`, `TeamGoalItem`, `TeamMemberItem`, `TeamLogItem`, `InviteMember` | API 응답 볼 때 참고 |
| **목록** | `MyGoalAdapter`, `TeamMemberAdapter`, `TeamLogAdapter`, `InviteAdapter`, `CreateTeamMemberAdapter` | 해당 리스트 UI 볼 때 |
| **팝업** | `AddGoalDialog`, `GoalDetailDialog`, `InviteMemberDialog`, `TeamPickerBottomSheet`, `ConfirmCancelDialog` | 버튼 눌렀을 때 Find Usages |
| **기타** | `FabSpeedDialMenu` | FAB 펼침 메뉴 |

---

## 3. MainActivity — 기능별 읽기 순서 (치트시트)

`MainActivity.java`가 길어서 **Structure 창(왼쪽)** 또는 아래 표로 목차처럼 보세요.

### 3-1. 앱 켜질 때 (필수)

| 순서 | 메서드 | 같이 볼 파일 |
|------|--------|--------------|
| 1 | `onCreate()` | `activity_main.xml` |
| 2 | `ensureUidReady()` | `ApiHelper`, `SessionManager` |
| 3 | `loadTeamsAndGoals()` | |
| 4 | `showEmptyTeamState()` / `showTeamContentState()` | empty vs main 레이아웃 전환 |
| 5 | `loadGoalsForTeam(teamId)` | `MyGoalAdapter`, `GoalItem` |
| 6 | `loadTeamLogs(teamId)` | `TeamLogAdapter`, `TeamLogItem` |

**디버거 추천:** `loadTeamsAndGoals()` 에 breakpoint → 팀 목록 API → 현재 팀 목표/로그까지 한 번에 추적.

### 3-2. 화면 상태·새로고침

| 메서드 | 하는 일 |
|--------|---------|
| `markRefreshStarted()` / `beginRefreshSections()` / `finishRefreshSection()` | 당겨서 새로고침: API 여러 개 끝날 때까지 스피너 유지 |
| `cancelRefresh()` | 에러 시 새로고침 중단 |
| `resetGoalsUi()` | 팀 없음/팀 변경 시 목표·로그 UI 초기화 |
| `onResume()` / `onPause()` | 돌아올 때 재로딩, 로그 “N분 전” 1분 타이머 |

### 3-3. 사용자가 누르는 것 (기능별)

| UI | 클릭 → 메서드 | 이어지는 클래스 |
|----|----------------|-----------------|
| 팀 이름 pill | `openTeamsBottomSheet()` | `TeamPickerBottomSheet` |
| 진행률 카드 | `TeamsActivity` 시작 | `TeamsActivity` |
| 종(알림) | `openInviteBottomSheet()` → `showInviteBottomSheetInternal()` | `InviteAdapter`, `handleInviteAction()` |
| FAB 팀 만들기 | `CreateTeamActivity` | |
| FAB 팀원 초대 | `openInviteMemberDialog()` | `InviteMemberDialog` |
| FAB 목표 추가 | `openAddGoalDialog()` | `AddGoalDialog` |
| 내 작업 행 | `onMyGoalClick()` | `GoalDetailDialog` |
| 진행/완료 헤더 | `toggleGoalSection()` | `applyCompleteSectionVisibility()` |
| 최근 현황 더보기 | `refreshTeamLogsDisplay()` | |
| 로그아웃 | `logoutBtnClick()` | |
| 회원 탈퇴 | `confirmDeleteAccount()` → `deleteAccount()` | `ConfirmCancelDialog` |

### 3-4. 데이터 파싱만 (나중에 봐도 됨)

| 메서드 | 내용 |
|--------|------|
| `parseInvites()` | 초대 JSON → `InviteItem` |
| `parseGoals()` | 목표 JSON → `GoalItem` |
| `parseTeamLogs()` | 로그 JSON → `TeamLogItem` |
| `findTeamById()` | teamList에서 현재 팀 찾기 |
| `applyTeamHeader()` | 상단 팀 이름, D-day |
| `getGoalAddEligibleMembers()` | 목표 추가 가능 멤버 필터 |
| `isCurrentTeamLeader()` | FAB 초대 버튼용 |

### 3-5. MainActivity 읽기 30분 코스

1. `onCreate` (105~215줄) — 뭐가 연결되는지만  
2. `loadTeamsAndGoals` (817줄~) — 팀 목록 API  
3. `loadGoalsForTeam` (943줄~) — 내 작업 + 진행률  
4. `openTeamsBottomSheet` (1092줄~) — 팀 바꾸기  
5. `openAddGoalDialog` (1109줄~) — 목표 추가 한 줄기  

나머지는 **기능 수정할 때** 해당 행만 Find Usages.

---

## 4. TeamsActivity — 짧은 치트시트

| 기능 | 메서드 | 연결 |
|------|--------|------|
| 진입 시 로드 | `loadTeamData()` → `loadMembersAndGoals()` | |
| 멤버 카드 | `mergeMembersAndGoals()` | `TeamMemberAdapter` |
| 목표 클릭 | `onGoalClick()` | `GoalDetailDialog` |
| 목표 삭제 | `onDeleteGoal()` → `deleteGoal()` | `ConfirmCancelDialog` |
| 목표 추가 | `showAddGoalDialog()` | `AddGoalDialog` |
| 팀 설정 | `showEditTeamDialog()` → `updateTeam()` | |
| 팀 바꾸기 | `openTeamPicker()` | `TeamPickerBottomSheet` |
| 하단 버튼 | `onBottomButtonClick()` | 팀 삭제 / 나가기 |

---

## 5. Android Studio 팁

| 하고 싶은 것 | 방법 |
|--------------|------|
| 메서드 목록만 보기 | 파일 열고 **Structure** (Alt+7) |
| 이 함수 어디서 쓰나 | 함수명에 커서 → **Find Usages** (Alt+F7) |
| 버튼 → 코드 | `activity_main.xml`에서 `@+id/...` 복사 → Activity에서 검색 |
| 한 기능만 추적 | 에뮬레이터에서 동작 → 해당 `setOnClickListener` 에 breakpoint |
| 레이아웃 ↔ 코드 | XML에서 `@+id/main_team_selector` → MainActivity에서 `R.id.main_team_selector` |

---

## 6. Spring Boot랑 대응

| Spring | 이 앱 |
|--------|--------|
| `@RestController` | Activity / Dialog 의 API 호출 부분 |
| `Service` | Activity private 메서드 + `ApiHelper` |
| DTO | `GoalItem`, `TeamItem`, … |
| `RestTemplate` | `ApiHelper.CLIENT` |
| 세션 | `SessionManager` |
| HTML | `res/layout/*.xml` + Adapter |

---

## 7. 일주일 읽기 플랜 (선택)

| Day | 내용 |
|-----|------|
| 1 | `SessionManager`, `LoginActivity`, `ApiHelper` |
| 2 | `MainActivity` onCreate + `loadTeamsAndGoals` |
| 3 | `TeamsActivity` + `TeamMemberAdapter` |
| 4 | `AddGoalDialog` 한 줄기 (추가 → API → 갱신) |
| 5 | 초대 (`InviteMemberDialog`, `InviteAdapter`) |
| 6 | `CreateTeamActivity` |
| 7 | 나머지 Util / Dialog 필요할 때만 |

---

## 8. API 주소 바꾸는 곳

`app/build.gradle` → `API_BASE_URL` 한 줄  
앱 전체는 `ApiConfig.BASE_URL` 로 참조합니다.

---

더 자세한 API·백엔드 구조는 프로젝트 루트 `PROJECT_CONTEXT.md` 를 보세요.

---

## 9. 코드 주석 읽는 법

프로젝트 Java 파일에는 아래 규칙으로 주석이 달려 있습니다.

| 표기 | 의미 |
|------|------|
| `/** ... */` (클래스·메서드) | **무엇을 하는지** + **어떤 원리/흐름으로 동작하는지** |
| `// [중요] ...` | 꼭 알아야 할 비즈니스·보안·버그 방지 로직 (uid 헤더, 세션 검증, 권한 등) |
| `// --- 섹션명 ---` | 긴 Activity 안에서 lifecycle / auth / API / UI 블록 구분 |

**예시 — 최근 활동 로그가 안 보였던 이유**  
`MainActivity.loadTeamLogs` 주석: `GET /api/team/log/{teamId}` 는 **`uid` 헤더 필수** (`ApiHelper.uidRequest` 사용).

백엔드 Service 주석은 **권한·상태값·트랜잭션** 규칙을, Android 주석은 **OkHttp 콜백 → UI 갱신** 흐름을 설명합니다.

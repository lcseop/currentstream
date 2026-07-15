package lee.mjc.current_stream_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 로그인 후 보이는 메인 화면임.
 * 팀 고르고, 내 목표·진행률·최근 활동·초대 처리하는 허브 역할함.
 */
public class MainActivity extends AppCompatActivity {

    // 접혀있을 때 최근 활동 갯수
    private static final int TEAM_LOGS_COLLAPSED_COUNT = 3;
    // 더보기를 열었을 때 최근 활동 최대 갯수
    private static final int TEAM_LOGS_MAX_COUNT = 10;

    Button logoutBtn, noTeamLogoutBtn, noTeamCreateBtn;
    TextView deleteAccountBtn, noTeamDeleteAccountBtn;
    ImageButton notificationBtn;
    TextView teamSelectorTv;
    TextView notificationBadge;

    private SwipeRefreshLayout swipeRefreshLayout;
    private View mainContentLayout;
    private View emptyLayout;

    private TextView tvDeadlineDday;
    private TextView tvTotalPercent;
    private ProgressBar pbAll;

    private TextView tvMyTaskCount;
    private TextView tvUserNickname;
    private TextView tvMyTaskTag;
    private TextView tvNoTeamTag;
    private TextView tvProgressToggle;
    private TextView tvCompleteToggle;
    private TextView tvProgressEmpty;
    private TextView tvCompleteEmpty;
    private RecyclerView rvProgress;
    private RecyclerView rvComplete;
    private RecyclerView rvTeamLogs;
    private TextView tvTeamLogsEmpty;
    private TextView btnTeamLogsMore;
    private FloatingActionButton mainFab;

    // 모든 팀 최근 활동 리스트
    private final List<TeamLogItem> allTeamLogs = new ArrayList<>();
    // 최근 활동이 펼쳐져있는지 여부
    private boolean teamLogsExpanded = false;

    // 팀 리스트
    private final List<TeamItem> teamList = new ArrayList<>();
    // 진행 중인 목표 리스트
    private final List<GoalItem> progressGoals = new ArrayList<>();
    // 완료 목표 리스트
    private final List<GoalItem> completeGoals = new ArrayList<>();
    // 최근 활동 리스트
    private final List<TeamLogItem> teamLogs = new ArrayList<>();

    private MyGoalAdapter progressAdapter;
    private MyGoalAdapter completeAdapter;
    private TeamLogAdapter teamLogAdapter;
    private FabSpeedDialMenu mainFabMenu;

    // 당겨서 새로고침, 팀/목표 로딩 상태
    private int pendingRefreshSections = 0;
    private boolean refreshInProgress = false;
    private boolean isLeader = false;
    private boolean completeSectionExpanded = false;
    private Long lastGoalsTeamId = null;

    private final Handler logTimeHandler = new Handler(Looper.getMainLooper());

    /**
     * 최근 활동 로그 상대 시간("N분 전")을 60초마다 갱신하는 Runnable임.
     * onResume에서 postDelayed, onPause에서 removeCallbacks 함.
     */
    private final Runnable logTimeRefreshRunnable = new Runnable() {
        /** teamLogAdapter.notifyDataSetChanged로 상대 시간 텍스트 재렌더링함. */
        @Override
        public void run() {
            if (!teamLogs.isEmpty() && teamLogAdapter != null) {
                teamLogAdapter.notifyDataSetChanged();
            }
            logTimeHandler.postDelayed(this, 60_000L);
        }
    };

    // --- lifecycle ---

    /**
     * 액티비티 최초 진입 시 뷰·어댑터·리스너 바인딩하고 데이터 로드 시작함.
     * showLoadingState로 더미 UI 숨긴 뒤 applyUserTag → ensureUidReady → loadTeamsAndGoals 순임.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 상단·로그아웃·알림·팀 선택 뷰 바인딩
        logoutBtn = findViewById(R.id.main_team_logout);
        deleteAccountBtn = findViewById(R.id.main_team_delete_account);
        noTeamLogoutBtn = findViewById(R.id.main_no_team_logout);
        noTeamDeleteAccountBtn = findViewById(R.id.main_no_team_delete_account);
        noTeamCreateBtn = findViewById(R.id.main_no_team_create);
        notificationBtn = findViewById(R.id.main_notification);
        notificationBadge = findViewById(R.id.main_notification_badge);
        teamSelectorTv = findViewById(R.id.main_team_selector);

        // 스와이프 새로고침·메인/빈 화면 레이아웃
        swipeRefreshLayout = findViewById(R.id.main_swipe_refresh);
        mainContentLayout = findViewById(R.id.main_content_layout);
        emptyLayout = findViewById(R.id.empty_layout);

        // 팀 진행률·마감 D-day
        tvDeadlineDday = findViewById(R.id.main_team_deadline);
        tvTotalPercent = findViewById(R.id.main_team_total_percent);
        pbAll = findViewById(R.id.main_team_all_progress);

        // 내 작업·닉네임·태그·목표 섹션 토글
        tvMyTaskCount = findViewById(R.id.main_my_task_count);
        tvUserNickname = findViewById(R.id.main_user_nickname);
        tvMyTaskTag = findViewById(R.id.main_my_task_tag);
        tvNoTeamTag = findViewById(R.id.main_no_team_tag);
        tvProgressToggle = findViewById(R.id.tv_progress_toggle);
        tvCompleteToggle = findViewById(R.id.tv_complete_toggle);
        tvProgressEmpty = findViewById(R.id.tv_progress_empty);
        tvCompleteEmpty = findViewById(R.id.tv_complete_empty);

        // 목표·최근 활동 RecyclerView·FAB
        rvProgress = findViewById(R.id.rv_progress);
        rvComplete = findViewById(R.id.rv_complete);
        rvTeamLogs = findViewById(R.id.main_team_list);
        tvTeamLogsEmpty = findViewById(R.id.tv_team_logs_empty);
        btnTeamLogsMore = findViewById(R.id.btn_team_logs_more);
        mainFab = findViewById(R.id.main_float_button);

        // RecyclerView 세로 레이아웃 설정
        rvProgress.setLayoutManager(new LinearLayoutManager(this));
        rvComplete.setLayoutManager(new LinearLayoutManager(this));
        rvTeamLogs.setLayoutManager(new LinearLayoutManager(this));

        // 어댑터 연결 (진행/완료 목표, 최근 활동)
        progressAdapter = new MyGoalAdapter(progressGoals, false, this::onMyGoalClick);
        completeAdapter = new MyGoalAdapter(completeGoals, true, this::onMyGoalClick);
        teamLogAdapter = new TeamLogAdapter(teamLogs);
        rvProgress.setAdapter(progressAdapter);
        rvComplete.setAdapter(completeAdapter);
        rvTeamLogs.setAdapter(teamLogAdapter);

        // 초기: 진행 섹션 펼침, 완료 섹션 접힘
        rvProgress.setVisibility(View.VISIBLE);
        tvProgressToggle.setText("▽");
        rvComplete.setVisibility(View.GONE);
        tvCompleteEmpty.setVisibility(View.GONE);
        tvCompleteToggle.setText("△");

        View layoutProgressHeader = findViewById(R.id.layout_progress_header);
        View layoutCompleteHeader = findViewById(R.id.layout_complete_header);

        // 로그아웃·탈퇴·팀 만들기 버튼
        logoutBtn.setOnClickListener((v) -> logoutBtnClick());
        deleteAccountBtn.setOnClickListener(v -> confirmDeleteAccount());
        noTeamLogoutBtn.setOnClickListener((v) -> logoutBtnClick());
        noTeamDeleteAccountBtn.setOnClickListener(v -> confirmDeleteAccount());
        btnTeamLogsMore.setOnClickListener(v -> {
            teamLogsExpanded = true;
            refreshTeamLogsDisplay();
        });
        noTeamCreateBtn.setOnClickListener(v ->
                startActivity(new Intent(this, CreateTeamActivity.class))
        );

        // 팀 선택·팀 상세·초대 알림
        teamSelectorTv.setOnClickListener(v -> openTeamsBottomSheet());

        View teamOverviewCard = findViewById(R.id.main_team_overview_card);
        teamOverviewCard.setOnClickListener(v -> {
            Long currentTeamId = SessionManager.getInstance().getCurrentTeamId();
            if (currentTeamId == null) {
                showErrorDialog("선택된 팀이 없습니다.");
                return;
            }
            Intent intent = new Intent(this, TeamsActivity.class);
            intent.putExtra(TeamsActivity.EXTRA_TEAM_ID, currentTeamId);
            startActivity(intent);
        });

        notificationBtn.setOnClickListener(v -> openInviteBottomSheet());

        // 진행/완료 목표 섹션 펼침·접힘
        tvProgressToggle.setOnClickListener(v -> toggleGoalSection(true));
        layoutProgressHeader.setOnClickListener(v -> toggleGoalSection(true));

        tvCompleteToggle.setOnClickListener(v -> toggleGoalSection(false));
        layoutCompleteHeader.setOnClickListener(v -> toggleGoalSection(false));

        // FAB 스피드 다이얼 (팀 만들기·초대·목표 추가)
        mainFabMenu = new FabSpeedDialMenu(this, mainFab, Arrays.asList(
                new FabSpeedDialMenu.Item("팀 만들기", R.drawable.pic_menu_create_team, () ->
                        startActivity(new Intent(this, CreateTeamActivity.class))),
                new FabSpeedDialMenu.Item("팀원 초대하기", R.drawable.pic_menu_invite, this::openInviteMemberDialog),
                new FabSpeedDialMenu.Item("목표 추가하기", R.drawable.pic_menu_add_goal, this::openAddGoalDialog)
        ));

        // 당겨서 새로고침 → uid 준비 후 팀/목표 재로드
        swipeRefreshLayout.setOnRefreshListener(() -> ensureUidReady(this::loadTeamsAndGoals));

        // API 응답 전까지 더미 UI가 보이지 않도록 숨김
        showLoadingState();

        // 최초 진입: uid 준비 후 팀/목표 로딩
        applyUserTag();
        ensureUidReady(() -> {
            applyUserTag();
            loadTeamsAndGoals();
        });
    }

    // --- auth ---

    /**
     * SessionManager에 저장된 닉네임·태그를 메인/무팀 레이아웃에 반영함.
     * tvUserNickname에 이름(없으면 "사용자"), tvMyTaskTag·tvNoTeamTag에 태그 뱃지 적용함.
     */
    private void applyUserTag() {
        String tag = SessionManager.getInstance().getTag();
        String name = SessionManager.getInstance().getUserName();
        if (tvUserNickname != null) {
            tvUserNickname.setText(name != null && !name.isEmpty() ? name : "사용자");
        }
        DialogUiHelper.applyTagBadge(tvMyTaskTag, tag);
        if (tvNoTeamTag != null) {
            DialogUiHelper.applyTagBadge(tvNoTeamTag, tag);
        }
    }

    // --- invites ---

    /**
     * 알림(초대) 배지 숫자를 갱신함.
     * count ≤ 0이면 GONE, 9 초과 시 "9+" 표시함.
     */
    private void updateNotificationBadge(int count) {
        if (notificationBadge == null) return;
        if (count <= 0) {
            notificationBadge.setVisibility(View.GONE);
            return;
        }
        notificationBadge.setVisibility(View.VISIBLE);
        notificationBadge.setText(count > 9 ? "9+" : String.valueOf(count));
    }

    /**
     * 받은 팀 초대 목록을 조회해 알림 배지에 반영함.
     * uid 헤더로 초대 목록 가져와 parseInvites 후 size를 배지에 표시함.
     */
    private void loadPendingInviteCount() {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) {
            updateNotificationBadge(0);
            return;
        }

        Request request = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/team/invite")
                .get()
                .addHeader("uid", uid)
                .build();

        ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
            /** 네트워크 실패 시 배지는 그대로 둠. */
            @Override
            public void onFailure(Call call, IOException e) {
                // 배지는 실패 시 유지
            }

            /** 응답 파싱 후 초대 개수로 배지 갱신함. */
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (!response.isSuccessful()) {
                        updateNotificationBadge(0);
                        return;
                    }
                    try {
                        JSONObject root = new JSONObject(resBody);
                        if (!ApiHelper.isSuccess(root)) {
                            updateNotificationBadge(0);
                            return;
                        }
                        updateNotificationBadge(parseInvites(root.optJSONArray("responseData")).size());
                    } catch (Exception e) {
                        updateNotificationBadge(0);
                    }
                });
            }
        });
    }

    /**
     * 화면이 포그라운드로 돌아올 때 데이터 재동기화하고 로그 상대시간 타이머 시작함.
     * uid가 있으면 loadTeamsAndGoals + loadPendingInviteCount 호출함.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (SessionManager.getInstance().getUid() != null) {
            loadTeamsAndGoals();
            loadPendingInviteCount();
        }
        logTimeHandler.postDelayed(logTimeRefreshRunnable, 60_000L);
    }

    /**
     * 화면이 백그라운드로 갈 때 타이머 해제하고 FAB 메뉴 접음.
     */
    @Override
    protected void onPause() {
        super.onPause();
        logTimeHandler.removeCallbacks(logTimeRefreshRunnable);
        if (mainFabMenu != null) {
            mainFabMenu.collapse();
        }
    }

    /**
     * 로그아웃 버튼 처리 — Firebase signOut 후 세션 초기화 및 LoginActivity 이동함.
     */
    private void logoutBtnClick() {
        FirebaseAuth.getInstance().signOut();
        SessionManager.getInstance().clear();

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * 회원 탈퇴 전 ConfirmCancelDialog로 사용자 확인 받음.
     * 확인 시 deleteAccount() 호출함.
     */
    private void confirmDeleteAccount() {
        ConfirmCancelDialog dialog = new ConfirmCancelDialog(
                this,
                "정말 회원 탈퇴하시겠습니까?\n탈퇴 시 팀·목표·초대 정보가 삭제되며 복구할 수 없습니다.",
                "탈퇴",
                "취소"
        );
        dialog.setOnConfirmListener(v -> {
            dialog.dismiss();
            deleteAccount();
        });
        dialog.setOnCancelClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * 백엔드 회원 삭제 후 Firebase 계정도 삭제함.
     * uid 헤더로 삭제 요청 → 성공 시 FirebaseUser.delete() → finishAccountDeletion 호출함.
     */
    private void deleteAccount() {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) {
            showErrorDialog("로그인 정보가 없습니다.");
            return;
        }

        Request request = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/user")
                .delete()
                .addHeader("uid", uid)
                .build();

        ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
            /** 탈퇴 API 네트워크 실패 시 에러 다이얼로그 표시함. */
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> showErrorDialog("회원 탈퇴에 실패했습니다."));
            }

            /** 탈퇴 성공 시 Firebase 계정 삭제 후 finishAccountDeletion 호출함. */
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (!response.isSuccessful()) {
                        showErrorDialog("회원 탈퇴에 실패했습니다.");
                        return;
                    }

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        user.delete().addOnCompleteListener(task -> finishAccountDeletion());
                    } else {
                        finishAccountDeletion();
                    }
                });
            }
        });
    }

    /**
     * 탈퇴 완료 후 세션·Firebase 정리하고 LoginActivity로 이동함.
     */
    private void finishAccountDeletion() {
        FirebaseAuth.getInstance().signOut();
        SessionManager.getInstance().clear();
        showErrorDialog("회원 탈퇴가 완료되었습니다.");
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    /**
     * [중요] API 호출 전 SessionManager에 uid가 준비됐는지 보장함.
     * uid가 이미 있으면 onReady 바로 실행함.
     * 없으면 idToken으로 로그인 API 호출해서 uid·tag·userId 저장 후 onReady 실행함.
     * idToken도 없으면 moveToLogin으로 로그인 화면 보냄.
     */
    private void ensureUidReady(Runnable onReady) {
        SessionManager sm = SessionManager.getInstance();
        String uid = sm.getUid();
        if (uid != null && !uid.isEmpty()) {
            if (onReady != null) onReady.run();
            return;
        }

        String idToken = sm.getIdToken();
        if (idToken == null || idToken.isEmpty()) {
            moveToLogin();
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("idToken", idToken);

            RequestBody body = RequestBody.create(json.toString(), ApiHelper.JSON);

            Request request = new Request.Builder()
                    .url(ApiConfig.BASE_URL + "/api/user/login")
                    .post(body)
                    .build();

            ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
                /** 로그인 API 네트워크 실패 시 새로고침 중단·에러 표시함. */
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        stopRefreshing();
                        showErrorDialog("서버와 연결할 수 없습니다.");
                    });
                }

                /** 로그인 응답에서 uid 파싱·세션 저장 후 onReady 실행함. */
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String resBody = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        if (!response.isSuccessful()) {
                            stopRefreshing();
                            showErrorDialog("로그인 정보를 확인할 수 없습니다.");
                            return;
                        }

                        try {
                            JSONObject root = new JSONObject(resBody);
                            if (!ApiHelper.isSuccess(root)) {
                                stopRefreshing();
                                showErrorDialog(ApiHelper.getMessage(root, "로그인에 실패했습니다."));
                                return;
                            }

                            JSONObject data = root.optJSONObject("responseData");
                            String parsedUid = data != null ? data.optString("uid", "") : "";
                            if (parsedUid.isEmpty()) {
                                stopRefreshing();
                                showErrorDialog("사용자 정보(uid)를 받아오지 못했습니다.");
                                return;
                            }

                            SessionManager sm = SessionManager.getInstance();
                            sm.setUid(parsedUid);
                            if (data != null) {
                                sm.setTag(data.optString("tag", ""));
                                if (data.has("id") && !data.isNull("id")) {
                                    sm.setUserId(data.getLong("id"));
                                }
                            }
                            applyUserTag();
                            if (onReady != null) onReady.run();
                        } catch (Exception e) {
                            stopRefreshing();
                            showErrorDialog("로그인 응답을 처리하지 못했습니다.");
                        }
                    });
                }
            });
        } catch (Exception e) {
            stopRefreshing();
            showErrorDialog("로그인 요청을 만들지 못했습니다.");
        }
    }

    /**
     * 세션 만료·인증 실패 시 Firebase signOut, SessionManager.clear 후 LoginActivity로 이동함.
     */
    private void moveToLogin() {
        FirebaseAuth.getInstance().signOut();
        SessionManager.getInstance().clear();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    // --- refresh ---

    /**
     * 최초 로딩 또는 데이터 대기 중 메인 콘텐츠 숨기고 SwipeRefresh 스피너만 표시함.
     */
    private void showLoadingState() {
        swipeRefreshLayout.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setRefreshing(true);
        mainContentLayout.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.GONE);
    }

    /**
     * 가입한 팀이 없을 때 empty 레이아웃으로 전환함.
     * cancelRefresh, currentTeamId null, resetGoalsUi, FAB만 표시함.
     */
    private void showEmptyTeamState() {
        cancelRefresh();
        swipeRefreshLayout.setVisibility(View.GONE);
        mainContentLayout.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.VISIBLE);
        mainFab.setVisibility(View.VISIBLE);

        SessionManager.getInstance().setCurrentTeamId(null);
        teamSelectorTv.setText("팀 없음");
        applyUserTag();
        resetGoalsUi();
    }

    /**
     * 팀이 존재할 때 메인 콘텐츠 레이아웃으로 전환함.
     */
    private void showTeamContentState() {
        emptyLayout.setVisibility(View.GONE);
        swipeRefreshLayout.setVisibility(View.VISIBLE);
        mainContentLayout.setVisibility(View.VISIBLE);
        mainFab.setVisibility(View.VISIBLE);
    }

    // --- teams/goals ---

    /**
     * 진행 중 또는 완료 목표 섹션 펼침/접힘 토글함.
     * RecyclerView·빈 메시지 visibility와 토글 아이콘(▽/△) 갱신함.
     */
    private void toggleGoalSection(boolean progressSection) {
        if (progressSection) {
            boolean expanded = rvProgress.getVisibility() == View.VISIBLE
                    || tvProgressEmpty.getVisibility() == View.VISIBLE;
            boolean willExpand = !expanded;
            if (progressGoals.isEmpty()) {
                rvProgress.setVisibility(View.GONE);
                tvProgressEmpty.setVisibility(willExpand ? View.VISIBLE : View.GONE);
            } else {
                tvProgressEmpty.setVisibility(View.GONE);
                rvProgress.setVisibility(willExpand ? View.VISIBLE : View.GONE);
            }
            tvProgressToggle.setText(willExpand ? "▽" : "△");
            if (willExpand && !progressGoals.isEmpty()) {
                updateRecyclerViewHeightFully(rvProgress);
            }
        } else {
            completeSectionExpanded = !completeSectionExpanded;
            applyCompleteSectionVisibility();
        }
    }

    /**
     * 목표 데이터 변경 후 진행/완료 섹션 UI 일괄 동기화함.
     */
    private void updateGoalsSectionUi() {
        if (progressGoals.isEmpty()) {
            rvProgress.setVisibility(View.GONE);
            tvProgressEmpty.setVisibility(View.VISIBLE);
        } else {
            rvProgress.setVisibility(View.VISIBLE);
            tvProgressEmpty.setVisibility(View.GONE);
            updateRecyclerViewHeightFully(rvProgress);
        }

        applyCompleteSectionVisibility();

        tvProgressToggle.setText(rvProgress.getVisibility() == View.VISIBLE
                || tvProgressEmpty.getVisibility() == View.VISIBLE ? "▽" : "△");
    }

    /**
     * completeSectionExpanded 플래그에 따라 완료 목표 섹션 표시 여부 결정함.
     */
    private void applyCompleteSectionVisibility() {
        if (completeGoals.isEmpty()) {
            rvComplete.setVisibility(View.GONE);
            tvCompleteEmpty.setVisibility(completeSectionExpanded ? View.VISIBLE : View.GONE);
        } else if (completeSectionExpanded) {
            rvComplete.setVisibility(View.VISIBLE);
            tvCompleteEmpty.setVisibility(View.GONE);
            updateRecyclerViewHeightFully(rvComplete);
        } else {
            rvComplete.setVisibility(View.GONE);
            tvCompleteEmpty.setVisibility(View.GONE);
        }
        tvCompleteToggle.setText(completeSectionExpanded ? "▽" : "△");
        updateMyTaskCount();
    }

    /**
     * "내 작업 N개" 라벨에 progressGoals 크기 반영함.
     */
    private void updateMyTaskCount() {
        if (tvMyTaskCount == null) return;
        tvMyTaskCount.setText(progressGoals.size() + "개");
    }

    // --- UI helpers ---

    /**
     * NestedScrollView 안에서 RecyclerView가 전체 아이템 높이만큼 펼쳐지도록 measure 후 height 설정함.
     */
    @SuppressWarnings("unchecked")
    private void updateRecyclerViewHeightFully(RecyclerView recyclerView) {
        RecyclerView.Adapter adapter = recyclerView.getAdapter();
        if (adapter == null || adapter.getItemCount() == 0) return;

        recyclerView.post(() -> {
            int width = recyclerView.getWidth();
            if (width <= 0) {
                recyclerView.post(() -> updateRecyclerViewHeightFully(recyclerView));
                return;
            }

            int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
            int height = 0;
            float density = getResources().getDisplayMetrics().density;
            int defaultBottomMargin = Math.round(8 * density);

            for (int i = 0; i < adapter.getItemCount(); i++) {
                int viewType = adapter.getItemViewType(i);
                RecyclerView.ViewHolder holder = adapter.createViewHolder(recyclerView, viewType);
                adapter.onBindViewHolder(holder, i);
                holder.itemView.measure(
                        widthSpec,
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                );
                height += holder.itemView.getMeasuredHeight();
                ViewGroup.MarginLayoutParams lp =
                        (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
                if (lp != null) {
                    height += lp.topMargin + lp.bottomMargin;
                } else {
                    height += defaultBottomMargin;
                }
            }

            ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
            params.height = height;
            recyclerView.setLayoutParams(params);
        });
    }

    /**
     * SwipeRefreshLayout refreshing 인디케이터 끔.
     */
    private void stopRefreshing() {
        swipeRefreshLayout.setRefreshing(false);
    }

    /**
     * 새로고침 세션 시작하고 스피너 켬.
     * refreshInProgress = true 설정함. loadTeamsAndGoals 진입 시 호출됨.
     */
    private void markRefreshStarted() {
        refreshInProgress = true;
        swipeRefreshLayout.setRefreshing(true);
    }

    /**
     * [중요] 병렬 API 완료 대기 카운터 설정함.
     * loadTeamsAndGoals 성공 후 beginRefreshSections(2) 호출하고,
     * loadGoalsForTeam·loadTeamLogs 각각 finishRefreshSection() 호출할 때마다 카운트 줄어듦.
     */
    private void beginRefreshSections(int count) {
        pendingRefreshSections = count;
    }

    /**
     * [중요] 새로고침 섹션 하나 완료됐음을 알리고, 모두 끝나면 스피너 끔.
     * pendingRefreshSections가 0 이하가 되면 stopRefreshing + loadPendingInviteCount 호출함.
     */
    private void finishRefreshSection() {
        if (!refreshInProgress) return;
        pendingRefreshSections--;
        if (pendingRefreshSections <= 0) {
            refreshInProgress = false;
            stopRefreshing();
            loadPendingInviteCount();
        }
    }

    /**
     * 에러 등으로 새로고침 세션 즉시 중단함.
     * refreshInProgress·pendingRefreshSections 초기화 후 stopRefreshing 호출함.
     */
    private void cancelRefresh() {
        refreshInProgress = false;
        pendingRefreshSections = 0;
        stopRefreshing();
    }

    /**
     * 팀 없음 또는 팀 변경 시 목표·로그 관련 UI와 내부 리스트 초기화함.
     */
    private void resetGoalsUi() {
        progressGoals.clear();
        completeGoals.clear();
        progressAdapter.notifyDataSetChanged();
        completeAdapter.notifyDataSetChanged();

        tvMyTaskCount.setText("0개");
        tvTotalPercent.setText("0%");
        pbAll.setProgress(0);
        tvDeadlineDday.setText("D-?");

        rvProgress.setVisibility(View.GONE);
        rvComplete.setVisibility(View.GONE);
        tvProgressEmpty.setVisibility(View.GONE);
        tvCompleteEmpty.setVisibility(View.GONE);
        tvProgressToggle.setText("▽");
        tvCompleteToggle.setText("△");
        completeSectionExpanded = false;
        lastGoalsTeamId = null;

        teamLogs.clear();
        allTeamLogs.clear();
        teamLogsExpanded = false;
        teamLogAdapter.notifyDataSetChanged();
        if (btnTeamLogsMore != null) {
            btnTeamLogsMore.setVisibility(View.GONE);
        }
        rvTeamLogs.setVisibility(View.GONE);
        tvTeamLogsEmpty.setVisibility(View.GONE);
    }

    /**
     * 내 목표 항목 클릭 시 GoalDetailDialog 엶.
     * 수정 완료 콜백에서 loadGoalsForTeam + loadTeamLogs로 목록 재동기화함.
     */
    private void onMyGoalClick(GoalItem item) {
        TeamGoalItem goal = new TeamGoalItem(
                item.id,
                item.userId,
                item.goalText,
                item.remark,
                item.status,
                item.goalEndDate
        );
        GoalDetailDialog.show(
                this,
                ApiHelper.CLIENT,
                goal,
                isLeader,
                () -> {
                    Long teamId = SessionManager.getInstance().getCurrentTeamId();
                    if (teamId != null) {
                        loadGoalsForTeam(teamId);
                        loadTeamLogs(teamId);
                    }
                }
        );
    }

    /**
     * 공통 에러 다이얼로그 표시함.
     */
    private void showErrorDialog(String message) {
        CommonDialog.showError(this, message);
    }

    /**
     * 알림 버튼 클릭 — uid 준비 후 초대 바텀시트 엶.
     */
    private void openInviteBottomSheet() {
        ensureUidReady(this::showInviteBottomSheetInternal);
    }

    /**
     * 초대 목록 BottomSheetDialog 구성하고 API로 목록 로드함.
     * 수락/거절 시 handleInviteAction → 목록·배지 갱신함.
     */
    private void showInviteBottomSheetInternal() {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.main_bottom_sheet_invite, null);
        dialog.setContentView(view);

        RecyclerView rvInvite = view.findViewById(R.id.invite_recycler);
        TextView inviteEmptyTv = view.findViewById(R.id.invite_empty_text);
        rvInvite.setLayoutManager(new LinearLayoutManager(this));

        List<InviteItem> inviteList = new ArrayList<>();
        // 익명 리스너 내부에서 adapter 참조가 필요해서, 초기화 순서 문제를 피하기 위한 홀더 사용
        InviteAdapter[] adapterHolder = new InviteAdapter[1];
        InviteAdapter adapter = new InviteAdapter(inviteList, new InviteAdapter.InviteListener() {
            /** 초대 수락 — handleInviteAction 후 목록·배지 갱신·팀 재로드함. */
            @Override
            public void onAccept(InviteItem item) {
                handleInviteAction(item.id, true, () -> {
                    inviteList.remove(item);
                    if (adapterHolder[0] != null) adapterHolder[0].notifyDataSetChanged();
                    updateNotificationBadge(inviteList.size());
                    if (inviteList.isEmpty()) {
                        dialog.dismiss();
                    } else {
                        inviteEmptyTv.setVisibility(View.GONE);
                        rvInvite.setVisibility(View.VISIBLE);
                    }
                    loadTeamsAndGoals();
                });
            }

            /** 초대 거절 — handleInviteAction 후 목록·배지 갱신함. */
            @Override
            public void onReject(InviteItem item) {
                handleInviteAction(item.id, false, () -> {
                    inviteList.remove(item);
                    if (adapterHolder[0] != null) adapterHolder[0].notifyDataSetChanged();
                    updateNotificationBadge(inviteList.size());
                    loadTeamsAndGoals();
                });
            }
        });
        adapterHolder[0] = adapter;
        rvInvite.setAdapter(adapter);

        // 서버에서 초대 목록 가져오기
        Request request = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/team/invite")
                .get()
                .addHeader("uid", uid)
                .build();

        ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
            /** 초대 목록 API 네트워크 실패 시 에러 표시함. */
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> showErrorDialog("초대 목록을 불러오지 못했습니다."));
            }

            /** 초대 목록 파싱 후 RecyclerView·빈 화면·배지 갱신함. */
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    try {
                        if (!response.isSuccessful()) {
                            showErrorDialog("초대 목록 요청에 실패했습니다.");
                            return;
                        }

                        JSONObject root = new JSONObject(resBody);
                        if (!ApiHelper.isSuccess(root)) {
                            showErrorDialog(ApiHelper.getMessage(root, "초대 목록을 불러오지 못했습니다."));
                            return;
                        }

                        inviteList.clear();
                        inviteList.addAll(parseInvites(root.optJSONArray("responseData")));
                        adapter.notifyDataSetChanged();
                        updateNotificationBadge(inviteList.size());
                        if (inviteList.isEmpty()) {
                            inviteEmptyTv.setVisibility(View.VISIBLE);
                            rvInvite.setVisibility(View.GONE);
                        } else {
                            inviteEmptyTv.setVisibility(View.GONE);
                            rvInvite.setVisibility(View.VISIBLE);
                        }
                    } catch (Exception e) {
                        showErrorDialog("초대 목록 응답을 처리하지 못했습니다.");
                    }
                });
            }
        });

        dialog.show();
    }

    /**
     * [중요] 팀 목록 조회하고 현재 팀의 목표·활동 로그 연쇄 로드함.
     * uid 헤더로 팀 목록 가져옴. 팀 없으면 showEmptyTeamState,
     * 있으면 currentTeamId 결정 → beginRefreshSections(2) → loadGoalsForTeam + loadTeamLogs 병렬 호출함.
     */
    private void loadTeamsAndGoals() {
        markRefreshStarted();

        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) {
            cancelRefresh();
            return;
        }

        Request request = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/team")
                .get()
                .addHeader("uid", uid)
                .build();

        ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
            /** 팀 목록 API 네트워크 실패 시 새로고침 취소·에러 표시함. */
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    cancelRefresh();
                    showErrorDialog("팀 목록을 불러오지 못했습니다.");
                    if (teamList.isEmpty()) {
                        showEmptyTeamState();
                    }
                });
            }

            /** 팀 목록 파싱 후 현재 팀 정하고 목표·로그 병렬 로드 시작함. */
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    try {
                        if (!response.isSuccessful()) {
                            cancelRefresh();
                            showErrorDialog("팀 목록 요청에 실패했습니다.");
                            if (teamList.isEmpty()) {
                                showEmptyTeamState();
                            }
                            return;
                        }

                        JSONObject root = new JSONObject(resBody);
                        if (!ApiHelper.isSuccess(root)) {
                            cancelRefresh();
                            showErrorDialog(ApiHelper.getMessage(root, "팀 목록을 불러오지 못했습니다."));
                            if (teamList.isEmpty()) {
                                showEmptyTeamState();
                            }
                            return;
                        }

                        teamList.clear();
                        teamList.addAll(TeamItem.parseList(resBody));

                        if (teamList.isEmpty()) {
                            showEmptyTeamState();
                            return;
                        }

                        showTeamContentState();

                        Long currentTeamId = SessionManager.getInstance().getCurrentTeamId();
                        TeamItem current = findTeamById(currentTeamId);
                        if (current == null) {
                            current = teamList.get(0);
                            SessionManager.getInstance().setCurrentTeamId(current.id);
                        }

                        applyTeamHeader(current);
                        beginRefreshSections(2);
                        loadGoalsForTeam(current.id);
                        loadTeamLogs(current.id);
                    } catch (Exception e) {
                        cancelRefresh();
                        showErrorDialog("팀 목록 응답을 처리하지 못했습니다.");
                        if (teamList.isEmpty()) {
                            showEmptyTeamState();
                        }
                    }
                });
            }
        });
    }


    /**
     * 초대 JSON 배열을 InviteItem 리스트로 파싱함.
     */
    private List<InviteItem> parseInvites(JSONArray arr) {
        List<InviteItem> result = new ArrayList<>();
        if (arr == null) return result;

        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject obj = arr.getJSONObject(i);
                if (!obj.has("id") || !obj.has("teamId")) continue;

                long id = obj.getLong("id");
                long teamId = obj.getLong("teamId");
                String teamName = obj.optString("teamName", "알 수 없는 팀");
                String inviterName = obj.optString("inviterName", "");

                String msg = inviterName.isEmpty()
                        ? "팀 초대가 도착했습니다."
                        : inviterName + " 님이 초대했습니다.";

                result.add(new InviteItem(id, teamId, teamName, inviterName, msg));
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    /**
     * teamList에서 id로 TeamItem 찾음. 없으면 null 반환함.
     */
    private TeamItem findTeamById(Long id) {
        if (id == null) return null;
        for (TeamItem t : teamList) {
            if (t.id == id) return t;
        }
        return null;
    }

    /**
     * 상단 팀 선택기에 팀 이름·마감 D-day 표시함.
     */
    private void applyTeamHeader(TeamItem team) {
        teamSelectorTv.setText(team.teamName);
        tvDeadlineDday.setText(DateTimeUtil.formatDday(team.endDate));
    }

    /**
     * 특정 팀의 팀원·목표 조회해서 진행률과 내 작업 목록 갱신함.
     * 팀 변경 시 completeSectionExpanded 초기화함. 완료 시 finishRefreshSection 호출함.
     */
    private void loadGoalsForTeam(long teamId) {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        if (lastGoalsTeamId == null || lastGoalsTeamId != teamId) {
            completeSectionExpanded = false;
            lastGoalsTeamId = teamId;
        }

        Request membersRequest = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/team/" + teamId + "/members")
                .get()
                .addHeader("uid", uid)
                .build();

        Request goalsRequest = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/goal/team/" + teamId + "/all")
                .get()
                .addHeader("uid", uid)
                .build();

        ApiHelper.CLIENT.newCall(membersRequest).enqueue(new Callback() {
            /** 팀원 API 네트워크 실패 시 에러 표시·새로고침 섹션 완료 처리함. */
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showErrorDialog("목표 목록을 불러오지 못했습니다.");
                    finishRefreshSection();
                });
            }

            /** 팀원 응답 성공 시 목표 API 연쇄 호출함. */
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String membersBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        showErrorDialog("팀원 정보를 불러오지 못했습니다.");
                        finishRefreshSection();
                    });
                    return;
                }

                ApiHelper.CLIENT.newCall(goalsRequest).enqueue(new Callback() {
                    /** 목표 API 네트워크 실패 시 에러 표시·새로고침 섹션 완료 처리함. */
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> {
                            showErrorDialog("목표 목록을 불러오지 못했습니다.");
                            finishRefreshSection();
                        });
                    }

                    /** 목표 파싱 후 진행률·내 작업 목록 UI 갱신함. */
                    @Override
                    public void onResponse(Call call, Response goalsResponse) throws IOException {
                        String goalsBody = goalsResponse.body() != null ? goalsResponse.body().string() : "";
                        runOnUiThread(() -> {
                            try {
                                if (!goalsResponse.isSuccessful()) {
                                    showErrorDialog("목표 목록 요청에 실패했습니다.");
                                    finishRefreshSection();
                                    return;
                                }

                                JSONObject goalsRoot = new JSONObject(goalsBody);
                                if (!ApiHelper.isSuccess(goalsRoot)) {
                                    showErrorDialog(ApiHelper.getMessage(goalsRoot, "목표 목록을 불러오지 못했습니다."));
                                    finishRefreshSection();
                                    return;
                                }

                                List<TeamMemberItem> members = TeamMemberItem.parseList(membersBody);
                                Long myUserId = SessionManager.getInstance().getUserId();
                                isLeader = false;
                                for (TeamMemberItem member : members) {
                                    if (myUserId != null && myUserId == member.userId) {
                                        isLeader = member.leader;
                                        if (member.name != null && !member.name.isEmpty()) {
                                            SessionManager.getInstance().setUserName(member.name);
                                            applyUserTag();
                                        }
                                        break;
                                    }
                                }

                                progressGoals.clear();
                                completeGoals.clear();

                                JSONArray goalArr = goalsRoot.optJSONArray("responseData");
                                List<GoalItem> allGoals = parseGoals(goalArr);
                                int teamTotal = 0;
                                int teamCompleted = 0;

                                for (GoalItem item : allGoals) {
                                    if (item.status == 0) {
                                        teamTotal++;
                                    } else if (item.status == 1) {
                                        teamTotal++;
                                        teamCompleted++;
                                    }

                                    if (myUserId != null && item.userId == myUserId) {
                                        if (item.status == 0) progressGoals.add(item);
                                        else if (item.status == 1) completeGoals.add(item);
                                    }
                                }

                                double percent = teamTotal == 0 ? 0.0 : (teamCompleted * 100.0 / teamTotal);
                                tvTotalPercent.setText(String.format("%.1f%%", percent));
                                pbAll.setProgress((int) Math.round(percent));

                                progressAdapter.notifyDataSetChanged();
                                completeAdapter.notifyDataSetChanged();
                                updateGoalsSectionUi();
                            } catch (Exception e) {
                                showErrorDialog("목표 목록 응답을 처리하지 못했습니다.");
                            } finally {
                                finishRefreshSection();
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * 목표 JSON 배열을 GoalItem 리스트로 파싱함.
     */
    private List<GoalItem> parseGoals(JSONArray arr) {
        List<GoalItem> result = new ArrayList<>();
        if (arr == null) return result;

        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject obj = arr.getJSONObject(i);
                if (!obj.has("id")) continue;

                long id = obj.getLong("id");
                String goalText = obj.optString("goalText", "");
                int status = obj.optInt("status", 0);
                long userId = obj.optLong("userId", -1);
                String remark = obj.optString("remark", "");
                String goalEndDate = obj.has("goalEndDate") && !obj.isNull("goalEndDate")
                        ? obj.optString("goalEndDate", null)
                        : null;

                result.add(new GoalItem(id, userId, goalText, status, remark, goalEndDate));
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    /**
     * 팀 선택 BottomSheet 엶. 팀 변경 시 목표·로그 재로드함.
     */
    private void openTeamsBottomSheet() {
        Long currentTeamId = SessionManager.getInstance().getCurrentTeamId();
        TeamPickerBottomSheet.show(
                this,
                teamList,
                currentTeamId,
                selectedTeam -> {
                    SessionManager.getInstance().setCurrentTeamId(selectedTeam.id);
                    applyTeamHeader(selectedTeam);
                    loadGoalsForTeam(selectedTeam.id);
                    loadTeamLogs(selectedTeam.id);
                },
                () -> startActivity(new Intent(this, CreateTeamActivity.class))
        );
    }

    /**
     * FAB "목표 추가하기" — 팀원 조회 후 AddGoalDialog 엶.
     * 성공 시 loadGoalsForTeam + loadTeamLogs 호출함.
     */
    private void openAddGoalDialog() {
        Long teamId = SessionManager.getInstance().getCurrentTeamId();
        if (teamId == null) {
            showErrorDialog("목표를 추가할 팀이 없습니다.\n팀을 먼저 만들거나 선택해 주세요.");
            return;
        }

        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        Request request = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/team/" + teamId + "/members")
                .get()
                .addHeader("uid", uid)
                .build();

        ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
            /** 팀원 API 네트워크 실패 시 에러 표시함. */
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> showErrorDialog("팀원 정보를 불러오지 못했습니다."));
            }

            /** 팀원 파싱 후 AddGoalDialog 표시함. */
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (!response.isSuccessful()) {
                        showErrorDialog("팀원 정보를 불러오지 못했습니다.");
                        return;
                    }
                    try {
                        List<TeamMemberItem> members = TeamMemberItem.parseList(body);
                        List<TeamMemberItem> eligible = getGoalAddEligibleMembers(members);
                        if (eligible.isEmpty()) {
                            showErrorDialog("목표를 추가할 수 있는 팀원이 없습니다.");
                            return;
                        }

                        AddGoalDialog.show(MainActivity.this, ApiHelper.CLIENT, teamId, eligible, null,
                                new AddGoalDialog.OnComplete() {
                                    /** 목표 추가 성공 시 목표·로그 재로드함. */
                                    @Override
                                    public void onSuccess() {
                                        loadGoalsForTeam(teamId);
                                        loadTeamLogs(teamId);
                                    }

                                    /** 목표 추가 실패 시 에러 메시지 표시함. */
                                    @Override
                                    public void onError(String message) {
                                        showErrorDialog(message);
                                    }
                                });
                    } catch (Exception e) {
                        showErrorDialog("팀원 정보 처리에 실패했습니다.");
                    }
                });
            }
        });
    }

    /**
     * 목표 추가 대상 팀원 필터링함. 팀장은 전원, 일반 팀원은 본인만 해당됨.
     */
    private List<TeamMemberItem> getGoalAddEligibleMembers(List<TeamMemberItem> members) {
        List<TeamMemberItem> eligible = new ArrayList<>();
        Long myUserId = SessionManager.getInstance().getUserId();
        boolean leader = isCurrentTeamLeader();
        for (TeamMemberItem member : members) {
            if (leader || (myUserId != null && myUserId == member.userId)) {
                eligible.add(member);
            }
        }
        return eligible;
    }

    /**
     * FAB "팀원 초대하기" — 팀장만 InviteMemberDialog 엶.
     */
    private void openInviteMemberDialog() {
        Long teamId = SessionManager.getInstance().getCurrentTeamId();
        if (teamId == null) {
            showErrorDialog("초대할 팀이 없습니다.\n팀을 먼저 만들거나 선택해 주세요.");
            return;
        }
        if (!isCurrentTeamLeader()) {
            showErrorDialog("팀장만 팀원을 초대할 수 있습니다.");
            return;
        }

        InviteMemberDialog.show(this, teamId, (success, message) -> {
            if (message != null && !message.isEmpty()) {
                showErrorDialog(message);
            }
            if (success) {
                loadTeamLogs(teamId);
            }
        });
    }

    /**
     * 현재 선택 팀에서 내가 팀장(leaderId)인지 확인함.
     */
    private boolean isCurrentTeamLeader() {
        Long myUserId = SessionManager.getInstance().getUserId();
        TeamItem current = findTeamById(SessionManager.getInstance().getCurrentTeamId());
        return myUserId != null && current != null && myUserId == current.leaderId;
    }

    // --- team logs ---

    /**
     * [중요] 팀 활동 로그 API로 조회해서 최근 활동 UI 갱신함.
     * ApiHelper.uidRequest로 uid 헤더 자동 첨부함 — uid 없으면 빈 목록 표시 후 finishRefreshSection 호출함.
     * 완료 시 finishRefreshSection 호출해서 새로고침 카운터 줄임.
     */
    private void loadTeamLogs(long teamId) {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) {
            updateTeamLogsUi(new ArrayList<>());
            finishRefreshSection();
            return;
        }

        Request request = ApiHelper.uidRequest(ApiConfig.BASE_URL + "/api/team/log/" + teamId)
                .get()
                .build();

        ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
            /** 로그 API 네트워크 실패 시 빈 목록 표시·새로고침 섹션 완료 처리함. */
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    updateTeamLogsUi(new ArrayList<>());
                    finishRefreshSection();
                });
            }

            /** 로그 파싱 후 최근 활동 UI 갱신함. */
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    try {
                        if (!response.isSuccessful()) {
                            updateTeamLogsUi(new ArrayList<>());
                            return;
                        }
                        JSONObject root = new JSONObject(resBody);
                        if (!ApiHelper.isSuccess(root)) {
                            updateTeamLogsUi(new ArrayList<>());
                            return;
                        }
                        updateTeamLogsUi(parseTeamLogs(root.optJSONArray("responseData")));
                    } catch (Exception e) {
                        updateTeamLogsUi(new ArrayList<>());
                    } finally {
                        finishRefreshSection();
                    }
                });
            }
        });
    }

    /**
     * 활동 로그 JSON 배열 파싱함. 최대 10개, createdAtMillis 보정 포함함.
     */
    private List<TeamLogItem> parseTeamLogs(JSONArray arr) {
        List<TeamLogItem> result = new ArrayList<>();
        if (arr == null) return result;

        int limit = Math.min(arr.length(), 10);
        for (int i = 0; i < limit; i++) {
            try {
                JSONObject obj = arr.getJSONObject(i);
                if (!obj.has("id")) continue;
                long id = obj.getLong("id");
                String message = obj.optString("message", "");
                long createdAtMillis = 0L;
                if (obj.has("createdAtMillis") && !obj.isNull("createdAtMillis")) {
                    createdAtMillis = obj.getLong("createdAtMillis");
                    if (createdAtMillis > 0L && createdAtMillis < 1_000_000_000_000L) {
                        createdAtMillis *= 1000L;
                    }
                }
                if (createdAtMillis <= 0L) {
                    createdAtMillis = DateTimeUtil.parseCreatedAtMillis(
                            obj.has("createdAt") && !obj.isNull("createdAt") ? obj.get("createdAt") : null
                    );
                }
                result.add(new TeamLogItem(id, message, createdAtMillis));
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    /**
     * 파싱된 로그를 allTeamLogs에 저장하고 접힌 상태로 화면 갱신함.
     */
    private void updateTeamLogsUi(List<TeamLogItem> parsedLogs) {
        allTeamLogs.clear();
        allTeamLogs.addAll(parsedLogs);
        teamLogsExpanded = false;
        refreshTeamLogsDisplay();
    }

    /**
     * teamLogsExpanded 플래그에 따라 표시할 로그 개수 결정하고 RecyclerView 갱신함.
     * 접힘: TEAM_LOGS_COLLAPSED_COUNT, 펼침: TEAM_LOGS_MAX_COUNT. 더보기 버튼 visibility 제어함.
     */
    private void refreshTeamLogsDisplay() {
        teamLogs.clear();
        if (allTeamLogs.isEmpty()) {
            teamLogAdapter.notifyDataSetChanged();
            rvTeamLogs.setVisibility(View.GONE);
            tvTeamLogsEmpty.setVisibility(View.VISIBLE);
            btnTeamLogsMore.setVisibility(View.GONE);
            return;
        }

        int displayCount = teamLogsExpanded
                ? Math.min(allTeamLogs.size(), TEAM_LOGS_MAX_COUNT)
                : Math.min(allTeamLogs.size(), TEAM_LOGS_COLLAPSED_COUNT);
        for (int i = 0; i < displayCount; i++) {
            teamLogs.add(allTeamLogs.get(i));
        }
        teamLogAdapter.notifyDataSetChanged();

        rvTeamLogs.setVisibility(View.VISIBLE);
        tvTeamLogsEmpty.setVisibility(View.GONE);
        btnTeamLogsMore.setVisibility(
                !teamLogsExpanded && allTeamLogs.size() > TEAM_LOGS_COLLAPSED_COUNT
                        ? View.VISIBLE
                        : View.GONE
        );
        rvTeamLogs.post(() -> updateRecyclerViewHeightFully(rvTeamLogs));
    }

    /**
     * [중요] 팀 초대 수락 또는 거절 API 호출함.
     * uid 헤더 필수임. accept면 accept 경로, reject면 reject 경로로 POST 요청함.
     * 성공 시 onSuccess 콜백 실행해서 목록·배지·팀 데이터 갱신 이어감.
     */
    private void handleInviteAction(long inviteId, boolean accept, Runnable onSuccess) {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        String path = accept ? "accept" : "reject";
        String url = ApiConfig.BASE_URL + "/api/team/invite/" + inviteId + "/" + path;

        RequestBody body = RequestBody.create("{}", ApiHelper.JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("uid", uid)
                .build();

        ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
            /** 초대 처리 API 네트워크 실패 시 에러 표시함. */
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> showErrorDialog("초대 처리에 실패했습니다."));
            }

            /** 초대 처리 성공 시 onSuccess 콜백 실행함. */
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    try {
                        if (response.isSuccessful()) {
                            JSONObject root = new JSONObject(body);
                            if (ApiHelper.isSuccess(root)) {
                                if (onSuccess != null) {
                                    onSuccess.run();
                                }
                                return;
                            }
                        }
                        showErrorDialog(accept ? "초대 수락에 실패했습니다." : "초대 거절에 실패했습니다.");
                    } catch (Exception e) {
                        showErrorDialog(accept ? "초대 수락에 실패했습니다." : "초대 거절에 실패했습니다.");
                    }
                });
            }
        });
    }
}

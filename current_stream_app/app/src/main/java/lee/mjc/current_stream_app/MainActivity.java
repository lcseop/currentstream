package lee.mjc.current_stream_app;

import android.content.Intent;
import android.os.Build;
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

import androidx.appcompat.app.AlertDialog;
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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://10.0.2.2:8080";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final int TEAM_LOGS_COLLAPSED_COUNT = 3;
    private static final int TEAM_LOGS_MAX_COUNT = 10;

    Button logoutBtn, noTeamLogoutBtn, noTeamCreateBtn;
    TextView deleteAccountBtn, noTeamDeleteAccountBtn;
    ImageButton notificationBtn;
    TextView teamSelectorTv;
    TextView notificationBadge;

    private final OkHttpClient client = new OkHttpClient();

    private SwipeRefreshLayout swipeRefreshLayout;
    private View mainContentLayout;
    private View emptyLayout;

    private TextView tvDeadlineDday;
    private TextView tvTotalPercent;
    private ProgressBar pbAll;

    private TextView tvMyTaskCount;
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

    private final List<TeamLogItem> allTeamLogs = new ArrayList<>();
    private boolean teamLogsExpanded = false;

    private final List<TeamItem> teamList = new ArrayList<>();
    private final List<GoalItem> progressGoals = new ArrayList<>();
    private final List<GoalItem> completeGoals = new ArrayList<>();
    private final List<TeamLogItem> teamLogs = new ArrayList<>();

    private GoalAdapter progressAdapter;
    private GoalAdapter completeAdapter;
    private TeamLogAdapter teamLogAdapter;
    private FabSpeedDialMenu mainFabMenu;

    private int pendingRefreshSections = 0;
    private boolean refreshInProgress = false;
    private String myUserColor = "#E8E8E8";

    private final Handler logTimeHandler = new Handler(Looper.getMainLooper());
    private final Runnable logTimeRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (!teamLogs.isEmpty() && teamLogAdapter != null) {
                teamLogAdapter.notifyDataSetChanged();
            }
            logTimeHandler.postDelayed(this, 60_000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logoutBtn = findViewById(R.id.main_team_logout);
        deleteAccountBtn = findViewById(R.id.main_team_delete_account);
        noTeamLogoutBtn = findViewById(R.id.main_no_team_logout);
        noTeamDeleteAccountBtn = findViewById(R.id.main_no_team_delete_account);
        noTeamCreateBtn = findViewById(R.id.main_no_team_create);
        notificationBtn = findViewById(R.id.main_notification);
        notificationBadge = findViewById(R.id.main_notification_badge);
        teamSelectorTv = findViewById(R.id.main_team_selector);

        swipeRefreshLayout = findViewById(R.id.main_swipe_refresh);
        mainContentLayout = findViewById(R.id.main_content_layout);
        emptyLayout = findViewById(R.id.empty_layout);

        tvDeadlineDday = findViewById(R.id.main_team_deadline);
        tvTotalPercent = findViewById(R.id.main_team_total_percent);
        pbAll = findViewById(R.id.main_team_all_progress);

        tvMyTaskCount = findViewById(R.id.main_my_task_count);
        tvMyTaskTag = findViewById(R.id.main_my_task_tag);
        tvNoTeamTag = findViewById(R.id.main_no_team_tag);
        tvProgressToggle = findViewById(R.id.tv_progress_toggle);
        tvCompleteToggle = findViewById(R.id.tv_complete_toggle);
        tvProgressEmpty = findViewById(R.id.tv_progress_empty);
        tvCompleteEmpty = findViewById(R.id.tv_complete_empty);

        rvProgress = findViewById(R.id.rv_progress);
        rvComplete = findViewById(R.id.rv_complete);
        rvTeamLogs = findViewById(R.id.main_team_list);
        tvTeamLogsEmpty = findViewById(R.id.tv_team_logs_empty);
        btnTeamLogsMore = findViewById(R.id.btn_team_logs_more);
        mainFab = findViewById(R.id.main_float_button);

        rvProgress.setLayoutManager(new LinearLayoutManager(this));
        rvComplete.setLayoutManager(new LinearLayoutManager(this));
        rvTeamLogs.setLayoutManager(new LinearLayoutManager(this));

        progressAdapter = new GoalAdapter(progressGoals);
        completeAdapter = new GoalAdapter(completeGoals);
        teamLogAdapter = new TeamLogAdapter(teamLogs);
        rvProgress.setAdapter(progressAdapter);
        rvComplete.setAdapter(completeAdapter);
        rvTeamLogs.setAdapter(teamLogAdapter);

        // 초기 상태: 진행/완료 목록 펼침
        rvProgress.setVisibility(View.VISIBLE);
        tvProgressToggle.setText("▽");
        rvComplete.setVisibility(View.VISIBLE);
        tvCompleteToggle.setText("▽");

        View layoutProgressHeader = findViewById(R.id.layout_progress_header);
        View layoutCompleteHeader = findViewById(R.id.layout_complete_header);

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

        tvProgressToggle.setOnClickListener(v -> toggleGoalSection(true));
        layoutProgressHeader.setOnClickListener(v -> toggleGoalSection(true));

        tvCompleteToggle.setOnClickListener(v -> toggleGoalSection(false));
        layoutCompleteHeader.setOnClickListener(v -> toggleGoalSection(false));

        mainFabMenu = new FabSpeedDialMenu(this, mainFab, Arrays.asList(
                new FabSpeedDialMenu.Item("팀 만들기", R.drawable.pic_menu_create_team, () ->
                        startActivity(new Intent(this, CreateTeamActivity.class))),
                new FabSpeedDialMenu.Item("멤버 초대하기", R.drawable.pic_menu_invite, this::openInviteMemberDialog),
                new FabSpeedDialMenu.Item("목표 추가하기", R.drawable.pic_menu_add_goal, this::openAddGoalDialog)
        ));

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

    private void applyUserTag() {
        String tag = SessionManager.getInstance().getTag();
        if (tag != null && !tag.isEmpty()) {
            tvMyTaskTag.setText(tag);
            if (tvNoTeamTag != null) {
                tvNoTeamTag.setText(tag);
            }
        }
    }

    private void updateNotificationBadge(int count) {
        if (notificationBadge == null) return;
        if (count <= 0) {
            notificationBadge.setVisibility(View.GONE);
            return;
        }
        notificationBadge.setVisibility(View.VISIBLE);
        notificationBadge.setText(count > 9 ? "9+" : String.valueOf(count));
    }

    private void loadPendingInviteCount() {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) {
            updateNotificationBadge(0);
            return;
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "/api/team/invite")
                .get()
                .addHeader("uid", uid)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 배지는 실패 시 유지
            }

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
                        if (!isSuccessResponse(root)) {
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

    @Override
    protected void onResume() {
        super.onResume();
        if (SessionManager.getInstance().getUid() != null) {
            loadTeamsAndGoals();
            loadPendingInviteCount();
        }
        logTimeHandler.postDelayed(logTimeRefreshRunnable, 60_000L);
    }

    @Override
    protected void onPause() {
        super.onPause();
        logTimeHandler.removeCallbacks(logTimeRefreshRunnable);
        if (mainFabMenu != null) {
            mainFabMenu.collapse();
        }
    }

    private void logoutBtnClick() {
        FirebaseAuth.getInstance().signOut();
        SessionManager.getInstance().clear();

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setMessage("정말 회원 탈퇴하시겠습니까?\n탈퇴 시 팀·목표·초대 정보가 삭제되며 복구할 수 없습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("탈퇴", (d, w) -> deleteAccount())
                .show();
    }

    private void deleteAccount() {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) {
            showErrorDialog("로그인 정보가 없습니다.");
            return;
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "/api/user")
                .delete()
                .addHeader("uid", uid)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> showErrorDialog("회원 탈퇴에 실패했습니다."));
            }

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

    private void finishAccountDeletion() {
        FirebaseAuth.getInstance().signOut();
        SessionManager.getInstance().clear();
        showErrorDialog("회원 탈퇴가 완료되었습니다.");
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    /**
     * uid가 준비되어 있으면 바로, 없으면 /api/user/login을 통해 uid를 받아온 뒤 실행.
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

            RequestBody body = RequestBody.create(json.toString(), JSON);

            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/user/login")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        stopRefreshing();
                        showErrorDialog("서버와 연결할 수 없습니다.");
                    });
                }

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
                            if (!isSuccessResponse(root)) {
                                stopRefreshing();
                                showErrorDialog(getApiMessage(root, "로그인에 실패했습니다."));
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

    private void moveToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void showLoadingState() {
        swipeRefreshLayout.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setRefreshing(true);
        mainContentLayout.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.GONE);
    }

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

    private void showTeamContentState() {
        emptyLayout.setVisibility(View.GONE);
        swipeRefreshLayout.setVisibility(View.VISIBLE);
        mainContentLayout.setVisibility(View.VISIBLE);
        mainFab.setVisibility(View.VISIBLE);
    }

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
        } else {
            boolean expanded = rvComplete.getVisibility() == View.VISIBLE
                    || tvCompleteEmpty.getVisibility() == View.VISIBLE;
            boolean willExpand = !expanded;
            if (completeGoals.isEmpty()) {
                rvComplete.setVisibility(View.GONE);
                tvCompleteEmpty.setVisibility(willExpand ? View.VISIBLE : View.GONE);
            } else {
                tvCompleteEmpty.setVisibility(View.GONE);
                rvComplete.setVisibility(willExpand ? View.VISIBLE : View.GONE);
            }
            tvCompleteToggle.setText(willExpand ? "▽" : "△");
        }
    }

    private void updateGoalsSectionUi() {
        if (progressGoals.isEmpty()) {
            rvProgress.setVisibility(View.GONE);
            tvProgressEmpty.setVisibility(View.VISIBLE);
        } else {
            rvProgress.setVisibility(View.VISIBLE);
            tvProgressEmpty.setVisibility(View.GONE);
            updateNestedRecyclerViewHeight(rvProgress);
        }

        if (completeGoals.isEmpty()) {
            rvComplete.setVisibility(View.GONE);
            tvCompleteEmpty.setVisibility(View.VISIBLE);
        } else {
            rvComplete.setVisibility(View.VISIBLE);
            tvCompleteEmpty.setVisibility(View.GONE);
            updateNestedRecyclerViewHeight(rvComplete);
        }

        tvProgressToggle.setText(rvProgress.getVisibility() == View.VISIBLE
                || tvProgressEmpty.getVisibility() == View.VISIBLE ? "▽" : "△");
        tvCompleteToggle.setText(rvComplete.getVisibility() == View.VISIBLE
                || tvCompleteEmpty.getVisibility() == View.VISIBLE ? "▽" : "△");
    }

    @SuppressWarnings("unchecked")
    private void updateNestedRecyclerViewHeight(RecyclerView recyclerView) {
        RecyclerView.Adapter adapter = recyclerView.getAdapter();
        if (adapter == null || adapter.getItemCount() == 0) return;

        int width = recyclerView.getWidth();
        if (width <= 0) {
            recyclerView.post(() -> updateNestedRecyclerViewHeight(recyclerView));
            return;
        }

        int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int height = 0;
        for (int i = 0; i < adapter.getItemCount(); i++) {
            int viewType = adapter.getItemViewType(i);
            RecyclerView.ViewHolder holder = adapter.createViewHolder(recyclerView, viewType);
            adapter.onBindViewHolder(holder, i);
            holder.itemView.measure(
                    widthSpec,
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            height += holder.itemView.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
        params.height = height;
        recyclerView.setLayoutParams(params);
    }

    private void stopRefreshing() {
        swipeRefreshLayout.setRefreshing(false);
    }

    private void markRefreshStarted() {
        refreshInProgress = true;
        swipeRefreshLayout.setRefreshing(true);
    }

    private void beginRefreshSections(int count) {
        pendingRefreshSections = count;
    }

    private void finishRefreshSection() {
        if (!refreshInProgress) return;
        pendingRefreshSections--;
        if (pendingRefreshSections <= 0) {
            refreshInProgress = false;
            stopRefreshing();
            loadPendingInviteCount();
        }
    }

    private void cancelRefresh() {
        refreshInProgress = false;
        pendingRefreshSections = 0;
        stopRefreshing();
    }

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
        tvCompleteToggle.setText("▽");

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

    private void showErrorDialog(String message) {
        CommonDialog dialog = new CommonDialog(this, message, "확인");
        dialog.setOnConfirmListener(v -> dialog.dismiss());
        dialog.show();
    }

    private boolean isSuccessResponse(JSONObject root) {
        String code = root.optString("responseCode", "");
        return code.endsWith("_ok");
    }

    private String getApiMessage(JSONObject root, String fallback) {
        String message = root.optString("message", "");
        return message.isEmpty() ? fallback : message;
    }

    /**
     * 초대 목록 바텀시트 열기
     */
    private void openInviteBottomSheet() {
        ensureUidReady(this::showInviteBottomSheetInternal);
    }

    private void showInviteBottomSheetInternal() {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.main_bottom_sheet_invite, null);
        dialog.setContentView(view);

        RecyclerView rvInvite = view.findViewById(R.id.invite_recycler);
        TextView inviteEmptyTv = view.findViewById(R.id.invite_empty_text);
        rvInvite.setLayoutManager(new LinearLayoutManager(this));

        List<InviteItem> inviteList = new ArrayList<>();
        // 익명 리스너 내부에서 adapter 참조가 필요해서, 초기화 순서 문제를 피하기 위한 홀더 사용
        InviteAdapter[] adapterHolder = new InviteAdapter[1];
        InviteAdapter adapter = new InviteAdapter(inviteList, new InviteAdapter.InviteListener() {
            @Override
            public void onAccept(InviteItem item) {
                handleInviteAction(item.id, true, () -> {
                    inviteList.remove(item);
                    if (adapterHolder[0] != null) adapterHolder[0].notifyDataSetChanged();
                    updateNotificationBadge(inviteList.size());
                    loadTeamsAndGoals();
                });
            }

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
                .url(BASE_URL + "/api/team/invite")
                .get()
                .addHeader("uid", uid)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> showErrorDialog("초대 목록을 불러오지 못했습니다."));
            }

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
                        if (!isSuccessResponse(root)) {
                            showErrorDialog(getApiMessage(root, "초대 목록을 불러오지 못했습니다."));
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
     * 팀 목록 + 현재 팀 목표/진행률 로딩
     */
    private void loadTeamsAndGoals() {
        markRefreshStarted();

        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) {
            cancelRefresh();
            return;
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "/api/team")
                .get()
                .addHeader("uid", uid)
                .build();

        client.newCall(request).enqueue(new Callback() {
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
                        if (!isSuccessResponse(root)) {
                            showErrorDialog(getApiMessage(root, "팀 목록을 불러오지 못했습니다."));
                            if (teamList.isEmpty()) {
                                showEmptyTeamState();
                            }
                            return;
                        }

                        teamList.clear();
                        teamList.addAll(parseTeams(root.optJSONArray("responseData")));

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

    private List<TeamItem> parseTeams(JSONArray arr) {
        List<TeamItem> result = new ArrayList<>();
        if (arr == null) return result;

        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject obj = arr.getJSONObject(i);
                if (!obj.has("id")) continue;

                long id = obj.getLong("id");
                String teamName = obj.optString("teamName", "이름 없는 팀");
                String endDate = obj.has("endDate") && !obj.isNull("endDate")
                        ? obj.optString("endDate", null)
                        : null;
                long leaderId = obj.optLong("leaderId", -1);

                result.add(new TeamItem(id, teamName, endDate, leaderId));
            } catch (Exception ignored) {
            }
        }
        return result;
    }

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

    private TeamItem findTeamById(Long id) {
        if (id == null) return null;
        for (TeamItem t : teamList) {
            if (t.id == id) return t;
        }
        return null;
    }

    private void applyTeamHeader(TeamItem team) {
        teamSelectorTv.setText(team.teamName);
        tvDeadlineDday.setText(formatDday(team.endDate));
    }

    /**
     * 선택된 팀의 목표 목록을 받아서 진행/완료 UI 갱신
     */
    private void loadGoalsForTeam(long teamId) {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        Request membersRequest = new Request.Builder()
                .url(BASE_URL + "/api/team/" + teamId + "/members")
                .get()
                .addHeader("uid", uid)
                .build();

        Request goalsRequest = new Request.Builder()
                .url(BASE_URL + "/api/goal/team/" + teamId + "/all")
                .get()
                .addHeader("uid", uid)
                .build();

        client.newCall(membersRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showErrorDialog("목표 목록을 불러오지 못했습니다.");
                    finishRefreshSection();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String membersBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        showErrorDialog("멤버 정보를 불러오지 못했습니다.");
                        finishRefreshSection();
                    });
                    return;
                }

                client.newCall(goalsRequest).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> {
                            showErrorDialog("목표 목록을 불러오지 못했습니다.");
                            finishRefreshSection();
                        });
                    }

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
                                if (!isSuccessResponse(goalsRoot)) {
                                    showErrorDialog(getApiMessage(goalsRoot, "목표 목록을 불러오지 못했습니다."));
                                    finishRefreshSection();
                                    return;
                                }

                                List<TeamMemberItem> members = TeamMemberItem.parseList(membersBody);
                                Long myUserId = SessionManager.getInstance().getUserId();
                                myUserColor = "#E8E8E8";
                                for (TeamMemberItem member : members) {
                                    if (myUserId != null && myUserId == member.userId) {
                                        myUserColor = member.userColor;
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

                                int myTotal = progressGoals.size() + completeGoals.size();
                                double percent = teamTotal == 0 ? 0.0 : (teamCompleted * 100.0 / teamTotal);

                                tvMyTaskCount.setText(myTotal + "개");
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

    private String formatDday(String endDate) {
        if (endDate == null || endDate.isEmpty()) return "D-?";
        try {
            LocalDate end = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                end = LocalDate.parse(endDate);
            }
            LocalDate today = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                today = LocalDate.now();
            }
            long days = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                days = ChronoUnit.DAYS.between(today, end);
            }
            return days >= 0 ? ("D-" + days) : ("D+" + Math.abs(days));
        } catch (Exception e) {
            return "D-?";
        }
    }

    private String formatRemainingDays(String endDate) {
        if (endDate == null || endDate.isEmpty()) return "";
        try {
            LocalDate end = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                end = LocalDate.parse(endDate);
            }
            LocalDate today = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                today = LocalDate.now();
            }
            long days = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                days = ChronoUnit.DAYS.between(today, end);
            }
            return days >= 0 ? (days + "일 남음") : (Math.abs(days) + "일 지남");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 팀 목록 바텀시트 열기
     */
    private void openTeamsBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.main_bottom_sheet_teams, null);
        dialog.setContentView(view);

        RecyclerView rvTeams = view.findViewById(R.id.teams_recycler);
        Button createTeamBtn = view.findViewById(R.id.teams_sheet_create_btn);
        rvTeams.setLayoutManager(new LinearLayoutManager(this));

        if (teamList.isEmpty()) {
            rvTeams.setVisibility(View.GONE);
        } else {
            rvTeams.setVisibility(View.VISIBLE);
            rvTeams.setAdapter(new TeamBottomSheetAdapter(teamList, selectedTeam -> {
                SessionManager.getInstance().setCurrentTeamId(selectedTeam.id);
                applyTeamHeader(selectedTeam);
                loadGoalsForTeam(selectedTeam.id);
                loadTeamLogs(selectedTeam.id);
                dialog.dismiss();
            }));
        }

        createTeamBtn.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, CreateTeamActivity.class));
        });

        dialog.show();
    }

    private void openAddGoalDialog() {
        Long teamId = SessionManager.getInstance().getCurrentTeamId();
        if (teamId == null) {
            showErrorDialog("목표를 추가할 팀이 없습니다.\n팀을 먼저 만들거나 선택해 주세요.");
            return;
        }

        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        Request request = new Request.Builder()
                .url(BASE_URL + "/api/team/" + teamId + "/members")
                .get()
                .addHeader("uid", uid)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> showErrorDialog("멤버 정보를 불러오지 못했습니다."));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (!response.isSuccessful()) {
                        showErrorDialog("멤버 정보를 불러오지 못했습니다.");
                        return;
                    }
                    try {
                        List<TeamMemberItem> members = TeamMemberItem.parseList(body);
                        List<TeamMemberItem> eligible = getGoalAddEligibleMembers(members);
                        if (eligible.isEmpty()) {
                            showErrorDialog("목표를 추가할 수 있는 멤버가 없습니다.");
                            return;
                        }

                        AddGoalDialog.show(MainActivity.this, client, teamId, eligible, null,
                                new AddGoalDialog.OnComplete() {
                                    @Override
                                    public void onSuccess() {
                                        loadGoalsForTeam(teamId);
                                        loadTeamLogs(teamId);
                                    }

                                    @Override
                                    public void onError(String message) {
                                        showErrorDialog(message);
                                    }
                                });
                    } catch (Exception e) {
                        showErrorDialog("멤버 정보 처리에 실패했습니다.");
                    }
                });
            }
        });
    }

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

    private void openInviteMemberDialog() {
        Long teamId = SessionManager.getInstance().getCurrentTeamId();
        if (teamId == null) {
            showErrorDialog("초대할 팀이 없습니다.\n팀을 먼저 만들거나 선택해 주세요.");
            return;
        }
        if (!isCurrentTeamLeader()) {
            showErrorDialog("팀장만 멤버를 초대할 수 있습니다.");
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

    private boolean isCurrentTeamLeader() {
        Long myUserId = SessionManager.getInstance().getUserId();
        TeamItem current = findTeamById(SessionManager.getInstance().getCurrentTeamId());
        return myUserId != null && current != null && myUserId == current.leaderId;
    }

    private void loadTeamLogs(long teamId) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/team/log/" + teamId)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    updateTeamLogsUi(new ArrayList<>());
                    finishRefreshSection();
                });
            }

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
                        if (!isSuccessResponse(root)) {
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

    private void updateTeamLogsUi(List<TeamLogItem> parsedLogs) {
        allTeamLogs.clear();
        allTeamLogs.addAll(parsedLogs);
        teamLogsExpanded = false;
        refreshTeamLogsDisplay();
    }

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
        rvTeamLogs.post(() -> updateNestedRecyclerViewHeight(rvTeamLogs));
    }

    private void handleInviteAction(long inviteId, boolean accept, Runnable onSuccess) {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        String path = accept ? "accept" : "reject";
        String url = BASE_URL + "/api/team/invite/" + inviteId + "/" + path;

        RequestBody body = RequestBody.create("{}", JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("uid", uid)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> showErrorDialog("초대 처리에 실패했습니다."));
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful() && onSuccess != null) {
                        onSuccess.run();
                        return;
                    }
                    showErrorDialog(accept ? "초대 수락에 실패했습니다." : "초대 거절에 실패했습니다.");
                });
            }
        });
    }

    // 초대 목록을 위한 간단한 모델
    static class InviteItem {
        long id;
        long teamId;
        String teamName;
        String inviterName;
        String message;

        InviteItem(long id, long teamId, String teamName, String inviterName, String message) {
            this.id = id;
            this.teamId = teamId;
            this.teamName = teamName;
            this.inviterName = inviterName;
            this.message = message;
        }
    }

    // 팀 목록 모델
    static class TeamItem {
        long id;
        String teamName;
        String endDate;
        long leaderId;

        TeamItem(long id, String teamName, String endDate, long leaderId) {
            this.id = id;
            this.teamName = teamName;
            this.endDate = endDate;
            this.leaderId = leaderId;
        }
    }

    // 목표(작업) 모델
    static class GoalItem {
        long id;
        long userId;
        String goalText;
        int status;
        String remark;
        String goalEndDate;

        GoalItem(long id, long userId, String goalText, int status, String remark, String goalEndDate) {
            this.id = id;
            this.userId = userId;
            this.goalText = goalText;
            this.status = status;
            this.remark = remark;
            this.goalEndDate = goalEndDate;
        }
    }

    /**
     * 목표(진행/완료) RecyclerView Adapter
     */
    private class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.GoalVH> {
        private final List<GoalItem> goals;

        GoalAdapter(List<GoalItem> goals) {
            this.goals = goals;
        }

        @Override
        public GoalVH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.main_item_my_task, parent, false);
            return new GoalVH(v);
        }

        @Override
        public void onBindViewHolder(GoalVH holder, int position) {
            GoalItem item = goals.get(position);
            holder.nameTv.setText(item.goalText);
            holder.deadlineTv.setText(formatRemainingDays(item.goalEndDate));
            if (isOverdue(item.goalEndDate)) {
                holder.deadlineTv.setTextColor(0xFFE53935);
            } else {
                holder.deadlineTv.setTextColor(0xFFAAAAAA);
            }

            int baseColor = ColorUtil.parseColorOrDefault(myUserColor, 0xFFE8E8E8);
            ColorUtil.applyRoundedBackground(holder.itemView, myUserColor, 0.32f, 10);
        }

        private boolean isOverdue(String endDate) {
            if (endDate == null || endDate.isEmpty()) return false;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    LocalDate end = LocalDate.parse(endDate);
                    return end.isBefore(LocalDate.now());
                }
            } catch (Exception ignored) {
            }
            return false;
        }

        @Override
        public int getItemCount() {
            return goals.size();
        }

        class GoalVH extends RecyclerView.ViewHolder {
            TextView nameTv;
            TextView deadlineTv;

            GoalVH(View itemView) {
                super(itemView);
                nameTv = itemView.findViewById(R.id.item_task_name);
                deadlineTv = itemView.findViewById(R.id.item_task_deadline);
            }
        }
    }

    interface OnTeamSelected {
        void onSelected(TeamItem team);
    }

    /**
     * 팀 목록 바텀시트 Adapter
     */
    private static class TeamBottomSheetAdapter extends RecyclerView.Adapter<TeamBottomSheetAdapter.TeamVH> {
        private final List<TeamItem> items;
        private final OnTeamSelected listener;

        TeamBottomSheetAdapter(List<TeamItem> items, OnTeamSelected listener) {
            this.items = items;
            this.listener = listener;
        }

        @Override
        public TeamVH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.main_bottom_sheet_team_item, parent, false);
            return new TeamVH(v);
        }

        @Override
        public void onBindViewHolder(TeamVH holder, int position) {
            TeamItem item = items.get(position);
            holder.nameTv.setText(item.teamName);
            holder.deadlineTv.setText(formatDday(item.endDate));

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onSelected(item);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class TeamVH extends RecyclerView.ViewHolder {
            TextView nameTv;
            TextView deadlineTv;

            TeamVH(View itemView) {
                super(itemView);
                nameTv = itemView.findViewById(R.id.item_team_name);
                deadlineTv = itemView.findViewById(R.id.item_team_deadline);
            }
        }

        private static String formatDday(String endDate) {
            if (endDate == null || endDate.isEmpty()) return "D-?";
            try {
                LocalDate end = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    end = LocalDate.parse(endDate);
                }
                LocalDate today = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    today = LocalDate.now();
                }
                long days = 0;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    days = ChronoUnit.DAYS.between(today, end);
                }
                return days >= 0 ? ("D-" + days) : ("D+" + Math.abs(days));
            } catch (Exception e) {
                return "D-?";
            }
        }
    }
}
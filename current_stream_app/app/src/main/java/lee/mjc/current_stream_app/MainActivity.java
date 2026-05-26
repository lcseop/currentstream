package lee.mjc.current_stream_app;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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

    Button logoutBtn, noTeamLogoutBtn, noTeamCreateBtn;
    ImageButton notificationBtn;
    TextView teamSelectorTv;

    private final OkHttpClient client = new OkHttpClient();

    private SwipeRefreshLayout swipeRefreshLayout;
    private View mainContentLayout;
    private View emptyLayout;

    private TextView tvDeadlineDday;
    private TextView tvTotalPercent;
    private ProgressBar pbAll;

    private TextView tvMyTaskCount;
    private TextView tvMyTaskTag;
    private TextView tvProgressToggle;
    private TextView tvCompleteToggle;
    private RecyclerView rvProgress;
    private RecyclerView rvComplete;

    private final List<TeamItem> teamList = new ArrayList<>();
    private final List<GoalItem> progressGoals = new ArrayList<>();
    private final List<GoalItem> completeGoals = new ArrayList<>();

    private GoalAdapter progressAdapter;
    private GoalAdapter completeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logoutBtn = findViewById(R.id.main_team_logout);
        noTeamLogoutBtn = findViewById(R.id.main_no_team_logout);
        noTeamCreateBtn = findViewById(R.id.main_no_team_create);
        notificationBtn = findViewById(R.id.main_notification);
        teamSelectorTv = findViewById(R.id.main_team_selector);

        swipeRefreshLayout = findViewById(R.id.main_swipe_refresh);
        mainContentLayout = findViewById(R.id.main_content_layout);
        emptyLayout = findViewById(R.id.empty_layout);

        tvDeadlineDday = findViewById(R.id.main_team_deadline);
        tvTotalPercent = findViewById(R.id.main_team_total_percent);
        pbAll = findViewById(R.id.main_team_all_progress);

        tvMyTaskCount = findViewById(R.id.main_my_task_count);
        tvMyTaskTag = findViewById(R.id.main_my_task_tag);
        tvProgressToggle = findViewById(R.id.tv_progress_toggle);
        tvCompleteToggle = findViewById(R.id.tv_complete_toggle);

        rvProgress = findViewById(R.id.rv_progress);
        rvComplete = findViewById(R.id.rv_complete);

        rvProgress.setLayoutManager(new LinearLayoutManager(this));
        rvComplete.setLayoutManager(new LinearLayoutManager(this));

        progressAdapter = new GoalAdapter(progressGoals);
        completeAdapter = new GoalAdapter(completeGoals);
        rvProgress.setAdapter(progressAdapter);
        rvComplete.setAdapter(completeAdapter);

        // 초기 상태: rv_complete는 XML에서 gone
        if (rvComplete.getVisibility() != View.VISIBLE) {
            tvCompleteToggle.setText("△");
        }

        logoutBtn.setOnClickListener((v) -> logoutBtnClick());
        noTeamLogoutBtn.setOnClickListener((v) -> logoutBtnClick());
        noTeamCreateBtn.setOnClickListener(v ->
                startActivity(new Intent(this, CreateTeamActivity.class))
        );

        teamSelectorTv.setOnClickListener(v -> {
            if (teamList.isEmpty()) {
                showErrorDialog("아직 속한 팀이 없습니다.\n팀을 만들거나 초대를 수락해 주세요.");
                return;
            }
            openTeamsBottomSheet();
        });
        notificationBtn.setOnClickListener(v -> openInviteBottomSheet());

        tvProgressToggle.setOnClickListener(v -> {
            boolean willExpand = rvProgress.getVisibility() != View.VISIBLE;
            rvProgress.setVisibility(willExpand ? View.VISIBLE : View.GONE);
            tvProgressToggle.setText(willExpand ? "▽" : "△");
        });

        tvCompleteToggle.setOnClickListener(v -> {
            boolean willExpand = rvComplete.getVisibility() != View.VISIBLE;
            rvComplete.setVisibility(willExpand ? View.VISIBLE : View.GONE);
            tvCompleteToggle.setText(willExpand ? "▽" : "△");
        });

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
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 팀 만들기 화면 등에서 돌아왔을 때 목록 갱신
        if (SessionManager.getInstance().getUid() != null) {
            loadTeamsAndGoals();
        }
    }

    private void logoutBtnClick() {
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
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
        stopRefreshing();
        swipeRefreshLayout.setVisibility(View.GONE);
        mainContentLayout.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.VISIBLE);

        SessionManager.getInstance().setCurrentTeamId(null);
        teamSelectorTv.setText("팀 없음");
        resetGoalsUi();
    }

    private void showTeamContentState() {
        stopRefreshing();
        emptyLayout.setVisibility(View.GONE);
        swipeRefreshLayout.setVisibility(View.VISIBLE);
        mainContentLayout.setVisibility(View.VISIBLE);
    }

    private void stopRefreshing() {
        swipeRefreshLayout.setRefreshing(false);
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
                    // 팀 참여가 바뀔 수 있으므로 팀/목표도 다시 갱신
                    loadTeamsAndGoals();
                });
            }

            @Override
            public void onReject(InviteItem item) {
                handleInviteAction(item.id, false, () -> {
                    inviteList.remove(item);
                    if (adapterHolder[0] != null) adapterHolder[0].notifyDataSetChanged();
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
                        // 초대가 없으면 에러 없이 빈 목록만 표시
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
        swipeRefreshLayout.setRefreshing(true);

        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) {
            stopRefreshing();
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
                    stopRefreshing();
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
                    stopRefreshing();

                    try {
                        if (!response.isSuccessful()) {
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
                        loadGoalsForTeam(current.id);
                    } catch (Exception e) {
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

                result.add(new TeamItem(id, teamName, endDate));
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

        String url = BASE_URL + "/api/goal/team/" + teamId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("uid", uid)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> showErrorDialog("목표 목록을 불러오지 못했습니다."));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (!response.isSuccessful()) {
                        showErrorDialog("목표 목록 요청에 실패했습니다.");
                        return;
                    }

                    try {
                        JSONObject root = new JSONObject(resBody);
                        if (!isSuccessResponse(root)) {
                            showErrorDialog(getApiMessage(root, "목표 목록을 불러오지 못했습니다."));
                            return;
                        }

                        progressGoals.clear();
                        completeGoals.clear();

                        List<GoalItem> parsedGoals = parseGoals(root.optJSONArray("responseData"));
                        for (GoalItem item : parsedGoals) {
                            if (item.status == 0) progressGoals.add(item);
                            else if (item.status == 1) completeGoals.add(item);
                        }

                        int total = progressGoals.size() + completeGoals.size();
                        int completed = completeGoals.size();
                        double percent = total == 0 ? 0.0 : (completed * 100.0 / total);

                        tvMyTaskCount.setText(total + "개");
                        tvTotalPercent.setText(String.format("%.1f%%", percent));
                        pbAll.setProgress((int) Math.round(percent));

                        progressAdapter.notifyDataSetChanged();
                        completeAdapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        showErrorDialog("목표 목록 응답을 처리하지 못했습니다.");
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
                String remark = obj.optString("remark", "");
                String goalEndDate = obj.has("goalEndDate") && !obj.isNull("goalEndDate")
                        ? obj.optString("goalEndDate", null)
                        : null;

                result.add(new GoalItem(id, goalText, status, remark, goalEndDate));
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
        if (teamList.isEmpty()) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.main_bottom_sheet_teams, null);
        dialog.setContentView(view);

        RecyclerView rvTeams = view.findViewById(R.id.teams_recycler);
        rvTeams.setLayoutManager(new LinearLayoutManager(this));

        rvTeams.setAdapter(new TeamBottomSheetAdapter(teamList, selectedTeam -> {
            SessionManager.getInstance().setCurrentTeamId(selectedTeam.id);
            applyTeamHeader(selectedTeam);
            loadGoalsForTeam(selectedTeam.id);
            dialog.dismiss();
        }));

        dialog.show();
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

        TeamItem(long id, String teamName, String endDate) {
            this.id = id;
            this.teamName = teamName;
            this.endDate = endDate;
        }
    }

    // 목표(작업) 모델
    static class GoalItem {
        long id;
        String goalText;
        int status;
        String remark;
        String goalEndDate;

        GoalItem(long id, String goalText, int status, String remark, String goalEndDate) {
            this.id = id;
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
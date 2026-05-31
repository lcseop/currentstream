package lee.mjc.current_stream_app;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TeamsActivity extends AppCompatActivity implements TeamMemberAdapter.Listener {

    public static final String EXTRA_TEAM_ID = "teamId";

    private static final String BASE_URL = "http://10.0.2.2:8080";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private ImageButton backBtn;
    private TextView titleTv;
    private TextView deadlineTv;
    private TextView percentTv;
    private ProgressBar progressBar;
    private RecyclerView memberList;
    private Button bottomBtn;
    private FloatingActionButton teamsFab;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FabSpeedDialMenu teamsFabMenu;

    private final OkHttpClient client = new OkHttpClient();
    private final List<TeamMemberItem> members = new ArrayList<>();
    private TeamMemberAdapter memberAdapter;

    private long teamId;
    private String teamName = "";
    private String teamEndDate = "";
    private long leaderId;
    private boolean isLeader;

    private Long getMyUserId() {
        return SessionManager.getInstance().getUserId();
    }

    private void refreshLeaderState() {
        Long myUserId = getMyUserId();
        isLeader = myUserId != null && myUserId == leaderId;
        bottomBtn.setText(isLeader ? "팀 삭제" : "팀 나가기");
    }

    private void syncMyUserIdFromMembers(List<TeamMemberItem> parsedMembers) {
        if (getMyUserId() != null) return;
        String myTag = SessionManager.getInstance().getTag();
        if (myTag == null || myTag.isEmpty()) return;
        for (TeamMemberItem member : parsedMembers) {
            if (myTag.equals(member.tag)) {
                SessionManager.getInstance().setUserId(member.userId);
                break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teams);

        backBtn = findViewById(R.id.teams_header_back);
        titleTv = findViewById(R.id.teams_header_title);
        deadlineTv = findViewById(R.id.teams_deadline);
        percentTv = findViewById(R.id.teams_total_percent);
        progressBar = findViewById(R.id.teams_all_progress);
        memberList = findViewById(R.id.main_team_list);
        bottomBtn = findViewById(R.id.teams_team_delete);
        teamsFab = findViewById(R.id.teams_float_button);
        swipeRefreshLayout = findViewById(R.id.teams_swipe_refresh);

        memberList.setLayoutManager(new LinearLayoutManager(this));
        memberAdapter = new TeamMemberAdapter(members, false, getMyUserId(), this);
        memberList.setAdapter(memberAdapter);

        teamId = getIntent().getLongExtra(EXTRA_TEAM_ID, -1);
        if (teamId <= 0) {
            Long saved = SessionManager.getInstance().getCurrentTeamId();
            teamId = saved != null ? saved : -1;
        }

        if (teamId <= 0) {
            showErrorDialog("팀 정보를 불러올 수 없습니다.", () -> finish());
            return;
        }

        backBtn.setOnClickListener(v -> finish());
        titleTv.setOnClickListener(v -> showEditTeamDialog());
        bottomBtn.setOnClickListener(v -> onBottomButtonClick());

        teamsFabMenu = new FabSpeedDialMenu(this, teamsFab, Arrays.asList(
                new FabSpeedDialMenu.Item("멤버 초대하기", R.drawable.pic_menu_invite, this::openInviteMemberDialog),
                new FabSpeedDialMenu.Item("목표 추가하기", R.drawable.pic_menu_add_goal, () -> showAddGoalDialog(null))
        ));

        swipeRefreshLayout.setOnRefreshListener(() -> loadTeamData(true));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (teamsFabMenu != null) {
            teamsFabMenu.collapse();
        }
    }

    private void openInviteMemberDialog() {
        if (!isLeader) {
            showErrorDialog("팀장만 멤버를 초대할 수 있습니다.", null);
            return;
        }

        InviteMemberDialog.show(this, teamId, (success, message) -> {
            if (success) {
                loadMembersAndGoals();
            }
            if (message != null && !message.isEmpty()) {
                showErrorDialog(message, null);
            }
        });
    }

    private List<TeamMemberItem> getGoalAddEligibleMembers() {
        List<TeamMemberItem> eligible = new ArrayList<>();
        Long myUserId = getMyUserId();
        for (TeamMemberItem member : members) {
            if (isLeader || (myUserId != null && myUserId == member.userId)) {
                eligible.add(member);
            }
        }
        return eligible;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (teamId > 0) {
            loadTeamData();
        }
    }

    private void loadTeamData() {
        loadTeamData(false);
    }

    private void loadTeamData(boolean fromPullRefresh) {
        if (fromPullRefresh) {
            swipeRefreshLayout.setRefreshing(true);
        }

        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) {
            stopRefreshing();
            showErrorDialog("로그인 정보가 없습니다.", () -> finish());
            return;
        }

        Request teamRequest = new Request.Builder()
                .url(BASE_URL + "/api/team")
                .addHeader("uid", uid)
                .build();

        client.newCall(teamRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    stopRefreshing();
                    showErrorDialog("팀 정보를 불러오지 못했습니다.", null);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (!response.isSuccessful()) {
                        stopRefreshing();
                        showErrorDialog("팀 정보를 불러오지 못했습니다.", null);
                        return;
                    }
                    try {
                        JSONObject root = new JSONObject(body);
                        JSONArray arr = root.optJSONArray("responseData");
                        if (arr == null) {
                            stopRefreshing();
                            showErrorDialog("팀 정보가 없습니다.", () -> finish());
                            return;
                        }

                        boolean found = false;
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            if (obj.getLong("id") == teamId) {
                                teamName = obj.optString("teamName", "");
                                teamEndDate = obj.optString("endDate", "");
                                leaderId = obj.optLong("leaderId", -1);
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            stopRefreshing();
                            showErrorDialog("해당 팀에 속해 있지 않습니다.", () -> finish());
                            return;
                        }

                        Long myUserId = getMyUserId();
                        isLeader = myUserId != null && myUserId == leaderId;
                        applyTeamHeader();
                        loadMembersAndGoals();
                    } catch (Exception e) {
                        stopRefreshing();
                        showErrorDialog("팀 정보 처리에 실패했습니다.", null);
                    }
                });
            }
        });
    }

    private void applyTeamHeader() {
        titleTv.setText(teamName);
        deadlineTv.setText(formatKoreanDate(teamEndDate));
        refreshLeaderState();
        memberAdapter = new TeamMemberAdapter(members, isLeader, getMyUserId(), this);
        memberList.setAdapter(memberAdapter);
    }

    private void loadMembersAndGoals() {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) {
            stopRefreshing();
            return;
        }

        Request membersRequest = new Request.Builder()
                .url(BASE_URL + "/api/team/" + teamId + "/members")
                .addHeader("uid", uid)
                .build();

        Request goalsRequest = new Request.Builder()
                .url(BASE_URL + "/api/goal/team/" + teamId + "/all")
                .addHeader("uid", uid)
                .build();

        client.newCall(membersRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    stopRefreshing();
                    showErrorDialog("멤버 목록을 불러오지 못했습니다.", null);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        stopRefreshing();
                        showErrorDialog("멤버 목록을 불러오지 못했습니다.", null);
                    });
                    return;
                }

                client.newCall(goalsRequest).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        runOnUiThread(() -> {
                            stopRefreshing();
                            showErrorDialog("목표 목록을 불러오지 못했습니다.", null);
                        });
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response goalsResponse) throws IOException {
                        String goalsBody = goalsResponse.body() != null ? goalsResponse.body().string() : "";
                        runOnUiThread(() -> {
                            try {
                                if (!goalsResponse.isSuccessful()) {
                                    showErrorDialog("목표 목록을 불러오지 못했습니다.", null);
                                    return;
                                }
                                List<TeamMemberItem> parsedMembers = parseMembers(body);
                                List<TeamGoalItem> parsedGoals = parseGoals(goalsBody);
                                mergeMembersAndGoals(parsedMembers, parsedGoals);
                            } catch (Exception e) {
                                showErrorDialog("데이터 처리에 실패했습니다.", null);
                            } finally {
                                stopRefreshing();
                            }
                        });
                    }
                });
            }
        });
    }

    private List<TeamMemberItem> parseMembers(String body) throws Exception {
        return TeamMemberItem.parseList(body);
    }

    private List<TeamGoalItem> parseGoals(String body) throws Exception {
        List<TeamGoalItem> result = new ArrayList<>();
        JSONObject root = new JSONObject(body);
        JSONArray arr = root.optJSONArray("responseData");
        if (arr == null) return result;

        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            String endDate = obj.has("goalEndDate") && !obj.isNull("goalEndDate")
                    ? obj.optString("goalEndDate", null)
                    : null;
            result.add(new TeamGoalItem(
                    obj.getLong("id"),
                    obj.getLong("userId"),
                    obj.optString("goalText", ""),
                    obj.optString("remark", ""),
                    obj.optInt("status", 0),
                    endDate
            ));
        }
        return result;
    }

    private void mergeMembersAndGoals(List<TeamMemberItem> parsedMembers, List<TeamGoalItem> parsedGoals) {
        Map<Long, TeamMemberItem> preserved = new HashMap<>();
        for (TeamMemberItem m : members) {
            preserved.put(m.userId, m);
        }

        members.clear();
        for (TeamMemberItem member : parsedMembers) {
            TeamMemberItem prev = preserved.get(member.userId);
            if (prev != null) {
                member.ongoingExpanded = prev.ongoingExpanded;
                member.completedExpanded = prev.completedExpanded;
            }
            members.add(member);
        }

        for (TeamGoalItem goal : parsedGoals) {
            for (TeamMemberItem member : members) {
                if (member.userId == goal.userId) {
                    if (goal.status == 1) {
                        member.completedGoals.add(goal);
                    } else {
                        member.ongoingGoals.add(goal);
                    }
                    break;
                }
            }
        }

        members.sort((a, b) -> {
            if (a.leader == b.leader) return a.name.compareTo(b.name);
            return a.leader ? -1 : 1;
        });

        syncMyUserIdFromMembers(parsedMembers);
        refreshLeaderState();
        memberAdapter = new TeamMemberAdapter(members, isLeader, getMyUserId(), this);
        memberList.setAdapter(memberAdapter);

        updateTeamProgress(parsedGoals);
        memberAdapter.notifyDataSetChanged();
    }

    private void updateTeamProgress(List<TeamGoalItem> goals) {
        int total = goals.size();
        int completed = 0;
        for (TeamGoalItem g : goals) {
            if (g.status == 1) completed++;
        }

        int percent = total == 0 ? 0 : (int) Math.round(completed * 100.0 / total);
        percentTv.setText(String.format(Locale.KOREA, "%.1f%%", total == 0 ? 0.0 : completed * 100.0 / total));
        progressBar.setProgress(percent);
    }

    private void showEditTeamDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_teams_edit);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText nameEdit = dialog.findViewById(R.id.dialog_teams_edit_name);
        EditText dateEdit = dialog.findViewById(R.id.dialog_teams_edit_date);
        TextView nameWarn = dialog.findViewById(R.id.dialog_teams_edit_name_warn);
        TextView dateWarn = dialog.findViewById(R.id.dialog_teams_edit_date_warn);
        MaterialButton cancelBtn = dialog.findViewById(R.id.dialog_teams_edit_cancel);
        MaterialButton saveBtn = dialog.findViewById(R.id.dialog_teams_edit_save);

        nameEdit.setText(teamName);
        dateEdit.setText(teamEndDate);

        if (!isLeader) {
            nameEdit.setEnabled(false);
            nameEdit.setFocusable(false);
            dateEdit.setEnabled(false);
            dateEdit.setClickable(false);
            saveBtn.setVisibility(View.GONE);
        } else {
            final boolean[] check = {true, teamEndDate != null && !teamEndDate.isEmpty()};

            nameEdit.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() < 2 || s.length() > 100) {
                        nameWarn.setText("팀 이름은 2~100자로 입력해주세요.");
                        nameWarn.setVisibility(View.VISIBLE);
                        check[0] = false;
                    } else {
                        nameWarn.setVisibility(View.INVISIBLE);
                        check[0] = true;
                    }
                    saveBtn.setEnabled(check[0] && check[1]);
                }
            });

            dateEdit.setOnClickListener(v -> showDatePicker(dateEdit, dateWarn, selected -> {
                check[1] = selected != null;
                saveBtn.setEnabled(check[0] && check[1]);
            }));

            saveBtn.setOnClickListener(v -> {
                if (!check[0] || !check[1]) return;
                updateTeam(nameEdit.getText().toString().trim(), dateEdit.getText().toString().trim(), dialog);
            });
        }

        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void updateTeam(String name, String endDate, Dialog dialog) {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        String json = "{\"name\":\"" + escapeJson(name) + "\",\"endDate\":\"" + escapeJson(endDate) + "\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/team/" + teamId)
                .patch(body)
                .addHeader("uid", uid)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> showErrorDialog("팀 정보 수정에 실패했습니다.", null));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        dialog.dismiss();
                        loadTeamData();
                    } else {
                        showErrorDialog("팀 정보 수정에 실패했습니다.", null);
                    }
                });
            }
        });
    }

    @Override
    public void onAddGoal(TeamMemberItem member) {
        showAddGoalDialog(member);
    }

    private void showAddGoalDialog(TeamMemberItem preselected) {
        List<TeamMemberItem> eligible = getGoalAddEligibleMembers();
        if (eligible.isEmpty()) {
            showErrorDialog("목표를 추가할 수 있는 멤버가 없습니다.", null);
            return;
        }

        AddGoalDialog.show(this, client, teamId, eligible, preselected, new AddGoalDialog.OnComplete() {
            @Override
            public void onSuccess() {
                loadMembersAndGoals();
            }

            @Override
            public void onError(String message) {
                showErrorDialog(message, null);
            }
        });
    }

    @Override
    public void onGoalClick(TeamGoalItem goal, TeamMemberItem member) {
        showGoalDetailDialog(goal);
    }

    private void showGoalDetailDialog(TeamGoalItem goal) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_teams_goal_detail);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView titleTv = dialog.findViewById(R.id.dialog_teams_goal_title);
        TextView statusTv = dialog.findViewById(R.id.dialog_teams_goal_status);
        TextView deadlineTvLocal = dialog.findViewById(R.id.dialog_teams_goal_deadline);
        TextView remarkTv = dialog.findViewById(R.id.dialog_teams_goal_remark);
        MaterialButton completeBtn = dialog.findViewById(R.id.dialog_teams_goal_complete);
        MaterialButton confirmBtn = dialog.findViewById(R.id.dialog_teams_goal_confirm);

        titleTv.setText(goal.goalText);
        statusTv.setText(goal.status == 1 ? "상태: 완료" : "상태: 진행 중");
        deadlineTvLocal.setText("남은 시간: " + TeamMemberAdapter.formatRemainingDays(goal.goalEndDate));
        remarkTv.setText(goal.remark == null || goal.remark.isEmpty() ? "없음" : goal.remark);

        Long myUserId = SessionManager.getInstance().getUserId();
        boolean canComplete = goal.status == 0
                && (isLeader || (myUserId != null && myUserId == goal.userId));

        if (canComplete) {
            completeBtn.setVisibility(View.VISIBLE);
            completeBtn.setOnClickListener(v ->
                    updateGoalStatus(goal.id, 1, dialog)
            );
        }

        confirmBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void updateGoalStatus(long goalId, int status, Dialog dialog) {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        String json = "{\"status\":\"" + status + "\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/goal/" + goalId + "/status")
                .patch(body)
                .addHeader("uid", uid)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> showErrorDialog("상태 변경에 실패했습니다.", null));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        dialog.dismiss();
                        loadMembersAndGoals();
                    } else {
                        showErrorDialog("상태 변경에 실패했습니다.", null);
                    }
                });
            }
        });
    }

    @Override
    public void onDeleteGoal(TeamGoalItem goal) {
        if (!isLeader) return;

        new AlertDialog.Builder(this)
                .setMessage("이 목표를 삭제하시겠습니까?")
                .setPositiveButton("삭제", (d, w) -> deleteGoal(goal.id))
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteGoal(long goalId) {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        Request request = new Request.Builder()
                .url(BASE_URL + "/api/goal/" + goalId)
                .delete()
                .addHeader("uid", uid)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> showErrorDialog("목표 삭제에 실패했습니다.", null));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        loadMembersAndGoals();
                    } else {
                        showErrorDialog("목표 삭제에 실패했습니다.", null);
                    }
                });
            }
        });
    }

    private void onBottomButtonClick() {
        if (isLeader) {
            new AlertDialog.Builder(this)
                    .setMessage("팀을 삭제하시겠습니까?\n모든 목표와 멤버 정보가 삭제됩니다.")
                    .setPositiveButton("삭제", (d, w) -> deleteTeam())
                    .setNegativeButton("취소", null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setMessage("팀에서 나가시겠습니까?")
                    .setPositiveButton("나가기", (d, w) -> leaveTeam())
                    .setNegativeButton("취소", null)
                    .show();
        }
    }

    private void leaveTeam() {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        Request request = new Request.Builder()
                .url(BASE_URL + "/api/team/" + teamId + "/leave")
                .delete()
                .addHeader("uid", uid)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> showErrorDialog("팀 나가기에 실패했습니다.", null));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        SessionManager.getInstance().setCurrentTeamId(null);
                        finish();
                    } else {
                        String message = parseApiErrorMessage(body, "팀 나가기에 실패했습니다.");
                        if ("Leader cannot leave".equals(message)) {
                            message = "팀장은 팀 나가기를 할 수 없습니다.\n팀 삭제를 이용해 주세요.";
                            refreshLeaderState();
                        }
                        showErrorDialog(message, null);
                    }
                });
            }
        });
    }

    private void deleteTeam() {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        Request request = new Request.Builder()
                .url(BASE_URL + "/api/team/" + teamId)
                .delete()
                .addHeader("uid", uid)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> showErrorDialog("팀 삭제에 실패했습니다.", null));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        SessionManager.getInstance().setCurrentTeamId(null);
                        finish();
                    } else {
                        showErrorDialog("팀 삭제에 실패했습니다.", null);
                    }
                });
            }
        });
    }

    private String parseApiErrorMessage(String body, String fallback) {
        try {
            JSONObject root = new JSONObject(body);
            String data = root.optString("responseData", "");
            if (!data.isEmpty() && !"null".equals(data)) return data;
            String message = root.optString("message", "");
            if (!message.isEmpty()) return message;
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private void showDatePicker(EditText dateEdit, TextView warnTv, DateSelectedListener listener) {
        LocalDate minDate = LocalDate.now().plusDays(7);
        Calendar calendar = Calendar.getInstance();
        calendar.set(minDate.getYear(), minDate.getMonthValue() - 1, minDate.getDayOfMonth(), 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        DatePickerDialog picker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    LocalDate selected = LocalDate.of(year, month + 1, dayOfMonth);
                    if (selected.isBefore(LocalDate.now().plusDays(7))) {
                        warnTv.setText("날짜는 7일 후로 설정해주세요.");
                        warnTv.setVisibility(View.VISIBLE);
                        listener.onSelected(null);
                    } else {
                        warnTv.setVisibility(View.INVISIBLE);
                        dateEdit.setText(selected.toString());
                        listener.onSelected(selected);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        picker.getDatePicker().setMinDate(calendar.getTimeInMillis());
        picker.show();
    }

    private String formatKoreanDate(String endDate) {
        if (endDate == null || endDate.isEmpty()) return "";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDate date = LocalDate.parse(endDate);
                return date.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREA));
            }
        } catch (Exception ignored) {
        }
        return endDate;
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void stopRefreshing() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void showErrorDialog(String message, Runnable onConfirm) {
        CommonDialog dialog = new CommonDialog(this, message, "확인");
        dialog.setOnConfirmListener(v -> {
            dialog.dismiss();
            if (onConfirm != null) onConfirm.run();
        });
        dialog.show();
    }

    private interface DateSelectedListener {
        void onSelected(LocalDate date);
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}

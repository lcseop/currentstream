package lee.mjc.current_stream_app;

import android.content.Intent;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.util.TypedValue;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.TextViewCompat;
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
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

// 팀 상세 화면 (멤버별 목표, FAB, 팀 설정)
public class TeamsActivity extends AppCompatActivity implements TeamMemberAdapter.Listener {

    public static final String EXTRA_TEAM_ID = "teamId";

    private ImageButton backBtn;
    private ImageButton editBtn;
    private View titleHost;
    private TextView titleTv;
    private TextView deadlineTv;
    private TextView percentTv;
    private ProgressBar progressBar;
    private RecyclerView memberList;
    private Button bottomBtn;
    private FloatingActionButton teamsFab;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FabSpeedDialMenu teamsFabMenu;

    private final List<TeamMemberItem> members = new ArrayList<>();
    private TeamMemberAdapter memberAdapter;

    private long teamId;
    private String teamName = "";
    private String teamEndDate = "";
    private long leaderId;
    private boolean isLeader;

    // 세션에서 내 유저 ID 가져오기
    private Long getMyUserId() {
        return SessionManager.getInstance().getUserId();
    }

    // 팀장 여부에 따라 하단 버튼·수정 버튼 갱신
    private void refreshLeaderState() {
        Long myUserId = getMyUserId();
        isLeader = myUserId != null && myUserId == leaderId;
        bottomBtn.setText(isLeader ? "팀 삭제" : "팀 나가기");
        if (editBtn != null) {
            editBtn.setVisibility(isLeader ? View.VISIBLE : View.INVISIBLE);
            editBtn.setEnabled(isLeader);
            editBtn.setClickable(isLeader);
        }
        updateTitleMaxWidth();
    }

    // 바텀시트로 다른 팀 고르기
    private void openTeamPicker() {
        TeamPickerBottomSheet.loadAndShow(
                this,
                teamId,
                selectedTeam -> {
                    if (selectedTeam.id == teamId) {
                        return;
                    }
                    teamId = selectedTeam.id;
                    SessionManager.getInstance().setCurrentTeamId(teamId);
                    loadTeamData(false);
                },
                () -> startActivity(new Intent(this, CreateTeamActivity.class))
        );
    }

    // 멤버 목록에서 내 tag로 userId 맞춰두기
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

    // 화면 초기화하고 팀 데이터 로드 준비
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teams);

        backBtn = findViewById(R.id.teams_header_back);
        editBtn = findViewById(R.id.teams_header_edit);
        titleHost = findViewById(R.id.teams_header_title_host);
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
        titleTv.setOnClickListener(v -> openTeamPicker());
        if (editBtn != null) {
            editBtn.setOnClickListener(v -> showEditTeamDialog());
        }
        bottomBtn.setOnClickListener(v -> onBottomButtonClick());

        teamsFabMenu = new FabSpeedDialMenu(this, teamsFab, Arrays.asList(
                new FabSpeedDialMenu.Item("팀원 초대하기", R.drawable.pic_menu_invite, this::openInviteMemberDialog),
                new FabSpeedDialMenu.Item("목표 추가하기", R.drawable.pic_menu_add_goal, () -> showAddGoalDialog(null))
        ));

        swipeRefreshLayout.setOnRefreshListener(() -> loadTeamData(true));
    }

    // FAB 메뉴 접기
    @Override
    protected void onPause() {
        super.onPause();
        if (teamsFabMenu != null) {
            teamsFabMenu.collapse();
        }
    }

    // 팀장이면 멤버 초대 다이얼로그 열기
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

    // 목표 추가 가능한 멤버만 걸러내기
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

    // 화면 돌아오면 팀 데이터 다시 불러오기
    @Override
    protected void onResume() {
        super.onResume();
        if (teamId > 0) {
            loadTeamData();
        }
    }

    // 팀 정보 로드 (당겨서 새로고침 아님)
    private void loadTeamData() {
        loadTeamData(false);
    }

    // API로 팀 정보 불러오기
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
                .url(ApiConfig.BASE_URL + "/api/team")
                .addHeader("uid", uid)
                .build();

        ApiHelper.CLIENT.newCall(teamRequest).enqueue(new Callback() {
            // 팀 정보 요청 실패
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    stopRefreshing();
                    showErrorDialog("팀 정보를 불러오지 못했습니다.", null);
                });
            }

            // 팀 정보 응답 처리
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
                        if (!ApiHelper.isSuccess(root)) {
                            stopRefreshing();
                            showErrorDialog(ApiHelper.getMessage(root, "팀 정보를 불러오지 못했습니다."), null);
                            return;
                        }
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

    // 헤더(이름, 마감일)랑 멤버 어댑터 갱신
    private void applyTeamHeader() {
        titleTv.setText(teamName);
        updateTitleMaxWidth();
        deadlineTv.setText(formatKoreanDate(teamEndDate));
        refreshLeaderState();
        memberAdapter = new TeamMemberAdapter(members, isLeader, getMyUserId(), this);
        memberList.setAdapter(memberAdapter);
    }

    // 팀 이름 pill이 설정 버튼 영역을 침범하지 않도록 가운데 정렬 + 글자 축소
    private void updateTitleMaxWidth() {
        if (titleHost == null || titleTv == null) return;
        titleHost.post(() -> {
            int hostWidth = titleHost.getWidth();
            if (hostWidth > 0) {
                titleTv.setMaxWidth(hostWidth);
                TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                        titleTv,
                        10,
                        18,
                        1,
                        TypedValue.COMPLEX_UNIT_SP
                );
                titleTv.requestLayout();
            }
        });
    }

    // 멤버·목표 목록 API 호출
    private void loadMembersAndGoals() {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) {
            stopRefreshing();
            return;
        }

        Request membersRequest = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/team/" + teamId + "/members")
                .addHeader("uid", uid)
                .build();

        Request goalsRequest = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/goal/team/" + teamId + "/all")
                .addHeader("uid", uid)
                .build();

        ApiHelper.CLIENT.newCall(membersRequest).enqueue(new Callback() {
            // 멤버 목록 요청 실패
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    stopRefreshing();
                    showErrorDialog("멤버 목록을 불러오지 못했습니다.", null);
                });
            }

            // 멤버 받고 목표 요청 이어감
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

                try {
                    JSONObject membersRoot = new JSONObject(body);
                    if (!ApiHelper.isSuccess(membersRoot)) {
                        runOnUiThread(() -> {
                            stopRefreshing();
                            showErrorDialog(ApiHelper.getMessage(membersRoot, "멤버 목록을 불러오지 못했습니다."), null);
                        });
                        return;
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        stopRefreshing();
                        showErrorDialog("멤버 목록 처리에 실패했습니다.", null);
                    });
                    return;
                }

                ApiHelper.CLIENT.newCall(goalsRequest).enqueue(new Callback() {
                    // 목표 목록 요청 실패
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        runOnUiThread(() -> {
                            stopRefreshing();
                            showErrorDialog("목표 목록을 불러오지 못했습니다.", null);
                        });
                    }

                    // 멤버·목표 합쳐서 화면 갱신
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response goalsResponse) throws IOException {
                        String goalsBody = goalsResponse.body() != null ? goalsResponse.body().string() : "";
                        runOnUiThread(() -> {
                            try {
                                if (!goalsResponse.isSuccessful()) {
                                    showErrorDialog("목표 목록을 불러오지 못했습니다.", null);
                                    return;
                                }
                                JSONObject goalsRoot = new JSONObject(goalsBody);
                                if (!ApiHelper.isSuccess(goalsRoot)) {
                                    showErrorDialog(ApiHelper.getMessage(goalsRoot, "목표 목록을 불러오지 못했습니다."), null);
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

    // 멤버 JSON 파싱
    private List<TeamMemberItem> parseMembers(String body) throws Exception {
        return TeamMemberItem.parseList(body);
    }

    // 목표 JSON 파싱
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

    // 멤버에 목표 붙이고 정렬
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

        Long myUserId = getMyUserId();
        members.sort((a, b) -> {
            boolean aMe = myUserId != null && myUserId == a.userId;
            boolean bMe = myUserId != null && myUserId == b.userId;
            if (aMe != bMe) return aMe ? -1 : 1;
            if (a.leader != b.leader) return a.leader ? -1 : 1;
            return a.name.compareTo(b.name);
        });

        syncMyUserIdFromMembers(parsedMembers);
        refreshLeaderState();
        memberAdapter = new TeamMemberAdapter(members, isLeader, getMyUserId(), this);
        memberList.setAdapter(memberAdapter);

        updateTeamProgress(parsedGoals);
        memberAdapter.notifyDataSetChanged();
    }

    // 팀 전체 진행률 계산해서 UI 반영
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

    // 팀 이름·마감일 수정 다이얼로그
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
                // 팀 이름 길이 검사
                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() < TeamUiConstants.TEAM_NAME_MIN
                            || s.length() > TeamUiConstants.TEAM_NAME_MAX) {
                        nameWarn.setText(TeamUiConstants.teamNameLengthMessage());
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

    // 서버에 팀 정보 수정 PATCH
    private void updateTeam(String name, String endDate, Dialog dialog) {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        String json = "{\"name\":\"" + ApiHelper.escapeJson(name) + "\",\"endDate\":\"" + ApiHelper.escapeJson(endDate) + "\"}";
        RequestBody body = RequestBody.create(json, ApiHelper.JSON);
        Request request = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/team/" + teamId)
                .patch(body)
                .addHeader("uid", uid)
                .build();

        ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
            // 팀 수정 요청 실패
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> CommonDialog.showError(TeamsActivity.this, "팀 정보 수정에 실패했습니다."));
            }

            // 팀 수정 성공하면 다시 로드
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        dialog.dismiss();
                        loadTeamData();
                    } else {
                        CommonDialog.showError(TeamsActivity.this, "팀 정보 수정에 실패했습니다.");
                    }
                });
            }
        });
    }

    // 멤버에게 목표 추가 다이얼로그 열기
    @Override
    public void onAddGoal(TeamMemberItem member) {
        showAddGoalDialog(member);
    }

    // 목표 추가 다이얼로그 띄우기
    private void showAddGoalDialog(TeamMemberItem preselected) {
        List<TeamMemberItem> eligible = getGoalAddEligibleMembers();
        if (eligible.isEmpty()) {
            showErrorDialog("목표를 추가할 수 있는 멤버가 없습니다.", null);
            return;
        }

        AddGoalDialog.show(this, ApiHelper.CLIENT, teamId, eligible, preselected, new AddGoalDialog.OnComplete() {
            // 목표 추가 성공하면 목록 갱신
            @Override
            public void onSuccess() {
                loadMembersAndGoals();
            }

            // 목표 추가 실패 메시지 표시
            @Override
            public void onError(String message) {
                showErrorDialog(message, null);
            }
        });
    }

    // 목표 상세 다이얼로그 열기
    @Override
    public void onGoalClick(TeamGoalItem goal, TeamMemberItem member) {
        GoalDetailDialog.show(this, ApiHelper.CLIENT, goal, isLeader, this::loadMembersAndGoals);
    }

    // 팀장이면 목표 삭제 확인
    @Override
    public void onDeleteGoal(TeamGoalItem goal) {
        if (!isLeader) return;

        ConfirmCancelDialog dialog = new ConfirmCancelDialog(
                this,
                "이 목표를 삭제하시겠습니까?",
                "삭제",
                "취소"
        );
        dialog.setOnConfirmListener(v -> {
            dialog.dismiss();
            deleteGoal(goal.id);
        });
        dialog.show();
    }

    // 서버에 목표 DELETE
    private void deleteGoal(long goalId) {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        Request request = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/goal/" + goalId)
                .delete()
                .addHeader("uid", uid)
                .build();

        ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
            // 목표 삭제 요청 실패
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> CommonDialog.showError(TeamsActivity.this, "목표 삭제에 실패했습니다."));
            }

            // 목표 삭제 성공하면 목록 갱신
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        loadMembersAndGoals();
                    } else {
                        CommonDialog.showError(TeamsActivity.this, "목표 삭제에 실패했습니다.");
                    }
                });
            }
        });
    }

    // 팀 삭제 or 팀 나가기 확인
    private void onBottomButtonClick() {
        if (isLeader) {
            ConfirmCancelDialog dialog = new ConfirmCancelDialog(
                    this,
                    "팀을 삭제하시겠습니까?\n모든 목표와 멤버 정보가 삭제됩니다.",
                    "삭제",
                    "취소"
            );
            dialog.setOnConfirmListener(v -> {
                dialog.dismiss();
                deleteTeam();
            });
            dialog.show();
        } else {
            ConfirmCancelDialog dialog = new ConfirmCancelDialog(
                    this,
                    "팀에서 나가시겠습니까?",
                    "나가기",
                    "취소"
            );
            dialog.setOnConfirmListener(v -> {
                dialog.dismiss();
                leaveTeam();
            });
            dialog.show();
        }
    }

    // 팀 나가기 API 호출
    private void leaveTeam() {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        Request request = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/team/" + teamId + "/leave")
                .delete()
                .addHeader("uid", uid)
                .build();

        ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
            // 팀 나가기 요청 실패
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> CommonDialog.showError(TeamsActivity.this, "팀 나가기에 실패했습니다."));
            }

            // 팀 나가기 성공하면 화면 닫기
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

    // 팀 삭제 API 호출
    private void deleteTeam() {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        Request request = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/team/" + teamId)
                .delete()
                .addHeader("uid", uid)
                .build();

        ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
            // 팀 삭제 요청 실패
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> CommonDialog.showError(TeamsActivity.this, "팀 삭제에 실패했습니다."));
            }

            // 팀 삭제 성공하면 화면 닫기
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        SessionManager.getInstance().setCurrentTeamId(null);
                        finish();
                    } else {
                        CommonDialog.showError(TeamsActivity.this, "팀 삭제에 실패했습니다.");
                    }
                });
            }
        });
    }

    // API 에러 메시지 뽑기
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

    // 날짜 선택 (7일 후부터)
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

    // 날짜를 yyyy년 M월 d일 형식으로
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

    // 스와이프 새로고침 멈추기
    private void stopRefreshing() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    // 에러 다이얼로그 띄우기
    private void showErrorDialog(String message, Runnable onConfirm) {
        if (onConfirm == null) {
            CommonDialog.showError(this, message);
            return;
        }
        CommonDialog dialog = new CommonDialog(this, message, "확인");
        dialog.setOnConfirmListener(v -> {
            dialog.dismiss();
            onConfirm.run();
        });
        dialog.show();
    }

    private interface DateSelectedListener {
        // 날짜 선택됐을 때 콜백
        void onSelected(LocalDate date);
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        // TextWatcher 빈 구현
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        // TextWatcher 빈 구현
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}

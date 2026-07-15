package lee.mjc.current_stream_app;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 새 팀 만들기 화면임
 * 팀 이름·마감일 검증 후 서버에 팀 생성하고, tag로 팀원 최대 5명까지 순차 초대함
 */
public class CreateTeamActivity extends AppCompatActivity {

    /** 초대 가능한 팀원 최대 인원 */
    private static final int MAX_INVITE_COUNT = 5;

    private EditText nameEdit;
    private EditText dateEdit;
    private EditText usersEdit;
    private TextView nameWarn;
    private TextView dateWarn;
    private TextView usersCountTv;
    private Button createBtn;
    private Button closeBtn;
    private ImageButton backBtn;
    private RecyclerView usersList;

    /** 초대할 팀원 목록 (tag 검증 통과한 멤버만 담김) */
    private final List<InviteMember> inviteMembers = new ArrayList<>();
    private CreateTeamMemberAdapter memberAdapter;

    /** 0: 팀 이름, 1: 목표 날짜 — 둘 다 true일 때만 생성 버튼 활성화됨 */
    boolean[] check = {false, false};

    // --- lifecycle ---

    /**
     * 레이아웃 바인딩하고 입력 검증·날짜 선택·초대 입력·생성 버튼 리스너 연결함
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_team);

        // 뷰 참조
        nameEdit = findViewById(R.id.create_team_name_edit);
        dateEdit = findViewById(R.id.create_team_date_edit);
        View tagInputRoot = findViewById(R.id.create_team_tag_input);
        usersEdit = tagInputRoot.findViewById(R.id.tag_input_edit);
        MaterialButton addTagBtn = tagInputRoot.findViewById(R.id.tag_input_add_btn);
        nameWarn = findViewById(R.id.create_team_name_warn);
        dateWarn = findViewById(R.id.create_team_date_warn);
        usersCountTv = findViewById(R.id.create_team_users_count);
        createBtn = findViewById(R.id.create_team_button);
        closeBtn = findViewById(R.id.create_team_close_button);
        backBtn = findViewById(R.id.create_team_header_back);
        usersList = findViewById(R.id.create_team_users_list);

        // 초기 UI 상태
        nameWarn.setVisibility(View.INVISIBLE);
        dateWarn.setVisibility(View.INVISIBLE);
        createBtn.setEnabled(false);

        // 초대 팀원 RecyclerView 설정
        usersList.setLayoutManager(new LinearLayoutManager(this));
        memberAdapter = new CreateTeamMemberAdapter(inviteMembers, position -> {
            if (position >= 0 && position < inviteMembers.size()) {
                inviteMembers.remove(position);
                memberAdapter.notifyDataSetChanged();
                updateInviteCount();
            }
        });
        usersList.setAdapter(memberAdapter);
        updateInviteCount();

        setupNameValidation();
        setupDatePicker();
        setupInviteInput();
        addTagBtn.setOnClickListener(v -> verifyAndAddInviteTag());

        backBtn.setOnClickListener(v -> finish());
        closeBtn.setOnClickListener(v -> finish());
        createBtn.setOnClickListener(v -> createTeam());
    }

    /**
     * 팀 이름 입력란에 실시간 길이 검증 리스너 붙임
     */
    private void setupNameValidation() {
        nameEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() < 1) {
                    nameWarn.setText("팀 이름을 입력해주세요.");
                    nameWarn.setVisibility(View.VISIBLE);
                    check[0] = false;
                } else if (s.length() < TeamUiConstants.TEAM_NAME_MIN
                        || s.length() > TeamUiConstants.TEAM_NAME_MAX) {
                    nameWarn.setText(TeamUiConstants.teamNameLengthMessage());
                    nameWarn.setVisibility(View.VISIBLE);
                    check[0] = false;
                } else {
                    nameWarn.setVisibility(View.INVISIBLE);
                    check[0] = true;
                }
                updateCreateButton();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * 날짜 입력란 클릭 시 DatePickerDialog 열리게 함
     */
    private void setupDatePicker() {
        dateEdit.setOnClickListener(v -> showDatePicker());
    }

    /**
     * 마감일 선택 DatePickerDialog 표시함
     * 최소 7일 후만 선택 가능하도록 제한함
     */
    private void showDatePicker() {
        LocalDate minDate = LocalDate.now().plusDays(7);
        Calendar calendar = Calendar.getInstance();
        calendar.set(minDate.getYear(), minDate.getMonthValue() - 1, minDate.getDayOfMonth(), 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    LocalDate selected = LocalDate.of(year, month + 1, dayOfMonth);
                    dateEdit.setText(selected.toString());
                    validateSelectedDate(selected);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
        dialog.show();
    }

    /**
     * 선택한 날짜가 7일 후 이상인지 검사하고 check[1] 갱신함
     */
    private void validateSelectedDate(LocalDate date) {
        if (date.isBefore(LocalDate.now().plusDays(7))) {
            dateWarn.setText("날짜는 7일 후로 설정해주세요.");
            dateWarn.setVisibility(View.VISIBLE);
            check[1] = false;
        } else {
            dateWarn.setVisibility(View.INVISIBLE);
            check[1] = true;
        }
        updateCreateButton();
    }

    /**
     * 초대 tag 입력란에서 엔터(완료) 키 입력 시 tag 검증 실행함
     */
    private void setupInviteInput() {
        usersEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN)) {
                verifyAndAddInviteTag();
                return true;
            }
            return false;
        });
    }

    /**
     * 입력한 tag가 서버에 존재하는지 확인한 뒤 초대 목록에 추가함
     * 중복·본인 tag·최대 인원 초과는 여기서 걸러짐
     */
    private void verifyAndAddInviteTag() {
        String tag = usersEdit.getText().toString().trim();
        if (tag.isEmpty()) return;

        if (inviteMembers.size() >= MAX_INVITE_COUNT) {
            showDialog("팀원은 최대 5명까지 초대할 수 있습니다.");
            return;
        }

        if (containsTag(tag)) {
            showDialog("이미 추가된 tag입니다.");
            usersEdit.setText("");
            return;
        }

        usersEdit.setEnabled(false);

        try {
            String encodedTag = URLEncoder.encode(tag, StandardCharsets.UTF_8.toString());
            Request request = new Request.Builder()
                    .url(ApiConfig.BASE_URL + "/api/user/tag?tag=" + encodedTag)
                    .get()
                    .build();

            // GET /api/user/tag — tag 존재 여부 확인
            ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        usersEdit.setEnabled(true);
                        showDialog("tag 확인 중 서버와 연결할 수 없습니다.");
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String resBody = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        usersEdit.setEnabled(true);

                        if (!response.isSuccessful()) {
                            showDialog("존재하지 않는 tag입니다.");
                            return;
                        }

                        try {
                            JSONObject root = new JSONObject(resBody);
                            if (!ApiHelper.isSuccess(root)) {
                                showDialog("존재하지 않는 tag입니다.");
                                return;
                            }

                            JSONObject data = root.optJSONObject("responseData");
                            if (data == null) {
                                showDialog("존재하지 않는 tag입니다.");
                                return;
                            }

                            String foundTag = data.optString("tag", tag);
                            String foundName = data.optString("name", foundTag);
                            String foundUid = data.optString("uid", "");

                            // [중요] 본인 tag는 초대 목록에 넣지 않음
                            String myUid = SessionManager.getInstance().getUid();
                            if (myUid != null && myUid.equals(foundUid)) {
                                showDialog("본인 tag는 초대할 수 없습니다.");
                                usersEdit.setText("");
                                return;
                            }

                            if (containsTag(foundTag)) {
                                showDialog("이미 추가된 tag입니다.");
                                usersEdit.setText("");
                                return;
                            }

                            inviteMembers.add(new InviteMember(foundName, foundTag));
                            memberAdapter.notifyDataSetChanged();
                            usersEdit.setText("");
                            updateInviteCount();
                        } catch (Exception e) {
                            showDialog("tag 확인 응답을 처리하지 못했습니다.");
                        }
                    });
                }
            });
        } catch (Exception e) {
            usersEdit.setEnabled(true);
            showDialog("tag 확인 요청을 만들지 못했습니다.");
        }
    }

    /**
     * 초대 목록에 동일 tag가 이미 있는지 확인함
     */
    private boolean containsTag(String tag) {
        for (InviteMember member : inviteMembers) {
            if (member.tag.equals(tag)) return true;
        }
        return false;
    }

    /**
     * 초대 인원 수 UI를 (n/5) 형식으로 갱신함
     */
    private void updateInviteCount() {
        usersCountTv.setText("(" + inviteMembers.size() + "/" + MAX_INVITE_COUNT + ")");
    }

    /**
     * 이름·날짜 검증이 모두 통과했을 때만 생성 버튼 활성화함
     */
    private void updateCreateButton() {
        for (boolean b : check) {
            if (!b) {
                createBtn.setEnabled(false);
                return;
            }
        }
        createBtn.setEnabled(true);
    }

    // --- API ---

    /**
     * 검증된 입력값으로 서버에 팀 생성 POST 보냄
     * 성공하면 초대 팀원이 있을 때 순차 초대 이어감
     */
    private void createTeam() {
        if (!check[0] || !check[1]) return;

        // API 호출 전 로그인 uid 확인
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) {
            showDialog("로그인 정보가 없습니다.");
            return;
        }

        String name = nameEdit.getText().toString().trim();
        String endDate = dateEdit.getText().toString().trim();

        createBtn.setEnabled(false);

        try {
            JSONObject json = new JSONObject();
            json.put("name", name);
            json.put("endDate", endDate);

            RequestBody body = RequestBody.create(json.toString(), ApiHelper.JSON);
            Request request = new Request.Builder()
                    .url(ApiConfig.BASE_URL + "/api/team")
                    .post(body)
                    .addHeader("uid", uid)
                    .build();

            // POST /api/team — 팀 생성
            ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        updateCreateButton();
                        showDialog("서버와 연결할 수 없습니다.");
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String resBody = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        if (!response.isSuccessful()) {
                            updateCreateButton();
                            showDialog("팀 생성에 실패했습니다.");
                            return;
                        }

                        try {
                            JSONObject root = new JSONObject(resBody);
                            JSONObject data = root.optJSONObject("responseData");
                            if (data == null || !data.has("id")) {
                                updateCreateButton();
                                showDialog("팀 생성 응답을 처리하지 못했습니다.");
                                return;
                            }

                            long teamId = data.getLong("id");
                            if (inviteMembers.isEmpty()) {
                                showDialogAndFinish("팀이 생성되었습니다.");
                            } else {
                                inviteMembersSequentially(uid, teamId, 0);
                            }
                        } catch (Exception e) {
                            updateCreateButton();
                            showDialog("팀 생성 응답을 처리하지 못했습니다.");
                        }
                    });
                }
            });
        } catch (Exception e) {
            updateCreateButton();
            showDialog("팀 생성 요청을 만들지 못했습니다.");
        }
    }

    /**
     * 초대 팀원을 인덱스 순서대로 하나씩 서버에 전송함
     * 재귀 호출로 순차 처리해서 동시 요청 충돌 방지함
     */
    private void inviteMembersSequentially(String uid, long teamId, int index) {
        if (index >= inviteMembers.size()) {
            showDialogAndFinish("팀이 생성되었고 초대를 보냈습니다.");
            return;
        }

        String tag = inviteMembers.get(index).tag;

        try {
            JSONObject json = new JSONObject();
            json.put("teamId", String.valueOf(teamId));
            json.put("tag", tag);

            RequestBody body = RequestBody.create(json.toString(), ApiHelper.JSON);
            Request request = new Request.Builder()
                    .url(ApiConfig.BASE_URL + "/api/team/invite")
                    .post(body)
                    .addHeader("uid", uid)
                    .build();

            // POST /api/team/invite — 팀원 초대 전송
            ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        updateCreateButton();
                        showDialog("팀은 생성됐지만 일부 초대에 실패했습니다.");
                    });
                }

                @Override
                public void onResponse(Call call, Response response) {
                    runOnUiThread(() -> inviteMembersSequentially(uid, teamId, index + 1));
                }
            });
        } catch (Exception e) {
            updateCreateButton();
            showDialog("초대 요청을 만들지 못했습니다.");
        }
    }

    /**
     * 안내 메시지 대화상자 표시함
     */
    private void showDialog(String message) {
        CommonDialog dialog = new CommonDialog(this, message, "확인");
        dialog.setOnConfirmListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * 안내 메시지 표시한 뒤 Activity 종료함
     */
    private void showDialogAndFinish(String message) {
        CommonDialog dialog = new CommonDialog(this, message, "확인");
        dialog.setOnConfirmListener(v -> {
            dialog.dismiss();
            finish();
        });
        dialog.show();
    }
}

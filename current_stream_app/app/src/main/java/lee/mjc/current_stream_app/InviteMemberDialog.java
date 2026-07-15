package lee.mjc.current_stream_app;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 기존 팀에 tag로 팀원 검색해서 초대하는 대화상자임
 * tag 검증 → 중복·본인·기존 팀원 확인 → 순차 초대 전송 단계로 나눠서 잘못된 초대 미리 걸러냄
 */
public final class InviteMemberDialog {

    private static final int MAX_INVITE_COUNT = 5;

    /**
     * 초대 전송 다 끝났을 때 성공·실패 결과 알려주는 콜백임
     */
    public interface OnCompleteListener {
        void onComplete(boolean success, String message);
    }

    private InviteMemberDialog() {
    }

    /**
     * 팀원 초대 대화상자 띄우고 tag 입력·목록 UI 세팅함
     * 최대 5명까지 추가 가능하고 목록 비어 있으면 전송 버튼 비활성
     */
    public static void show(AppCompatActivity activity, long teamId, OnCompleteListener listener) {
        Dialog dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_invite_member);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View tagInputRoot = dialog.findViewById(R.id.dialog_invite_tag_input);
        EditText tagEdit = tagInputRoot.findViewById(R.id.tag_input_edit);
        MaterialButton addTagBtn = tagInputRoot.findViewById(R.id.tag_input_add_btn);
        TextView countTv = dialog.findViewById(R.id.dialog_invite_count);
        RecyclerView listRv = dialog.findViewById(R.id.dialog_invite_list);
        MaterialButton cancelBtn = dialog.findViewById(R.id.dialog_invite_cancel);
        MaterialButton submitBtn = dialog.findViewById(R.id.dialog_invite_submit);

        DialogUiHelper.styleDialogCancelButton(cancelBtn);
        DialogUiHelper.styleDialogPrimaryButton(submitBtn);

        OkHttpClient client = ApiHelper.CLIENT;
        List<InviteMember> inviteMembers = new ArrayList<>();

        listRv.setLayoutManager(new LinearLayoutManager(activity));
        final CreateTeamMemberAdapter[] adapterHolder = new CreateTeamMemberAdapter[1];
        adapterHolder[0] = new CreateTeamMemberAdapter(inviteMembers, position -> {
            if (position >= 0 && position < inviteMembers.size()) {
                inviteMembers.remove(position);
                adapterHolder[0].notifyDataSetChanged();
                updateCount(countTv, inviteMembers);
                submitBtn.setEnabled(!inviteMembers.isEmpty());
            }
        });
        listRv.setAdapter(adapterHolder[0]);
        updateCount(countTv, inviteMembers);

        addTagBtn.setOnClickListener(v ->
                verifyAndAddTag(activity, client, teamId, tagEdit, inviteMembers, adapterHolder[0], countTv, submitBtn)
        );

        // 키보드 완료(엔터) 눌러도 tag 추가되게 함
        tagEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN)) {
                verifyAndAddTag(activity, client, teamId, tagEdit, inviteMembers, adapterHolder[0], countTv, submitBtn);
                return true;
            }
            return false;
        });

        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        submitBtn.setOnClickListener(v -> {
            if (inviteMembers.isEmpty()) return;
            submitBtn.setEnabled(false);
            tagEdit.setEnabled(false);
            // index 0부터 재귀로 한 명씩 POST /api/team/invite
            sendInvites(activity, client, teamId, inviteMembers, 0, dialog, listener);
        });

        dialog.show();
    }

    /**
     * 초대 목록 인원 수 (n/5) 형식으로 갱신함
     */
    private static void updateCount(TextView countTv, List<InviteMember> members) {
        countTv.setText("(" + members.size() + "/" + MAX_INVITE_COUNT + ")");
    }

    /**
     * 입력한 tag 서버에 있는지 GET /api/user/tag 로 확인한 뒤 목록에 추가함
     * 중복·본인 tag·최대 인원은 클라에서 먼저 검사함
     */
    private static void verifyAndAddTag(
            AppCompatActivity activity,
            OkHttpClient client,
            long teamId,
            EditText tagEdit,
            List<InviteMember> inviteMembers,
            CreateTeamMemberAdapter adapter,
            TextView countTv,
            MaterialButton submitBtn
    ) {
        String tag = tagEdit.getText().toString().trim();
        if (tag.isEmpty()) return;

        if (inviteMembers.size() >= MAX_INVITE_COUNT) {
            showMessage(activity, "팀원은 최대 5명까지 초대할 수 있습니다.");
            return;
        }

        if (containsTag(inviteMembers, tag)) {
            showMessage(activity, "이미 추가된 tag입니다.");
            tagEdit.setText("");
            return;
        }

        tagEdit.setEnabled(false);

        try {
            String encodedTag = URLEncoder.encode(tag, StandardCharsets.UTF_8.toString());
            Request request = new Request.Builder()
                    .url(ApiConfig.BASE_URL + "/api/user/tag?tag=" + encodedTag)
                    .get()
                    .build();

            // [중요] GET /api/user/tag — tag 존재 여부 확인 (uid 헤더 없음, 공개 tag 조회)
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    activity.runOnUiThread(() -> {
                        tagEdit.setEnabled(true);
                        showMessage(activity, "tag 확인 중 서버와 연결할 수 없습니다.");
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String resBody = response.body() != null ? response.body().string() : "";
                    activity.runOnUiThread(() -> {
                        tagEdit.setEnabled(true);

                        if (!response.isSuccessful()) {
                            showMessage(activity, "존재하지 않는 tag입니다.");
                            return;
                        }

                        try {
                            JSONObject root = new JSONObject(resBody);
                            if (!ApiHelper.isSuccess(root)) {
                                showMessage(activity, "존재하지 않는 tag입니다.");
                                return;
                            }

                            JSONObject data = root.optJSONObject("responseData");
                            if (data == null) {
                                showMessage(activity, "존재하지 않는 tag입니다.");
                                return;
                            }

                            String foundTag = data.optString("tag", tag);
                            String foundName = data.optString("name", foundTag);
                            String foundUid = data.optString("uid", "");

                            // [중요] 본인 tag 초대 방지 — SessionManager uid랑 응답 uid 비교
                            String myUid = SessionManager.getInstance().getUid();
                            if (myUid != null && myUid.equals(foundUid)) {
                                showMessage(activity, "본인 tag는 초대할 수 없습니다.");
                                tagEdit.setText("");
                                return;
                            }

                            if (containsTag(inviteMembers, foundTag)) {
                                showMessage(activity, "이미 추가된 tag입니다.");
                                tagEdit.setText("");
                                return;
                            }

                            // 이미 팀원인지 GET /api/team/{id}/members 로 한 번 더 확인
                            validateNotAlreadyInTeam(
                                    activity,
                                    client,
                                    teamId,
                                    foundTag,
                                    foundName,
                                    tagEdit,
                                    inviteMembers,
                                    adapter,
                                    countTv,
                                    submitBtn
                            );
                        } catch (Exception e) {
                            showMessage(activity, "tag 확인 응답을 처리하지 못했습니다.");
                        }
                    });
                }
            });
        } catch (Exception e) {
            tagEdit.setEnabled(true);
            showMessage(activity, "tag 확인 요청을 만들지 못했습니다.");
        }
    }

    /**
     * 해당 tag 사용자가 이미 이 팀 팀원인지 서버 목록이랑 대조함
     * tag 추가 직전에 중복 팀원 초대 막으려고 한 번 더 확인함
     */
    private static void validateNotAlreadyInTeam(
            AppCompatActivity activity,
            OkHttpClient client,
            long teamId,
            String foundTag,
            String foundName,
            EditText tagEdit,
            List<InviteMember> inviteMembers,
            CreateTeamMemberAdapter adapter,
            TextView countTv,
            MaterialButton submitBtn
    ) {
        // [중요] API 호출 전 로그인 uid 확인 — 팀원 목록은 인증 필요
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) {
            showMessage(activity, "로그인 정보가 없습니다.");
            return;
        }

        Request request = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/team/" + teamId + "/members")
                .addHeader("uid", uid)
                .build();

        // [중요] GET /api/team/{id}/members — 기존 팀원 tag랑 비교해서 중복 초대 방지
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                activity.runOnUiThread(() ->
                        showMessage(activity, "팀원 목록을 확인하지 못했습니다.")
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                activity.runOnUiThread(() -> {
                    if (!response.isSuccessful()) {
                        showMessage(activity, "팀원 목록을 확인하지 못했습니다.");
                        return;
                    }
                    try {
                        JSONObject root = new JSONObject(body);
                        JSONArray arr = root.optJSONArray("responseData");
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject member = arr.getJSONObject(i);
                                if (foundTag.equals(member.optString("tag", ""))) {
                                    showMessage(activity, "이미 팀에 속한 팀원입니다.");
                                    tagEdit.setText("");
                                    return;
                                }
                            }
                        }

                        inviteMembers.add(new InviteMember(foundName, foundTag));
                        adapter.notifyDataSetChanged();
                        tagEdit.setText("");
                        updateCount(countTv, inviteMembers);
                        submitBtn.setEnabled(true);
                    } catch (Exception e) {
                        showMessage(activity, "팀원 확인 응답을 처리하지 못했습니다.");
                    }
                });
            }
        });
    }

    /**
     * 초대 목록을 인덱스 순서대로 하나씩 POST /api/team/invite 로 보냄
     * 재귀 호출로 순차 처리해서 동시 요청 부하·순서 꼬임 방지함
     */
    private static void sendInvites(
            AppCompatActivity activity,
            OkHttpClient client,
            long teamId,
            List<InviteMember> inviteMembers,
            int index,
            Dialog dialog,
            OnCompleteListener listener
    ) {
        if (index >= inviteMembers.size()) {
            dialog.dismiss();
            if (listener != null) {
                listener.onComplete(true, "초대를 보냈습니다.");
            }
            return;
        }

        // [중요] API 호출 전 로그인 uid 확인 — 팀장 권한 검증은 서버가 uid로 함
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) {
            if (listener != null) listener.onComplete(false, "로그인 정보가 없습니다.");
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

            // [중요] POST /api/team/invite — 팀원 초대 전송, 서버가 팀장 여부·중복 초대 검사
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    activity.runOnUiThread(() -> {
                        dialog.dismiss();
                        if (listener != null) {
                            listener.onComplete(false, "일부 초대 전송에 실패했습니다.");
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    activity.runOnUiThread(() -> {
                        if (!response.isSuccessful()) {
                            dialog.dismiss();
                            if (listener != null) {
                                listener.onComplete(false, mapInviteError(body));
                            }
                            return;
                        }
                        // 다음 사람 초대 (index+1)
                        sendInvites(activity, client, teamId, inviteMembers, index + 1, dialog, listener);
                    });
                }
            });
        } catch (Exception e) {
            dialog.dismiss();
            if (listener != null) listener.onComplete(false, "초대 요청을 만들지 못했습니다.");
        }
    }

    /**
     * 초대 목록에 같은 tag 이미 있는지 확인함
     */
    private static boolean containsTag(List<InviteMember> members, String tag) {
        for (InviteMember member : members) {
            if (member.tag.equals(tag)) return true;
        }
        return false;
    }

    /**
     * 서버 초대 에러 응답 문자열을 사용자 메시지로 바꿈
     * Not Leader면 팀장만 초대 가능하다고 알려줌
     */
    private static String mapInviteError(String body) {
        try {
            JSONObject root = new JSONObject(body);
            String data = root.optString("responseData", "");
            if ("Already in team".equals(data)) {
                return "이미 팀에 속한 팀원입니다.";
            }
            if ("Already invited".equals(data)) {
                return "이미 초대된 팀원입니다.";
            }
            if ("Not Leader".equals(data)) {
                return "팀장만 팀원을 초대할 수 있습니다.";
            }
            if (!data.isEmpty() && !"null".equals(data)) {
                return data;
            }
        } catch (Exception ignored) {
        }
        return "초대 전송에 실패했습니다.";
    }

    /**
     * CommonDialog로 오류·안내 메시지 띄움
     */
    private static void showMessage(AppCompatActivity activity, String message) {
        CommonDialog.showError(activity, message);
    }
}

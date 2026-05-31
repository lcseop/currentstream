package lee.mjc.current_stream_app;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.KeyEvent;
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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class InviteMemberDialog {

    private static final String BASE_URL = "http://10.0.2.2:8080";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int MAX_INVITE_COUNT = 5;

    public interface OnCompleteListener {
        void onComplete(boolean success, String message);
    }

    private InviteMemberDialog() {
    }

    public static void show(AppCompatActivity activity, long teamId, OnCompleteListener listener) {
        Dialog dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_invite_member);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText tagEdit = dialog.findViewById(R.id.dialog_invite_tag_edit);
        TextView countTv = dialog.findViewById(R.id.dialog_invite_count);
        RecyclerView listRv = dialog.findViewById(R.id.dialog_invite_list);
        MaterialButton cancelBtn = dialog.findViewById(R.id.dialog_invite_cancel);
        MaterialButton submitBtn = dialog.findViewById(R.id.dialog_invite_submit);

        OkHttpClient client = new OkHttpClient();
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
            sendInvites(activity, client, teamId, inviteMembers, 0, dialog, listener);
        });

        dialog.show();
    }

    private static void updateCount(TextView countTv, List<InviteMember> members) {
        countTv.setText("(" + members.size() + "/" + MAX_INVITE_COUNT + ")");
    }

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
                    .url(BASE_URL + "/api/user/tag?tag=" + encodedTag)
                    .get()
                    .build();

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
                            if (!root.optString("responseCode", "").endsWith("_ok")) {
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
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) {
            showMessage(activity, "로그인 정보가 없습니다.");
            return;
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "/api/team/" + teamId + "/members")
                .addHeader("uid", uid)
                .build();

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
                                    showMessage(activity, "이미 팀에 속한 멤버입니다.");
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

            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/team/invite")
                    .post(body)
                    .addHeader("uid", uid)
                    .build();

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
                        sendInvites(activity, client, teamId, inviteMembers, index + 1, dialog, listener);
                    });
                }
            });
        } catch (Exception e) {
            dialog.dismiss();
            if (listener != null) listener.onComplete(false, "초대 요청을 만들지 못했습니다.");
        }
    }

    private static boolean containsTag(List<InviteMember> members, String tag) {
        for (InviteMember member : members) {
            if (member.tag.equals(tag)) return true;
        }
        return false;
    }

    private static String mapInviteError(String body) {
        try {
            JSONObject root = new JSONObject(body);
            String data = root.optString("responseData", "");
            if ("Already in team".equals(data)) {
                return "이미 팀에 속한 멤버입니다.";
            }
            if ("Already invited".equals(data)) {
                return "이미 초대된 멤버입니다.";
            }
            if ("Not Leader".equals(data)) {
                return "팀장만 멤버를 초대할 수 있습니다.";
            }
            if (!data.isEmpty() && !"null".equals(data)) {
                return data;
            }
        } catch (Exception ignored) {
        }
        return "초대 전송에 실패했습니다.";
    }

    private static void showMessage(AppCompatActivity activity, String message) {
        CommonDialog commonDialog = new CommonDialog(activity, message, "확인");
        commonDialog.setOnConfirmListener(v -> commonDialog.dismiss());
        commonDialog.show();
    }
}

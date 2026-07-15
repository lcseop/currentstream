package lee.mjc.current_stream_app;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 팀 목표 상세 보여주고 완료 처리하는 대화상자임
 * 팀장이거나 목표 담당자만 완료 버튼 보이게 해서 권한 없는 상태 변경 막음
 */
public final class GoalDetailDialog {

    /**
     * 서버에 상태 반영된 뒤 상위 화면 목록 갱신할 때 쓰는 콜백임
     */
    public interface OnStatusUpdatedListener {
        void onStatusUpdated();
    }

    private GoalDetailDialog() {
    }

    /**
     * 목표 상세 대화상자 띄우고 상태·마감일·비고 바인딩함
     * 진행 중이면서 (팀장이거나 담당자)일 때만 완료 버튼 노출함
     */
    public static void show(
            @NonNull Activity activity,
            @NonNull OkHttpClient client,
            @NonNull TeamGoalItem goal,
            boolean isLeader,
            OnStatusUpdatedListener listener
    ) {
        Dialog dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_teams_goal_detail);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView titleTv = dialog.findViewById(R.id.dialog_teams_goal_title);
        TextView statusTv = dialog.findViewById(R.id.dialog_teams_goal_status);
        TextView deadlineTv = dialog.findViewById(R.id.dialog_teams_goal_deadline);
        TextView remarkTv = dialog.findViewById(R.id.dialog_teams_goal_remark);
        MaterialButton completeBtn = dialog.findViewById(R.id.dialog_teams_goal_complete);
        MaterialButton confirmBtn = dialog.findViewById(R.id.dialog_teams_goal_confirm);

        titleTv.setText(goal.goalText);
        statusTv.setText(goal.status == 1 ? "상태: 완료" : "상태: 진행 중");
        deadlineTv.setText("남은 시간: " + DateTimeUtil.formatRemainingDays(goal.goalEndDate));
        remarkTv.setText(goal.remark == null || goal.remark.isEmpty() ? "없음" : goal.remark);

        Long myUserId = SessionManager.getInstance().getUserId();
        // [중요] 완료 권한 — 진행 중(status 0)이고 (팀장이거나 목표 담당자 userId가 나)일 때만
        boolean canComplete = goal.status == 0
                && (isLeader || (myUserId != null && myUserId == goal.userId));

        if (canComplete) {
            completeBtn.setVisibility(android.view.View.VISIBLE);
            completeBtn.setOnClickListener(v ->
                    updateGoalStatus(activity, goal.id, 1, dialog, listener)
            );
        }

        confirmBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * PATCH /api/goal/{id}/status 로 목표 상태 서버에 반영함
     * 성공하면 대화상자 닫고 리스너로 상위 화면 갱신 알림
     */
    private static void updateGoalStatus(
            Activity activity,
            long goalId,
            int status,
            Dialog dialog,
            OnStatusUpdatedListener listener
    ) {
        // [중요] API 호출 전 로그인 uid 확인 — 없으면 그냥 return (조용히 실패)
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        String json = "{\"status\":\"" + status + "\"}";
        RequestBody body = RequestBody.create(json, ApiHelper.JSON);
        Request request = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/goal/" + goalId + "/status")
                .patch(body)
                .addHeader("uid", uid)
                .build();

        // [중요] PATCH /api/goal/{id}/status — 목표 완료(1) 등 상태 변경, uid로 본인·권한 검증은 서버가 함
        ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                ApiHelper.runOnUiThreadSafe(activity, () ->
                        CommonDialog.showError(activity, "상태 변경에 실패했습니다.")
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                ApiHelper.runOnUiThreadSafe(activity, () -> {
                    try {
                        if (response.isSuccessful()) {
                            JSONObject root = new JSONObject(body);
                            if (ApiHelper.isSuccess(root)) {
                                dialog.dismiss();
                                if (listener != null) {
                                    listener.onStatusUpdated();
                                }
                                return;
                            }
                        }
                        CommonDialog.showError(activity, "상태 변경에 실패했습니다.");
                    } catch (Exception e) {
                        CommonDialog.showError(activity, "상태 변경에 실패했습니다.");
                    }
                });
            }
        });
    }
}

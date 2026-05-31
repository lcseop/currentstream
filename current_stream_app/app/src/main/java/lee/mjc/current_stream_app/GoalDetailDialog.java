package lee.mjc.current_stream_app;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

// 팀 목표 상세 다이얼로그 (상태 변경, 마감일 표시)
public final class GoalDetailDialog {

    public interface OnStatusUpdatedListener {
        void onStatusUpdated();
    }

    private GoalDetailDialog() {
    }

    // 팀 목표 상세 다이얼로그 표시 (상태, 마감일, 완료 버튼)
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

    // 목표 상태 변경 API 호출 (완료 처리 등)
    private static void updateGoalStatus(
            Activity activity,
            long goalId,
            int status,
            Dialog dialog,
            OnStatusUpdatedListener listener
    ) {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        String json = "{\"status\":\"" + status + "\"}";
        RequestBody body = RequestBody.create(json, ApiHelper.JSON);
        Request request = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/goal/" + goalId + "/status")
                .patch(body)
                .addHeader("uid", uid)
                .build();

        ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                activity.runOnUiThread(() ->
                        CommonDialog.showError(activity, "상태 변경에 실패했습니다.")
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                activity.runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        dialog.dismiss();
                        if (listener != null) {
                            listener.onStatusUpdated();
                        }
                    } else {
                        CommonDialog.showError(activity, "상태 변경에 실패했습니다.");
                    }
                });
            }
        });
    }
}

package lee.mjc.current_stream_app;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

// 팀 목표 추가 다이얼로그 (멤버 선택, 마감일)
public final class AddGoalDialog {

    private static final int GOAL_TEXT_MAX = 50;

    public interface OnComplete {
        void onSuccess();
        void onError(String message);
    }

    private AddGoalDialog() {
    }

    // 팀 목표 추가 다이얼로그 표시 (멤버 선택, 마감일 입력)
    public static void show(
            AppCompatActivity activity,
            OkHttpClient client,
            long teamId,
            List<TeamMemberItem> eligible,
            @Nullable TeamMemberItem preselected,
            OnComplete callback
    ) {
        if (eligible == null || eligible.isEmpty()) {
            if (callback != null) {
                callback.onError("목표를 추가할 수 있는 멤버가 없습니다.");
            }
            return;
        }

        Dialog dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_teams_add_goal);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Spinner memberSpinner = dialog.findViewById(R.id.dialog_teams_add_member_spinner);
        TextView targetTv = dialog.findViewById(R.id.dialog_teams_add_target);
        EditText textEdit = dialog.findViewById(R.id.dialog_teams_add_text);
        EditText remarkEdit = dialog.findViewById(R.id.dialog_teams_add_remark);
        EditText dateEdit = dialog.findViewById(R.id.dialog_teams_add_date);
        TextView dateWarn = dialog.findViewById(R.id.dialog_teams_add_date_warn);
        MaterialButton cancelBtn = dialog.findViewById(R.id.dialog_teams_add_cancel);
        MaterialButton submitBtn = dialog.findViewById(R.id.dialog_teams_add_submit);

        targetTv.setVisibility(View.GONE);

        List<String> labels = new ArrayList<>();
        for (TeamMemberItem member : eligible) {
            String label = member.leader
                    ? member.name + " (팀장) - " + member.tag
                    : member.name + " - " + member.tag;
            labels.add(label);
        }
        memberSpinner.setAdapter(new ArrayAdapter<>(
                activity,
                android.R.layout.simple_spinner_dropdown_item,
                labels
        ));

        if (preselected != null) {
            for (int i = 0; i < eligible.size(); i++) {
                if (eligible.get(i).userId == preselected.userId) {
                    memberSpinner.setSelection(i);
                    break;
                }
            }
            memberSpinner.setEnabled(false);
        }

        final boolean[] check = {false, true, false};

        Runnable updateSubmit = () -> submitBtn.setEnabled(check[0] && check[1] && check[2]);

        textEdit.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                check[0] = !s.toString().trim().isEmpty() && s.length() <= GOAL_TEXT_MAX;
                updateSubmit.run();
            }
        });

        dateEdit.setOnClickListener(v -> showDatePicker(activity, dateEdit, dateWarn, selected -> {
            check[2] = selected != null;
            updateSubmit.run();
        }));

        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        submitBtn.setOnClickListener(v -> {
            if (!check[0] || !check[1] || !check[2]) return;
            TeamMemberItem selectedMember = eligible.get(memberSpinner.getSelectedItemPosition());
            createGoal(activity, teamId, selectedMember,
                    textEdit.getText().toString().trim(),
                    remarkEdit.getText().toString().trim(),
                    dateEdit.getText().toString().trim(),
                    dialog,
                    callback);
        });

        dialog.show();
    }

    // 서버에 팀 목표 생성 API 요청
    private static void createGoal(
            AppCompatActivity activity,
            long teamId,
            TeamMemberItem member,
            String text,
            String remark,
            String endDate,
            Dialog dialog,
            OnComplete callback
    ) {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) {
            if (callback != null) callback.onError("로그인 정보가 없습니다.");
            return;
        }

        Long myUserId = SessionManager.getInstance().getUserId();
        String targetPart = "";
        if (myUserId != null && myUserId != member.userId) {
            targetPart = ",\"targetUserId\":\"" + member.userId + "\"";
        }

        String json = "{\"teamId\":\"" + teamId
                + "\",\"text\":\"" + ApiHelper.escapeJson(text)
                + "\",\"remark\":\"" + ApiHelper.escapeJson(remark)
                + "\",\"endDate\":\"" + ApiHelper.escapeJson(endDate) + "\"" + targetPart + "}";

        RequestBody body = RequestBody.create(json, ApiHelper.JSON);
        Request request = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/goal")
                .post(body)
                .addHeader("uid", uid)
                .build();

        ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                activity.runOnUiThread(() -> {
                    if (callback != null) callback.onError("목표 추가에 실패했습니다.");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                activity.runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        dialog.dismiss();
                        if (callback != null) callback.onSuccess();
                    } else if (callback != null) {
                        callback.onError("목표 추가에 실패했습니다.");
                    }
                });
            }
        });
    }

    // 마감일 선택 DatePicker (최소 7일 후)
    private static void showDatePicker(
            AppCompatActivity activity,
            EditText dateEdit,
            TextView warnTv,
            DateSelectedListener listener
    ) {
        LocalDate minDate = LocalDate.now().plusDays(7);
        Calendar calendar = Calendar.getInstance();
        calendar.set(minDate.getYear(), minDate.getMonthValue() - 1, minDate.getDayOfMonth(), 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        DatePickerDialog picker = new DatePickerDialog(
                activity,
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

    private interface DateSelectedListener {
        void onSelected(LocalDate date);
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}

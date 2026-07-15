package lee.mjc.current_stream_app;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 팀 목표 추가 대화상자 띄우고 서버에 목표 생성 API 쏘는 유틸임
 * 팀원 고르기·입력 검증·마감일 7일 후 제한 다 여기서 함
 */
public final class AddGoalDialog {

    private static final int GOAL_TEXT_MAX = 50;

    /**
     * 목표 추가 성공·실패 때 상위 화면에 알려주는 콜백임
     * UI 스레드에서 대화상자 닫고 목록 갱신할 때 씀
     */
    public interface OnComplete {
        /** 서버 등록 성공 */
        void onSuccess();
        void onError(String message);
    }

    /** 인스턴스 안 만들고 static만 씀 */
    private AddGoalDialog() {
    }

    /**
     * 팀 목표 추가 대화상자 띄우고 입력·팀원 선택 UI 세팅함
     * preselected 있으면 팀원 선택 잠그고, 제출 전까지 텍스트·마감일 유효성 실시간 체크함
     */
    public static void show(
            AppCompatActivity activity,
            OkHttpClient client,
            long teamId,
            List<TeamMemberItem> eligible,
            @Nullable TeamMemberItem preselected,
            OnComplete callback
    ) {
        // 목표 줄 수 있는 팀원이 없으면 API 안 치고 바로 에러
        if (eligible == null || eligible.isEmpty()) {
            if (callback != null) {
                callback.onError("목표를 추가할 수 있는 팀원이 없습니다.");
            }
            return;
        }

        Dialog dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_teams_add_goal);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View memberSelector = dialog.findViewById(R.id.dialog_teams_add_member_selector);
        TextView memberNameTv = dialog.findViewById(R.id.dialog_teams_add_member_name);
        TextView memberTagTv = dialog.findViewById(R.id.dialog_teams_add_member_tag);
        TextView memberArrowTv = dialog.findViewById(R.id.dialog_teams_add_member_arrow);
        TextView targetTv = dialog.findViewById(R.id.dialog_teams_add_target);
        EditText textEdit = dialog.findViewById(R.id.dialog_teams_add_text);
        EditText remarkEdit = dialog.findViewById(R.id.dialog_teams_add_remark);
        EditText dateEdit = dialog.findViewById(R.id.dialog_teams_add_date);
        TextView dateWarn = dialog.findViewById(R.id.dialog_teams_add_date_warn);
        MaterialButton cancelBtn = dialog.findViewById(R.id.dialog_teams_add_cancel);
        MaterialButton submitBtn = dialog.findViewById(R.id.dialog_teams_add_submit);

        DialogUiHelper.styleDialogCancelButton(cancelBtn);
        DialogUiHelper.styleDialogPrimaryButton(submitBtn);

        targetTv.setVisibility(View.GONE);

        // preselected 팀원이면 eligible 목록에서 인덱스 맞춰둠
        int initialIndex = 0;
        if (preselected != null) {
            for (int i = 0; i < eligible.size(); i++) {
                if (eligible.get(i).userId == preselected.userId) {
                    initialIndex = i;
                    break;
                }
            }
        }

        // 특정 팀원 카드에서 들어왔으면 담당자 바꾸지 못하게 잠금
        final boolean memberLocked = preselected != null;
        final GoalMemberPickerAdapter[] memberAdapterHolder = new GoalMemberPickerAdapter[1];
        final PopupWindow[] memberPopupHolder = new PopupWindow[1];

        // 선택된 팀원 이름·tag 뱃지 UI 갱신
        Runnable updateMemberSelector = () -> {
            TeamMemberItem selected = memberAdapterHolder[0].getSelectedMember();
            memberNameTv.setText(selected.leader ? selected.name + " (팀장)" : selected.name);
            DialogUiHelper.applyTagBadge(memberTagTv, selected.tag);
        };

        Runnable dismissMemberPopup = () -> {
            if (memberPopupHolder[0] != null && memberPopupHolder[0].isShowing()) {
                memberPopupHolder[0].dismiss();
            }
            memberArrowTv.setText("▽");
        };

        memberAdapterHolder[0] = new GoalMemberPickerAdapter(eligible, position -> {
            updateMemberSelector.run();
            dismissMemberPopup.run();
        });
        memberAdapterHolder[0].setSelectedPosition(initialIndex);
        final GoalMemberPickerAdapter memberAdapter = memberAdapterHolder[0];

        updateMemberSelector.run();

        if (memberLocked) {
            // 팀원 카드에서 바로 들어온 경우 선택 UI 비활성
            memberSelector.setClickable(false);
            memberSelector.setFocusable(false);
            memberSelector.setForeground(null);
            memberArrowTv.setVisibility(View.GONE);
        } else {
            // 팀원 선택 팝업 띄움
            memberSelector.setOnClickListener(v -> {
                if (memberPopupHolder[0] != null && memberPopupHolder[0].isShowing()) {
                    dismissMemberPopup.run();
                    return;
                }

                View popupContent = LayoutInflater.from(activity)
                        .inflate(R.layout.popup_goal_member_picker, null, false);
                RecyclerView popupList = popupContent.findViewById(R.id.popup_member_list);
                popupList.setLayoutManager(new LinearLayoutManager(activity));
                popupList.setAdapter(memberAdapter);
                popupList.setNestedScrollingEnabled(true);

                int maxPopupHeight = (int) (220 * activity.getResources().getDisplayMetrics().density);
                PopupWindow popup = new PopupWindow(
                        popupContent,
                        memberSelector.getWidth(),
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        true
                );
                popup.setOutsideTouchable(true);
                popup.setFocusable(true);
                popup.setElevation(12f);
                popup.setOnDismissListener(() -> memberArrowTv.setText("▽"));

                memberPopupHolder[0] = popup;
                memberArrowTv.setText("△");
                popup.showAsDropDown(memberSelector, 0, (int) (4 * activity.getResources().getDisplayMetrics().density));

                // 인원 많으면 스크롤 되게 높이 제한
                popupList.post(() -> {
                    int itemCount = memberAdapter.getItemCount();
                    if (itemCount <= 0) return;
                    View child = popupList.getChildAt(0);
                    int rowHeight = child != null
                            ? child.getHeight()
                            : (int) (56 * activity.getResources().getDisplayMetrics().density);
                    int desired = Math.min(rowHeight * itemCount, maxPopupHeight);
                    ViewGroup.LayoutParams lp = popupList.getLayoutParams();
                    lp.height = desired;
                    popupList.setLayoutParams(lp);
                    popup.update(memberSelector.getWidth(), desired);
                });
            });
        }

        dialog.setOnDismissListener(d -> dismissMemberPopup.run());

        // check[0]=목표텍스트, check[1]=비고(항상 true), check[2]=마감일 — 셋 다 true여야 제출 가능
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
            TeamMemberItem selectedMember = memberAdapter.getSelectedMember();
            createGoal(activity, teamId, selectedMember,
                    textEdit.getText().toString().trim(),
                    remarkEdit.getText().toString().trim(),
                    dateEdit.getText().toString().trim(),
                    dialog,
                    callback);
        });

        dialog.show();
    }

    /**
     * 입력값이랑 고른 팀원으로 POST /api/goal 호출해서 팀 목표 만듦
     * 본인한테 줄 때는 targetUserId 안 넣고, 다른 팀원이면 targetUserId 넣어서 서버가 담당자 구분하게 함
     */
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
        // [중요] API 호출 전 로그인 uid 확인 — uid 헤더 없으면 서버가 401/거부함
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) {
            if (callback != null) callback.onError("로그인 정보가 없습니다.");
            return;
        }

        // [중요] 내 userId랑 선택한 팀원 userId 비교 — 다를 때만 targetUserId JSON에 넣음
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

        // [중요] POST /api/goal — 팀 목표 생성, uid 헤더로 누가 만드는지 인증
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

    /**
     * 마감일 DatePicker 띄움
     * 최소 7일 후만 고를 수 있게 막아서 너무 짧은 목표 기간 방지함
     */
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

    /** DatePicker에서 날짜 확정·취소될 때 제출 버튼 활성화 여부 알려주는 콜백임 */
    private interface DateSelectedListener {
        void onSelected(LocalDate date);
    }

    /** afterTextChanged만 쓰는 TextWatcher — 빈 메서드 구현 줄이려고 만듦 */
    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}

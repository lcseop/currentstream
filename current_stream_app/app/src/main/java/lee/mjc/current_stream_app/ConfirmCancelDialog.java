package lee.mjc.current_stream_app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;

/**
 * 확인·취소 둘 다 있는 커스텀 대화상자임
 * 삭제 전에 한 번 더 물어볼 때 씀
 */
public class ConfirmCancelDialog extends Dialog {

    private final MaterialButton btnConfirm;
    private final MaterialButton btnCancel;

    /**
     * 메시지랑 버튼 글자 넣어서 대화상자 만듦
     * DialogUiHelper로 버튼 스타일 맞춰서 앱 전체 대화상자 느낌 통일함
     */
    public ConfirmCancelDialog(
            @NonNull Context context,
            String message,
            String confirmText,
            String cancelText
    ) {
        super(context);
        setContentView(R.layout.dialog_ok_or_cancel);
        // CommonDialog랑 달리 여기는 취소 가능하게 열어둠
        setCancelable(true);
        setCanceledOnTouchOutside(true);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvMessage = findViewById(R.id.tv_message);
        btnConfirm = findViewById(R.id.btn_confirm);
        btnCancel = findViewById(R.id.btn_cancel);

        tvMessage.setText(message);
        btnConfirm.setText(confirmText);
        btnCancel.setText(cancelText);

        // 취소는 그냥 닫기만 함
        btnCancel.setOnClickListener(v -> dismiss());

        DialogUiHelper.styleDialogCancelButton(btnCancel);
        DialogUiHelper.styleDialogPrimaryButton(btnConfirm);
    }

    /**
     * 확인 눌렀을 때 할 일 연결함 (닫기는 호출하는 쪽에서 처리)
     */
    public void setOnConfirmListener(View.OnClickListener listener) {
        btnConfirm.setOnClickListener(listener);
    }

    /**
     * 취소 눌렀을 때 할 일 연결함
     * 여기서 dismiss 먼저 하고 리스너 호출함
     */
    public void setOnCancelClickListener(View.OnClickListener listener) {
        btnCancel.setOnClickListener(v -> {
            dismiss();
            listener.onClick(v);
        });
    }
}

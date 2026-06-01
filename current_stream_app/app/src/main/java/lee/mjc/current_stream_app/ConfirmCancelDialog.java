package lee.mjc.current_stream_app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;

// 확인/취소 두 버튼 다이얼로그
public class ConfirmCancelDialog extends Dialog {

    private final MaterialButton btnConfirm;
    private final MaterialButton btnCancel;

    // 메시지와 확인/취소 버튼 텍스트로 다이얼로그 생성
    public ConfirmCancelDialog(
            @NonNull Context context,
            String message,
            String confirmText,
            String cancelText
    ) {
        super(context);
        setContentView(R.layout.dialog_ok_or_cancel);
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

        btnCancel.setOnClickListener(v -> dismiss());

        DialogUiHelper.styleDialogCancelButton(btnCancel);
        DialogUiHelper.styleDialogPrimaryButton(btnConfirm);
    }

    // 확인 버튼 클릭 리스너 설정
    public void setOnConfirmListener(View.OnClickListener listener) {
        btnConfirm.setOnClickListener(listener);
    }

    // 취소 버튼 클릭 리스너 설정 (기본 동작: dismiss 후 listener)
    public void setOnCancelClickListener(View.OnClickListener listener) {
        btnCancel.setOnClickListener(v -> {
            dismiss();
            listener.onClick(v);
        });
    }
}

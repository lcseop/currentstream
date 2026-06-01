package lee.mjc.current_stream_app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;

// 확인 버튼 하나만 있는 알림 다이얼로그
public class CommonDialog extends Dialog {

    private final MaterialButton btnConfirm;

    // 메시지와 버튼 텍스트로 다이얼로그 생성
    public CommonDialog(
            @NonNull Context context,
            String message,
            String buttonText
    ) {
        super(context);

        setContentView(R.layout.dialog_normal);

        setCancelable(false);
        setCanceledOnTouchOutside(false);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );
        }

        TextView tvMessage = findViewById(R.id.tv_message);
        btnConfirm = findViewById(R.id.btn_confirm);

        tvMessage.setText(message);
        btnConfirm.setText(buttonText);
        DialogUiHelper.styleDialogPrimaryButton(btnConfirm);
    }

    // 확인 버튼 클릭 리스너 설정
    public void setOnConfirmListener(View.OnClickListener listener) {
        btnConfirm.setOnClickListener(listener);
    }

    // 확인 버튼만 있는 오류/안내 다이얼로그 바로 띄우기
    public static void showError(Context context, String message) {
        CommonDialog dialog = new CommonDialog(context, message, "확인");
        dialog.setOnConfirmListener(v -> dialog.dismiss());
        dialog.show();
    }
}

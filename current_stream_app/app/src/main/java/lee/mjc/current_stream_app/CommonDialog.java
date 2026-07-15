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
 * 확인 버튼 하나만 있는 알림 대화상자임
 * API 에러나 단순 안내 띄울 때 씀
 */
public class CommonDialog extends Dialog {

    private final MaterialButton btnConfirm;

    /**
     * 메시지랑 버튼 글자 넣어서 대화상자 만듦
     */
    public CommonDialog(
            @NonNull Context context,
            String message,
            String buttonText
    ) {
        super(context);

        setContentView(R.layout.dialog_normal);

        // 뒤로가기·바깥 터치로 안 닫히게 막음 (실수로 닫는 거 방지)
        setCancelable(false);
        setCanceledOnTouchOutside(false);

        // 창 배경 투명 처리해서 레이아웃 둥근 모서리 보이게 함
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

    /**
     * 확인 눌렀을 때 할 일 연결함
     */
    public void setOnConfirmListener(View.OnClickListener listener) {
        btnConfirm.setOnClickListener(listener);
    }

    /**
     * 에러 메시지 한 줄 띄우고 확인 누르면 닫히게 함
     * API 실패 처리할 때 여기로 모아둠
     */
    public static void showError(Context context, String message) {
        CommonDialog dialog = new CommonDialog(context, message, "확인");
        dialog.setOnConfirmListener(v -> dialog.dismiss());
        dialog.show();
    }
}

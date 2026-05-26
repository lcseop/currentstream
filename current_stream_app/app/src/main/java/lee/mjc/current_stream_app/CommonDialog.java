package lee.mjc.current_stream_app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;

public class CommonDialog extends Dialog {

    private final MaterialButton btnConfirm;

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
    }

    public void setOnConfirmListener(View.OnClickListener listener) {
        btnConfirm.setOnClickListener(listener);
    }
}
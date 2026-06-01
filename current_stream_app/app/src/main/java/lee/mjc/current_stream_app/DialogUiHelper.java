package lee.mjc.current_stream_app;

import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

// 다이얼로그·태그 UI 공통 스타일
public final class DialogUiHelper {

    private DialogUiHelper() {
    }

    public static void styleDialogCancelButton(MaterialButton button) {
        if (button == null) return;
        button.setBackgroundTintList(null);
        button.setBackgroundResource(R.drawable.bg_dialog_btn_cancel);
        button.setTextColor(0xFF333333);
        button.setRippleColorResource(R.color.dialog_btn_ripple);
    }

    public static void styleDialogPrimaryButton(MaterialButton button) {
        if (button == null) return;
        button.setBackgroundTintList(null);
        button.setBackgroundResource(R.drawable.bg_dialog_btn_primary);
        button.setTextColor(0xFF1A1A1A);
        button.setRippleColorResource(R.color.dialog_btn_ripple);
    }

    public static void applyTagBadge(TextView tagView, String tag) {
        if (tagView == null) return;
        if (tag == null || tag.isEmpty()) {
            tagView.setVisibility(TextView.GONE);
            return;
        }
        tagView.setVisibility(TextView.VISIBLE);
        tagView.setText(tag);
        tagView.setBackgroundResource(R.drawable.tag_badge_bg);
        tagView.setTextColor(0xFF5038C5);
    }
}

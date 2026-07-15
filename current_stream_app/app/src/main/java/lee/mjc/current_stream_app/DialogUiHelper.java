package lee.mjc.current_stream_app;

import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

/**
 * 다이얼로그 버튼·태그 뱃지 같은 공통 UI 스타일 적용 클래스임.
 * drawable·색상 한곳에서 맞춰서 화면마다 버튼 모양 안 달라지게 함.
 */
public final class DialogUiHelper {

    /** new 막음 */
    private DialogUiHelper() {
    }

    /** 다이얼로그 취소(보조) 버튼에 공통 스타일 입힘 */
    public static void styleDialogCancelButton(MaterialButton button) {
        // null이면 할 일 없음
        if (button == null) return;
        button.setBackgroundTintList(null);
        button.setBackgroundResource(R.drawable.bg_dialog_btn_cancel);
        button.setTextColor(0xFF333333);
        button.setRippleColorResource(R.color.dialog_btn_ripple);
    }

    /** 다이얼로그 확인(주요) 버튼에 공통 스타일 입힘 */
    public static void styleDialogPrimaryButton(MaterialButton button) {
        // null이면 할 일 없음
        if (button == null) return;
        button.setBackgroundTintList(null);
        button.setBackgroundResource(R.drawable.bg_dialog_btn_primary);
        button.setTextColor(0xFF1A1A1A);
        button.setRippleColorResource(R.color.dialog_btn_ripple);
    }

    /** 사용자 tag를 뱃지 형태로 TextView에 표시함 */
    public static void applyTagBadge(TextView tagView, String tag) {
        // null이면 할 일 없음
        if (tagView == null) return;
        // tag 없으면 뱃지 숨김
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

package lee.mjc.current_stream_app;

import android.graphics.Color;

/**
 * 서버에서 온 팀원 색상(user_color)을 Android Color int로 바꿔주는 유틸임.
 * HEX 파싱 실패하면 기본색 씀.
 */
public final class ColorUtil {

    /** new 막음 */
    private ColorUtil() {
    }

    /** HEX 문자열(#RRGGBB)을 Color int로 바꿈. # 없으면 붙여서 파싱함 */
    public static int parseColorOrDefault(String color, int defaultColor) {
        // null·빈 값이면 기본색
        if (color == null || color.isEmpty()) {
            return defaultColor;
        }
        try {
            String normalized = color.startsWith("#") ? color : "#" + color;
            return Color.parseColor(normalized);
        } catch (Exception ignored) {
            // 형식 틀리면 기본색
            return defaultColor;
        }
    }

    /** 기존 색에 알파(투명도) 곱해서 반환함 */
    public static int withAlpha(int color, float alpha) {
        int a = Math.round(255f * alpha);
        return (color & 0x00FFFFFF) | (a << 24);
    }
}

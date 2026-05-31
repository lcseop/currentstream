package lee.mjc.current_stream_app;

import android.graphics.Color;

// 멤버 user_color 파싱 및 배경색 알파 처리
public final class ColorUtil {

    private ColorUtil() {
    }

    // HEX 문자열을 Color int로 변환 (실패 시 기본색)
    public static int parseColorOrDefault(String color, int defaultColor) {
        if (color == null || color.isEmpty()) {
            return defaultColor;
        }
        try {
            String normalized = color.startsWith("#") ? color : "#" + color;
            return Color.parseColor(normalized);
        } catch (Exception ignored) {
            return defaultColor;
        }
    }

    // 색상에 알파값 적용 (0.0~1.0)
    public static int withAlpha(int color, float alpha) {
        int a = Math.round(255f * alpha);
        return (color & 0x00FFFFFF) | (a << 24);
    }
}

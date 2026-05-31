package lee.mjc.current_stream_app;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

public final class ColorUtil {

    private ColorUtil() {
    }

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

    public static int withAlpha(int color, float alpha) {
        int a = Math.round(255f * alpha);
        return (color & 0x00FFFFFF) | (a << 24);
    }

    public static void applyRoundedBackground(View view, String colorHex, float alpha, int cornerRadiusDp) {
        int baseColor = parseColorOrDefault(colorHex, Color.parseColor("#E8E8E8"));
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        float density = view.getResources().getDisplayMetrics().density;
        drawable.setCornerRadius(cornerRadiusDp * density);
        drawable.setColor(withAlpha(baseColor, alpha));
        view.setBackground(drawable);
    }
}

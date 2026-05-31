package lee.mjc.current_stream_app;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.drawable.GradientDrawable;
import android.widget.ImageView;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * FAB 스피드 다이얼 (+ 버튼 위로 미니 FAB + 라벨)
 */
public class FabSpeedDialMenu {

    private static final int MINI_BUTTON_SIZE_DP = 50;
    private static final int ROW_VERTICAL_PADDING_DP = 10;

    public static class Item {
        public final String label;
        @DrawableRes public final int iconResId;
        public final Runnable action;

        public Item(String label, @DrawableRes int iconResId, Runnable action) {
            this.label = label;
            this.iconResId = iconResId;
            this.action = action;
        }
    }

    private final AppCompatActivity activity;
    private final FloatingActionButton mainFab;
    private final List<Item> items;
    private final FrameLayout overlay;
    private final LinearLayout menuContainer;
    private boolean expanded = false;

    public FabSpeedDialMenu(AppCompatActivity activity, FloatingActionButton mainFab, List<Item> items) {
        this.activity = activity;
        this.mainFab = mainFab;
        this.items = new ArrayList<>(items);

        FrameLayout root = activity.findViewById(android.R.id.content);

        overlay = new FrameLayout(activity);
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        overlay.setBackgroundColor(0xB3000000);
        overlay.setVisibility(View.GONE);
        overlay.setClickable(true);
        overlay.setOnClickListener(v -> collapse());
        root.addView(overlay);

        menuContainer = new LinearLayout(activity);
        menuContainer.setOrientation(LinearLayout.VERTICAL);
        menuContainer.setGravity(Gravity.END);
        menuContainer.setVisibility(View.GONE);
        menuContainer.setAlpha(0f);

        FrameLayout.LayoutParams menuParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END
        );
        menuContainer.setLayoutParams(menuParams);
        root.addView(menuContainer);

        mainFab.post(this::positionMenuAboveFab);
        buildMenuItems();
        mainFab.setOnClickListener(v -> toggle());
    }

    private void positionMenuAboveFab() {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) menuContainer.getLayoutParams();
        int fabMargin = dp(24);
        int fabSize = mainFab.getHeight() > 0 ? mainFab.getHeight() : dp(56);
        params.setMargins(fabMargin, fabMargin, fabMargin, fabMargin + fabSize + dp(32));
        menuContainer.setLayoutParams(params);
    }

    private void buildMenuItems() {
        menuContainer.removeAllViews();
        for (int i = items.size() - 1; i >= 0; i--) {
            menuContainer.addView(createMenuRow(items.get(i)));
        }
    }

    private View createMenuRow(Item item) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        row.setPadding(0, dp(ROW_VERTICAL_PADDING_DP), 0, dp(ROW_VERTICAL_PADDING_DP));

        TextView label = new TextView(activity);
        label.setText(item.label);
        label.setTextColor(Color.WHITE);
        label.setTextSize(15f);
        label.setGravity(Gravity.END);
        label.setPadding(0, 0, dp(12), 0);

        FrameLayout iconButton = new FrameLayout(activity);
        int buttonSize = dp(MINI_BUTTON_SIZE_DP);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(buttonSize, buttonSize);
        iconButton.setLayoutParams(buttonParams);

        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setColor(Color.WHITE);
        iconButton.setBackground(circleBg);
        iconButton.setElevation(dp(6));

        ImageView iconView = new ImageView(activity);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        iconView.setLayoutParams(iconParams);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconView.setAdjustViewBounds(true);
        iconView.setImageResource(item.iconResId);
        iconButton.addView(iconView);

        row.addView(label);
        row.addView(iconButton);

        View.OnClickListener clickListener = v -> {
            collapse();
            item.action.run();
        };
        label.setOnClickListener(clickListener);
        iconButton.setOnClickListener(clickListener);

        return row;
    }

    private void toggle() {
        if (expanded) {
            collapse();
        } else {
            expand();
        }
    }

    private void expand() {
        expanded = true;
        positionMenuAboveFab();
        overlay.setVisibility(View.VISIBLE);
        menuContainer.setVisibility(View.VISIBLE);
        menuContainer.setTranslationY(dp(16));
        menuContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        bringFabToFront();
        mainFab.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        mainFab.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        mainFab.animate().rotation(0f).setDuration(200).start();
    }

    public void collapse() {
        if (!expanded) return;
        expanded = false;
        menuContainer.animate()
                .alpha(0f)
                .translationY(dp(16))
                .setDuration(160)
                .withEndAction(() -> menuContainer.setVisibility(View.GONE))
                .start();
        overlay.setVisibility(View.GONE);
        mainFab.setImageResource(android.R.drawable.ic_input_add);
        mainFab.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        mainFab.animate().rotation(0f).setDuration(160).start();
    }

    private void bringFabToFront() {
        ViewGroup content = activity.findViewById(android.R.id.content);
        content.bringChildToFront(overlay);
        content.bringChildToFront(menuContainer);
        if (mainFab.getParent() instanceof ViewGroup) {
            ((ViewGroup) mainFab.getParent()).bringChildToFront(mainFab);
        }
    }

    private int dp(int value) {
        float density = activity.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}

package lee.mjc.current_stream_app;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class TeamMemberAdapter extends RecyclerView.Adapter<TeamMemberAdapter.MemberVH> {

    public interface Listener {
        void onAddGoal(TeamMemberItem member);
        void onGoalClick(TeamGoalItem goal, TeamMemberItem member);
        void onDeleteGoal(TeamGoalItem goal);
    }

    private final List<TeamMemberItem> members;
    private final boolean isLeader;
    private final Long myUserId;
    private final Listener listener;

    public TeamMemberAdapter(List<TeamMemberItem> members, boolean isLeader, Long myUserId, Listener listener) {
        this.members = members;
        this.isLeader = isLeader;
        this.myUserId = myUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemberVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.teams_member_list, parent, false);
        return new MemberVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberVH holder, int position) {
        TeamMemberItem member = members.get(position);
        int memberColor = ColorUtil.parseColorOrDefault(member.userColor, Color.parseColor("#E8E8E8"));

        if (holder.cardView != null) {
            holder.cardView.setCardBackgroundColor(ColorUtil.withAlpha(memberColor, 0.18f));
        }

        String nameLabel = member.leader ? member.name + " (팀장)" : member.name;
        holder.nameTv.setText(nameLabel);
        holder.tagTv.setText(member.tag);

        boolean canAddGoal = isLeader || (myUserId != null && myUserId == member.userId);
        holder.addBtn.setVisibility(canAddGoal ? View.VISIBLE : View.GONE);

        holder.addBtn.setOnClickListener(v -> {
            if (listener != null) listener.onAddGoal(member);
        });

        holder.onHeader.setOnClickListener(v -> {
            member.ongoingExpanded = !member.ongoingExpanded;
            updateAccordion(holder.onContainer, holder.onHeaderText, member.ongoingExpanded, "진행 중");
        });

        holder.comHeader.setOnClickListener(v -> {
            member.completedExpanded = !member.completedExpanded;
            updateAccordion(holder.comContainer, holder.comHeaderText, member.completedExpanded, "완료");
        });

        bindGoalSection(holder, member, memberColor);
    }

    private void bindGoalSection(MemberVH holder, TeamMemberItem member, int memberColor) {
        holder.onContainer.removeAllViews();
        holder.comContainer.removeAllViews();

        updateAccordion(holder.onContainer, holder.onHeaderText, member.ongoingExpanded, "진행 중");
        updateAccordion(holder.comContainer, holder.comHeaderText, member.completedExpanded, "완료");

        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());

        for (TeamGoalItem goal : member.ongoingGoals) {
            View row = inflater.inflate(R.layout.teams_member_list_ongoing, holder.onContainer, false);
            applyRowBackground(row, memberColor);
            TextView titleTv = row.findViewById(R.id.teams_on_subtask_title);
            TextView deadlineTv = row.findViewById(R.id.teams_on_subtask_deadline);
            ImageButton banBtn = row.findViewById(R.id.teams_on_subtask_ban_btn);

            titleTv.setText(goal.goalText);
            applyDeadlineText(deadlineTv, goal.goalEndDate);

            banBtn.setVisibility(isLeader ? View.VISIBLE : View.GONE);
            banBtn.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteGoal(goal);
            });

            row.setOnClickListener(v -> {
                if (listener != null) listener.onGoalClick(goal, member);
            });

            holder.onContainer.addView(row);
        }

        for (TeamGoalItem goal : member.completedGoals) {
            View row = inflater.inflate(R.layout.teams_member_list_completed, holder.comContainer, false);
            applyRowBackground(row, memberColor);
            TextView titleTv = row.findViewById(R.id.teams_com_subtask_title);
            ImageButton banBtn = row.findViewById(R.id.teams_com_subtask_ban_btn);

            titleTv.setText(goal.goalText);

            banBtn.setVisibility(isLeader ? View.VISIBLE : View.GONE);
            banBtn.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteGoal(goal);
            });

            row.setOnClickListener(v -> {
                if (listener != null) listener.onGoalClick(goal, member);
            });

            holder.comContainer.addView(row);
        }
    }

    private void applyRowBackground(View row, int memberColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        float density = row.getResources().getDisplayMetrics().density;
        drawable.setCornerRadius(8f * density);
        drawable.setColor(ColorUtil.withAlpha(memberColor, 0.32f));
        row.setBackground(drawable);
        int padding = Math.round(density * 8);
        row.setPadding(padding, padding / 2, padding, padding / 2);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) row.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        params.bottomMargin = Math.round(density * 6);
        row.setLayoutParams(params);
    }

    private void updateAccordion(LinearLayout container, TextView headerText, boolean expanded, String label) {
        container.setVisibility(expanded ? View.VISIBLE : View.GONE);
        headerText.setText(label + (expanded ? " ▽" : " △"));
    }

    private void applyDeadlineText(TextView deadlineTv, String endDate) {
        String text = formatRemainingDays(endDate);
        deadlineTv.setText(text);
        if (isOverdue(endDate)) {
            deadlineTv.setTextColor(Color.parseColor("#E53935"));
        } else {
            deadlineTv.setTextColor(Color.parseColor("#BBBBBB"));
        }
    }

    private boolean isOverdue(String endDate) {
        if (endDate == null || endDate.isEmpty()) return false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDate end = LocalDate.parse(endDate);
                return end.isBefore(LocalDate.now());
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    static String formatRemainingDays(String endDate) {
        if (endDate == null || endDate.isEmpty()) return "";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDate end = LocalDate.parse(endDate);
                LocalDate today = LocalDate.now();
                long days = ChronoUnit.DAYS.between(today, end);
                return days >= 0 ? (days + "일 남음") : (Math.abs(days) + "일 초과");
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class MemberVH extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView nameTv;
        TextView tagTv;
        ImageButton addBtn;
        LinearLayout onHeader;
        TextView onHeaderText;
        LinearLayout onContainer;
        LinearLayout comHeader;
        TextView comHeaderText;
        LinearLayout comContainer;

        MemberVH(@NonNull View itemView) {
            super(itemView);
            if (itemView instanceof CardView) {
                cardView = (CardView) itemView;
            }
            nameTv = itemView.findViewById(R.id.teams_list_detail_name);
            tagTv = itemView.findViewById(R.id.teams_list_detail_id);
            addBtn = itemView.findViewById(R.id.teams_list_btn_add);
            onHeader = itemView.findViewById(R.id.teams_list_on_header);
            onHeaderText = itemView.findViewById(R.id.teams_list_on_header_text);
            onContainer = itemView.findViewById(R.id.teams_list_on);
            comHeader = itemView.findViewById(R.id.teams_list_com_header);
            comHeaderText = itemView.findViewById(R.id.teams_list_com_header_text);
            comContainer = itemView.findViewById(R.id.teams_list_com);
        }
    }
}

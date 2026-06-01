package lee.mjc.current_stream_app;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// 팀 상세 멤버 목록 어댑터 (진행/완료 목표 아코디언)
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

    // 팀 멤버 목록, 팀장 여부, 내 userId, 리스너로 어댑터 생성
    public TeamMemberAdapter(List<TeamMemberItem> members, boolean isLeader, Long myUserId, Listener listener) {
        this.members = members;
        this.isLeader = isLeader;
        this.myUserId = myUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    // 멤버 카드 ViewHolder 생성
    public MemberVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.teams_member_list, parent, false);
        return new MemberVH(v);
    }

    @Override
    // 멤버 정보, 아코디언, 목표 목록 바인딩
    public void onBindViewHolder(@NonNull MemberVH holder, int position) {
        TeamMemberItem member = members.get(position);
        int memberColor = ColorUtil.parseColorOrDefault(member.userColor, Color.parseColor("#E8E8E8"));

        if (holder.cardView != null) {
            holder.cardView.setCardBackgroundColor(Color.WHITE);
        }
        if (holder.headerLayout != null) {
            holder.headerLayout.setBackgroundColor(ColorUtil.withAlpha(memberColor, 0.28f));
        }

        holder.nameTv.setText(member.name);
        if (holder.crownIv != null) {
            holder.crownIv.setVisibility(member.leader ? View.VISIBLE : View.GONE);
        }
        DialogUiHelper.applyTagBadge(holder.tagTv, member.tag);

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

        bindGoalSection(holder, member);
    }

    // 진행 중/완료 목표 섹션 동적 렌더링
    private void bindGoalSection(MemberVH holder, TeamMemberItem member) {
        holder.onContainer.removeAllViews();
        holder.comContainer.removeAllViews();

        updateAccordion(holder.onContainer, holder.onHeaderText, member.ongoingExpanded, "진행 중");
        updateAccordion(holder.comContainer, holder.comHeaderText, member.completedExpanded, "완료");

        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());

        int ongoingCount = member.ongoingGoals.size();
        for (int i = 0; i < ongoingCount; i++) {
            TeamGoalItem goal = member.ongoingGoals.get(i);
            View row = inflater.inflate(R.layout.teams_member_list_ongoing, holder.onContainer, false);
            applyRowStyle(row, i < ongoingCount - 1);
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

        int completedCount = member.completedGoals.size();
        for (int i = 0; i < completedCount; i++) {
            TeamGoalItem goal = member.completedGoals.get(i);
            View row = inflater.inflate(R.layout.teams_member_list_completed, holder.comContainer, false);
            applyRowStyle(row, i < completedCount - 1);
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

    // 목표 행 패딩·구분선 스타일 적용
    private void applyRowStyle(View row, boolean showBottomDivider) {
        float density = row.getResources().getDisplayMetrics().density;
        int paddingH = Math.round(density * 4);
        int paddingV = Math.round(density * 8);
        row.setPadding(paddingH, paddingV, paddingH, paddingV);
        if (showBottomDivider) {
            row.setBackgroundResource(R.drawable.goal_row_bottom_divider);
        } else {
            row.setBackgroundColor(Color.WHITE);
        }
    }

    // 아코디언 헤더 텍스트·컨테이너 표시/숨김
    private void updateAccordion(LinearLayout container, TextView headerText, boolean expanded, String label) {
        container.setVisibility(expanded ? View.VISIBLE : View.GONE);
        headerText.setText(label + (expanded ? " ▽" : " △"));
    }

    // 마감일 텍스트·색상 (초과 시 빨간색)
    private void applyDeadlineText(TextView deadlineTv, String endDate) {
        String text = DateTimeUtil.formatRemainingDays(endDate);
        deadlineTv.setText(text);
        if (DateTimeUtil.isGoalOverdue(endDate)) {
            deadlineTv.setTextColor(Color.parseColor("#E53935"));
        } else {
            deadlineTv.setTextColor(Color.parseColor("#888888"));
        }
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class MemberVH extends RecyclerView.ViewHolder {
        CardView cardView;
        LinearLayout headerLayout;
        TextView nameTv;
        ImageView crownIv;
        TextView tagTv;
        ImageButton addBtn;
        LinearLayout onHeader;
        TextView onHeaderText;
        LinearLayout onContainer;
        LinearLayout comHeader;
        TextView comHeaderText;
        LinearLayout comContainer;

        // ViewHolder 뷰 연결
        MemberVH(@NonNull View itemView) {
            super(itemView);
            if (itemView instanceof CardView) {
                cardView = (CardView) itemView;
            }
            headerLayout = itemView.findViewById(R.id.teams_list_header);
            nameTv = itemView.findViewById(R.id.teams_list_detail_name);
            crownIv = itemView.findViewById(R.id.teams_list_leader_crown);
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

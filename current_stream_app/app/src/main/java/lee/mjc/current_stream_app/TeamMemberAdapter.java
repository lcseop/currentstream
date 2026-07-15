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

/**
 * 팀 상세 화면 팀원 카드 목록 보여주는 어댑터임
 * 팀원별 진행/완료 목표 아코디언 펼치고, 팀장·본인에게만 목표 추가·팀장만 삭제 권한 줌
 */
public class TeamMemberAdapter extends RecyclerView.Adapter<TeamMemberAdapter.MemberVH> {

    /**
     * 팀원 카드에서 목표 추가/상세/삭제 동작 상위 Activity로 넘기는 리스너임
     */
    public interface Listener {
        void onAddGoal(TeamMemberItem member);
        void onGoalClick(TeamGoalItem goal, TeamMemberItem member);
        void onDeleteGoal(TeamGoalItem goal);
    }

    private final List<TeamMemberItem> members;
    private final boolean isLeader;
    private final Long myUserId;
    private final Listener listener;

    /**
     * 팀원 목록·권한 정보·이벤트 리스너 받아서 만듦
     * isLeader랑 myUserId로 UI 노출 권한 클라에서 판별함 (서버 권한이랑 맞춰둠)
     */
    public TeamMemberAdapter(List<TeamMemberItem> members, boolean isLeader, Long myUserId, Listener listener) {
        this.members = members;
        this.isLeader = isLeader;
        this.myUserId = myUserId;
        this.listener = listener;
    }

    /**
     * 팀원 카드 한 줄 레이아웃 inflate해서 ViewHolder 만듦
     */
    @NonNull
    @Override
    public MemberVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.teams_member_list, parent, false);
        return new MemberVH(v);
    }

    /**
     * 팀원 정보·아코디언·목표 추가 버튼 바인딩하고 클릭 이벤트 연결함
     */
    @Override
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

        // [중요] 목표 추가 권한 — 팀장이거나 해당 팀원 본인(myUserId == member.userId)일 때만 + 버튼 보임
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

    /**
     * 진행 중/완료 목표 섹션 동적으로 inflate해서 컨테이너에 넣음
     * 목표 행마다 클릭·삭제 리스너 개별 연결함
     */
    private void bindGoalSection(MemberVH holder, TeamMemberItem member) {
        holder.onContainer.removeAllViews();
        holder.comContainer.removeAllViews();

        updateAccordion(holder.onContainer, holder.onHeaderText, member.ongoingExpanded, "진행 중");
        updateAccordion(holder.comContainer, holder.comHeaderText, member.completedExpanded, "완료");

        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());

        // 진행 중 목표 행들
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

            // [중요] 목표 삭제 권한 — 팀장(isLeader)만 삭제 버튼 보임, 담당자 본인은 삭제 못 함
            banBtn.setVisibility(isLeader ? View.VISIBLE : View.GONE);
            banBtn.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteGoal(goal);
            });

            row.setOnClickListener(v -> {
                if (listener != null) listener.onGoalClick(goal, member);
            });

            holder.onContainer.addView(row);
        }

        // 완료된 목표 행들
        int completedCount = member.completedGoals.size();
        for (int i = 0; i < completedCount; i++) {
            TeamGoalItem goal = member.completedGoals.get(i);
            View row = inflater.inflate(R.layout.teams_member_list_completed, holder.comContainer, false);
            applyRowStyle(row, i < completedCount - 1);
            TextView titleTv = row.findViewById(R.id.teams_com_subtask_title);
            ImageButton banBtn = row.findViewById(R.id.teams_com_subtask_ban_btn);

            titleTv.setText(goal.goalText);

            // [중요] 목표 삭제 권한 — 팀장만 삭제 버튼 보임
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

    /**
     * 목표 행 패딩·구분선 스타일 적용함
     * 마지막 행은 구분선 없이 흰 배경
     */
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

    /**
     * 아코디언 헤더 텍스트랑 컨테이너 표시/숨김 토글함
     * 펼침 상태를 △▽ 화살표로 표시함
     */
    private void updateAccordion(LinearLayout container, TextView headerText, boolean expanded, String label) {
        container.setVisibility(expanded ? View.VISIBLE : View.GONE);
        headerText.setText(label + (expanded ? " ▽" : " △"));
    }

    /**
     * 마감일 텍스트·색 설정함
     * 기한 지났으면 빨간색으로 강조함
     */
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

    /**
     * 팀원 카드 한 줄 뷰 들고 있는 ViewHolder임
     */
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

        /**
         * 레이아웃에서 팀원 카드·아코디언·목표 컨테이너 뷰 findViewById로 연결함
         */
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

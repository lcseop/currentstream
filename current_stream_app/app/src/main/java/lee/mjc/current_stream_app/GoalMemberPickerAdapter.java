package lee.mjc.current_stream_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * 목표 추가 대화상자에서 목표 줄 팀원 고르는 단일 선택 리스트 어댑터임
 * 고른 userId가 AddGoalDialog createGoal의 targetUserId로 넘어감
 */
public class GoalMemberPickerAdapter extends RecyclerView.Adapter<GoalMemberPickerAdapter.VH> {

    /** 행 눌렀을 때 선택 바뀐 인덱스를 대화상자에 알려줌 */
    public interface OnMemberSelectedListener {
        void onMemberSelected(int position);
    }

    private final List<TeamMemberItem> members;
    private final OnMemberSelectedListener listener;
    private int selectedPosition = 0;

    /**
     * 목표 줄 수 있는 팀원 목록이랑 선택 콜백 받아서 만듦
     */
    public GoalMemberPickerAdapter(List<TeamMemberItem> members, OnMemberSelectedListener listener) {
        this.members = members;
        this.listener = listener;
    }

    /**
     * 선택 행 바꿀 때 이전·새 행만 notifyItemChanged 해서 전체 깜빡임 줄임
     */
    public void setSelectedPosition(int position) {
        if (position < 0 || position >= members.size()) return;
        int old = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(old);
        notifyItemChanged(selectedPosition);
    }

    /** 현재 선택된 인덱스 반환함 */
    public int getSelectedPosition() {
        return selectedPosition;
    }

    /**
     * 지금 고른 팀원 반환함
     * AddGoalDialog에서 POST /api/goal body의 targetUserId 정할 때 씀 (본인이면 필드 안 넣음)
     */
    public TeamMemberItem getSelectedMember() {
        return members.get(selectedPosition);
    }

    /**
     * 팀원 한 줄 레이아웃 inflate해서 ViewHolder 만듦
     */
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.dialog_goal_member_item, parent, false);
        return new VH(v);
    }

    /**
     * 이름·tag·선택 뱃지 바인딩하고 행 클릭하면 선택 바꿈
     */
    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        TeamMemberItem member = members.get(position);
        holder.nameTv.setText(member.leader ? member.name + " (팀장)" : member.name);
        DialogUiHelper.applyTagBadge(holder.tagTv, member.tag);

        boolean selected = position == selectedPosition;
        holder.selectedBadge.setVisibility(selected ? View.VISIBLE : View.GONE);
        holder.row.setAlpha(selected ? 1f : 0.92f);

        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;
            setSelectedPosition(adapterPosition);
            if (listener != null) {
                listener.onMemberSelected(adapterPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    /**
     * 팀원 선택 한 줄 뷰 들고 있는 ViewHolder임
     */
    static class VH extends RecyclerView.ViewHolder {
        final View row;
        final TextView nameTv;
        final TextView tagTv;
        final TextView selectedBadge;

        /**
         * 레이아웃에서 행·이름·tag·선택 뱃지 findViewById로 연결함
         */
        VH(@NonNull View itemView) {
            super(itemView);
            row = itemView.findViewById(R.id.goal_member_row);
            nameTv = itemView.findViewById(R.id.goal_member_name);
            tagTv = itemView.findViewById(R.id.goal_member_tag);
            selectedBadge = itemView.findViewById(R.id.goal_member_selected);
        }
    }
}

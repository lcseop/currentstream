package lee.mjc.current_stream_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// 목표 추가 다이얼로그 멤버 선택 목록
public class GoalMemberPickerAdapter extends RecyclerView.Adapter<GoalMemberPickerAdapter.VH> {

    public interface OnMemberSelectedListener {
        void onMemberSelected(int position);
    }

    private final List<TeamMemberItem> members;
    private final OnMemberSelectedListener listener;
    private int selectedPosition = 0;

    public GoalMemberPickerAdapter(List<TeamMemberItem> members, OnMemberSelectedListener listener) {
        this.members = members;
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        if (position < 0 || position >= members.size()) return;
        int old = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(old);
        notifyItemChanged(selectedPosition);
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public TeamMemberItem getSelectedMember() {
        return members.get(selectedPosition);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.dialog_goal_member_item, parent, false);
        return new VH(v);
    }

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

    static class VH extends RecyclerView.ViewHolder {
        final View row;
        final TextView nameTv;
        final TextView tagTv;
        final TextView selectedBadge;

        VH(@NonNull View itemView) {
            super(itemView);
            row = itemView.findViewById(R.id.goal_member_row);
            nameTv = itemView.findViewById(R.id.goal_member_name);
            tagTv = itemView.findViewById(R.id.goal_member_tag);
            selectedBadge = itemView.findViewById(R.id.goal_member_selected);
        }
    }
}

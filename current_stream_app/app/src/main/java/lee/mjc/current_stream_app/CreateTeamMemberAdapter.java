package lee.mjc.current_stream_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// 팀 생성/초대 시 추가된 멤버 목록 어댑터
public class CreateTeamMemberAdapter extends RecyclerView.Adapter<CreateTeamMemberAdapter.MemberViewHolder> {

    public interface OnRemoveListener {
        void onRemove(int position);
    }

    private final List<InviteMember> members;
    private final OnRemoveListener listener;

    // 초대 멤버 목록과 삭제 리스너로 어댑터 생성
    public CreateTeamMemberAdapter(List<InviteMember> members, OnRemoveListener listener) {
        this.members = members;
        this.listener = listener;
    }

    @NonNull
    @Override
    // 멤버 항목 ViewHolder 생성
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.create_team_members, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    // 이름, tag, 삭제 버튼 바인딩
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        InviteMember member = members.get(position);
        holder.nameTv.setVisibility(View.VISIBLE);
        holder.nameTv.setText(member.name);
        DialogUiHelper.applyTagBadge(holder.tagTv, member.tag);

        holder.removeBtn.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (listener != null && adapterPosition != RecyclerView.NO_POSITION) {
                listener.onRemove(adapterPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView nameTv;
        TextView tagTv;
        ImageButton removeBtn;

        // ViewHolder 뷰 연결
        MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTv = itemView.findViewById(R.id.create_team_list_name);
            tagTv = itemView.findViewById(R.id.create_team_list_id);
            removeBtn = itemView.findViewById(R.id.create_team_ban_btn);
        }
    }
}

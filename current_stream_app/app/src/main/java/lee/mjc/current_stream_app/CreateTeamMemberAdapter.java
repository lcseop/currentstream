package lee.mjc.current_stream_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CreateTeamMemberAdapter extends RecyclerView.Adapter<CreateTeamMemberAdapter.MemberViewHolder> {

    public interface OnRemoveListener {
        void onRemove(int position);
    }

    private final List<InviteMember> members;
    private final OnRemoveListener listener;

    public CreateTeamMemberAdapter(List<InviteMember> members, OnRemoveListener listener) {
        this.members = members;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.create_team_members, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        InviteMember member = members.get(position);
        holder.nameTv.setVisibility(View.VISIBLE);
        holder.nameTv.setText(member.name);
        holder.tagTv.setText(member.tag);

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

        MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTv = itemView.findViewById(R.id.create_team_list_name);
            tagTv = itemView.findViewById(R.id.create_team_list_id);
            removeBtn = itemView.findViewById(R.id.create_team_ban_btn);
        }
    }
}

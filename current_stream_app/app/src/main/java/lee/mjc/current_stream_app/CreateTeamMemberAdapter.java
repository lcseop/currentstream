package lee.mjc.current_stream_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * 팀 만들기·초대 대화상자에서 아직 서버로 안 나간 초대 예정 목록 보여주는 어댑터임
 * InviteMember 리스트만 들고 있고 X 누르면 목록에서만 빠짐 (API 호출은 상위가 함)
 */
public class CreateTeamMemberAdapter extends RecyclerView.Adapter<CreateTeamMemberAdapter.MemberViewHolder> {

    /** X 버튼 눌렀을 때 몇 번째인지 상위에 알려줌 */
    public interface OnRemoveListener {
        void onRemove(int position);
    }

    private final List<InviteMember> members;
    private final OnRemoveListener listener;

    /**
     * 초대 예정 멤버 리스트랑 삭제 콜백 받아서 만듦
     */
    public CreateTeamMemberAdapter(List<InviteMember> members, OnRemoveListener listener) {
        this.members = members;
        this.listener = listener;
    }

    /**
     * 팀원 한 줄 레이아웃 inflate해서 ViewHolder 만듦
     */
    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.create_team_members, parent, false);
        return new MemberViewHolder(view);
    }

    /**
     * 이름·tag 뱃지 바인딩하고 X 버튼에 삭제 리스너 연결함
     */
    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        InviteMember member = members.get(position);
        holder.nameTv.setVisibility(View.VISIBLE);
        holder.nameTv.setText(member.name);
        DialogUiHelper.applyTagBadge(holder.tagTv, member.tag);

        holder.removeBtn.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            // [중요] RecyclerView 재사용 중 position이 옛날 값일 수 있어서 NO_POSITION이면 무시함
            if (listener != null && adapterPosition != RecyclerView.NO_POSITION) {
                listener.onRemove(adapterPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    /**
     * 초대 예정 팀원 한 줄 뷰 들고 있는 ViewHolder임
     */
    static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView nameTv;
        TextView tagTv;
        ImageButton removeBtn;

        /**
         * 레이아웃에서 이름·tag·삭제 버튼 findViewById로 연결함
         */
        MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTv = itemView.findViewById(R.id.create_team_list_name);
            tagTv = itemView.findViewById(R.id.create_team_list_id);
            removeBtn = itemView.findViewById(R.id.create_team_ban_btn);
        }
    }
}

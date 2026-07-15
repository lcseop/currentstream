package lee.mjc.current_stream_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * 팀 초대 알림 바텀시트 초대 목록 보여주는 어댑터임
 * 각 항목에 수락·거절 버튼 있고 실제 API 호출은 상위 Activity가 처리함
 */
public class InviteAdapter extends RecyclerView.Adapter<InviteAdapter.InviteViewHolder> {

    /**
     * 수락·거절 버튼 눌렀을 때 상위로 이벤트 넘기는 리스너임
     * 상위에서 POST 수락/거절 API 칠 때 inviteId 등 item 정보 씀
     */
    public interface InviteListener {
        void onAccept(InviteItem item);
        void onReject(InviteItem item);
    }

    private final List<InviteItem> items;
    private final InviteListener listener;

    /**
     * 초대 목록이랑 수락/거절 리스너 받아서 만듦
     */
    public InviteAdapter(List<InviteItem> items, InviteListener listener) {
        this.items = items;
        this.listener = listener;
    }

    /**
     * 초대 한 줄 레이아웃 inflate해서 ViewHolder 만듦
     */
    @NonNull
    @Override
    public InviteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.main_bottom_sheet_invite_item, parent, false);
        return new InviteViewHolder(view);
    }

    /**
     * 팀명·초대 메시지·수락/거절 버튼 바인딩함
     * message 비어 있으면 inviterName으로 기본 문구 만듦
     */
    @Override
    public void onBindViewHolder(@NonNull InviteViewHolder holder, int position) {
        InviteItem item = items.get(position);
        holder.teamText.setText(item.teamName);

        String msg = item.message;
        if (msg == null || msg.isEmpty()) {
            msg = item.inviterName != null && !item.inviterName.isEmpty()
                    ? item.inviterName + " 님이 초대했습니다."
                    : "팀 초대가 도착했습니다.";
        }
        holder.messageText.setText(msg);

        holder.acceptBtn.setOnClickListener(v -> {
            if (listener != null) listener.onAccept(item);
        });
        holder.rejectBtn.setOnClickListener(v -> {
            if (listener != null) listener.onReject(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * 초대 알림 한 줄 뷰 들고 있는 ViewHolder임
     */
    static class InviteViewHolder extends RecyclerView.ViewHolder {
        TextView teamText;
        TextView messageText;
        Button acceptBtn;
        Button rejectBtn;

        /**
         * 레이아웃에서 팀명·메시지·수락/거절 버튼 findViewById로 연결함
         */
        InviteViewHolder(@NonNull View itemView) {
            super(itemView);
            teamText = itemView.findViewById(R.id.item_invite_team);
            messageText = itemView.findViewById(R.id.item_invite_message);
            acceptBtn = itemView.findViewById(R.id.item_invite_accept);
            rejectBtn = itemView.findViewById(R.id.item_invite_reject);
        }
    }
}

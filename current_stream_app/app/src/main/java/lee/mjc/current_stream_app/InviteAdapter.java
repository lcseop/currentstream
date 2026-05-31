package lee.mjc.current_stream_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// 팀 초대 알림 목록 어댑터 (수락/거절)
public class InviteAdapter extends RecyclerView.Adapter<InviteAdapter.InviteViewHolder> {

    public interface InviteListener {
        void onAccept(InviteItem item);
        void onReject(InviteItem item);
    }

    private final List<InviteItem> items;
    private final InviteListener listener;

    // 초대 목록과 수락/거절 리스너로 어댑터 생성
    public InviteAdapter(List<InviteItem> items, InviteListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    // 초대 항목 ViewHolder 생성
    public InviteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.main_bottom_sheet_invite_item, parent, false);
        return new InviteViewHolder(view);
    }

    @Override
    // 팀명, 초대 메시지, 수락/거절 버튼 바인딩
    public void onBindViewHolder(@NonNull InviteViewHolder holder, int position) {
        InviteItem item = items.get(position);
        holder.teamText.setText(item.teamName);

        String msg = item.message;
        if (msg == null || msg.isEmpty()) {
            // 기본 문구
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

    static class InviteViewHolder extends RecyclerView.ViewHolder {
        TextView teamText;
        TextView messageText;
        Button acceptBtn;
        Button rejectBtn;

        // ViewHolder 뷰 연결
        InviteViewHolder(@NonNull View itemView) {
            super(itemView);
            teamText = itemView.findViewById(R.id.item_invite_team);
            messageText = itemView.findViewById(R.id.item_invite_message);
            acceptBtn = itemView.findViewById(R.id.item_invite_accept);
            rejectBtn = itemView.findViewById(R.id.item_invite_reject);
        }
    }
}


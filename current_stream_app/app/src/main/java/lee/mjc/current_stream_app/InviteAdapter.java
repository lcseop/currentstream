package lee.mjc.current_stream_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class InviteAdapter extends RecyclerView.Adapter<InviteAdapter.InviteViewHolder> {

    public interface InviteListener {
        void onAccept(MainActivity.InviteItem item);
        void onReject(MainActivity.InviteItem item);
    }

    private final List<MainActivity.InviteItem> items;
    private final InviteListener listener;

    public InviteAdapter(List<MainActivity.InviteItem> items, InviteListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public InviteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.main_bottom_sheet_invite_item, parent, false);
        return new InviteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InviteViewHolder holder, int position) {
        MainActivity.InviteItem item = items.get(position);
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

        InviteViewHolder(@NonNull View itemView) {
            super(itemView);
            teamText = itemView.findViewById(R.id.item_invite_team);
            messageText = itemView.findViewById(R.id.item_invite_message);
            acceptBtn = itemView.findViewById(R.id.item_invite_accept);
            rejectBtn = itemView.findViewById(R.id.item_invite_reject);
        }
    }
}


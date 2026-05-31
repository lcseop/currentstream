package lee.mjc.current_stream_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// 팀 활동 로그 목록 어댑터
public class TeamLogAdapter extends RecyclerView.Adapter<TeamLogAdapter.LogVH> {

    private final List<TeamLogItem> logs;

    // 팀 활동 로그 목록으로 어댑터 생성
    public TeamLogAdapter(List<TeamLogItem> logs) {
        this.logs = logs;
    }

    @NonNull
    @Override
    // 로그 항목 ViewHolder 생성
    public LogVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.main_teams_members, parent, false);
        return new LogVH(v);
    }

    @Override
    // 로그 메시지·상대 시간 바인딩
    public void onBindViewHolder(@NonNull LogVH holder, int position) {
        TeamLogItem item = logs.get(position);
        holder.messageTv.setText(item.message);
        holder.timeTv.setText(DateTimeUtil.formatRelativeTime(item.createdAtMillis));
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class LogVH extends RecyclerView.ViewHolder {
        TextView messageTv;
        TextView timeTv;

        // ViewHolder 뷰 연결
        LogVH(@NonNull View itemView) {
            super(itemView);
            messageTv = itemView.findViewById(R.id.teams_log_message);
            timeTv = itemView.findViewById(R.id.teams_log_time);
        }
    }
}

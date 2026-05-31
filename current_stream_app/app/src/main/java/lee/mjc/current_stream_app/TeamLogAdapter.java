package lee.mjc.current_stream_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TeamLogAdapter extends RecyclerView.Adapter<TeamLogAdapter.LogVH> {

    private final List<TeamLogItem> logs;

    public TeamLogAdapter(List<TeamLogItem> logs) {
        this.logs = logs;
    }

    @NonNull
    @Override
    public LogVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.main_teams_members, parent, false);
        return new LogVH(v);
    }

    @Override
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

        LogVH(@NonNull View itemView) {
            super(itemView);
            messageTv = itemView.findViewById(R.id.teams_log_message);
            timeTv = itemView.findViewById(R.id.teams_log_time);
        }
    }
}

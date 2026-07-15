package lee.mjc.current_stream_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * 팀 활동 로그 목록 RecyclerView에 바인딩하는 어댑터임
 * 로그 메시지랑 상대 시간만 보여주는 읽기 전용 목록임 (API는 상위가 이미 불러옴)
 */
public class TeamLogAdapter extends RecyclerView.Adapter<TeamLogAdapter.LogVH> {

    private final List<TeamLogItem> logs;

    /**
     * 팀 로그 리스트 받아서 만듦
     * 리스트 참조 직접 들고 있어서 상위에서 데이터 바꾼 뒤 notify 호출해야 함
     */
    public TeamLogAdapter(List<TeamLogItem> logs) {
        this.logs = logs;
    }

    /**
     * 로그 한 줄 레이아웃 inflate해서 ViewHolder 만듦
     */
    @NonNull
    @Override
    public LogVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.main_teams_members, parent, false);
        return new LogVH(v);
    }

    /**
     * 로그 메시지·생성 시각(상대 시간) 뷰에 바인딩함
     */
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

    /**
     * 팀 로그 한 줄 뷰 들고 있는 ViewHolder임
     */
    static class LogVH extends RecyclerView.ViewHolder {
        TextView messageTv;
        TextView timeTv;

        /**
         * 레이아웃에서 메시지·시간 TextView findViewById로 연결함
         */
        LogVH(@NonNull View itemView) {
            super(itemView);
            messageTv = itemView.findViewById(R.id.teams_log_message);
            timeTv = itemView.findViewById(R.id.teams_log_time);
        }
    }
}

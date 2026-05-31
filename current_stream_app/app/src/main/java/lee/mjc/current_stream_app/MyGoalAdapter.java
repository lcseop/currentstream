package lee.mjc.current_stream_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// 메인 화면 '내 작업' 진행/완료 목록 RecyclerView Adapter
public class MyGoalAdapter extends RecyclerView.Adapter<MyGoalAdapter.GoalVH> {

    public interface Listener {
        void onGoalClick(GoalItem item);
    }

    private final List<GoalItem> goals;
    private final boolean completedSection;
    private final Listener listener;

    // 목표 목록, 완료 섹션 여부, 클릭 리스너로 어댑터 생성
    public MyGoalAdapter(List<GoalItem> goals, boolean completedSection, Listener listener) {
        this.goals = goals;
        this.completedSection = completedSection;
        this.listener = listener;
    }

    @NonNull
    @Override
    // 목표 항목 ViewHolder 생성
    public GoalVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.main_item_my_task, parent, false);
        return new GoalVH(v, completedSection);
    }

    @Override
    // 목표 텍스트, 마감일, 클릭 이벤트 바인딩
    public void onBindViewHolder(@NonNull GoalVH holder, int position) {
        GoalItem item = goals.get(position);
        holder.nameTv.setText(item.goalText);

        if (!completedSection && holder.deadlineTv != null) {
            holder.deadlineTv.setText(DateTimeUtil.formatRemainingDays(item.goalEndDate));
            if (DateTimeUtil.isGoalOverdue(item.goalEndDate)) {
                holder.deadlineTv.setTextColor(0xFFE53935);
            } else {
                holder.deadlineTv.setTextColor(0xFF888888);
            }
        } else if (completedSection && holder.deadlineTv != null) {
            holder.deadlineTv.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGoalClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    static class GoalVH extends RecyclerView.ViewHolder {
        TextView nameTv;
        TextView deadlineTv;

        // ViewHolder 뷰 연결 (완료 섹션이면 마감일 숨김)
        GoalVH(View itemView, boolean completedSection) {
            super(itemView);
            nameTv = itemView.findViewById(R.id.item_task_name);
            deadlineTv = itemView.findViewById(R.id.item_task_deadline);
            if (completedSection && deadlineTv != null) {
                deadlineTv.setVisibility(View.GONE);
            }
        }
    }
}

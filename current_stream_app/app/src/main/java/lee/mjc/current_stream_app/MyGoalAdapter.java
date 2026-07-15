package lee.mjc.current_stream_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * 메인 화면 '내 작업' 진행/완료 목표 목록 RecyclerView 어댑터임
 * completedSection 플래그로 같은 어댑터 재사용하면서 마감일·글자색만 구분함
 */
public class MyGoalAdapter extends RecyclerView.Adapter<MyGoalAdapter.GoalVH> {

    /**
     * 목표 행 눌렀을 때 상세로 가려고 상위에 알려주는 콜백임
     */
    public interface Listener {
        void onGoalClick(GoalItem item);
    }

    private final List<GoalItem> goals;
    private final boolean completedSection;
    private final Listener listener;

    /**
     * 목표 리스트·완료 섹션 여부·클릭 리스너 받아서 만듦
     */
    public MyGoalAdapter(List<GoalItem> goals, boolean completedSection, Listener listener) {
        this.goals = goals;
        this.completedSection = completedSection;
        this.listener = listener;
    }

    /**
     * 내 작업 한 줄 레이아웃 inflate해서 ViewHolder 만듦
     */
    @NonNull
    @Override
    public GoalVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.main_item_my_task, parent, false);
        return new GoalVH(v, completedSection);
    }

    /**
     * 목표 텍스트·마감일·색 바인딩하고 클릭 이벤트 연결함
     * 완료 섹션이면 초록색+마감일 숨김, 진행 중이면 기한 지났으면 빨간색
     */
    @Override
    public void onBindViewHolder(@NonNull GoalVH holder, int position) {
        GoalItem item = goals.get(position);
        holder.nameTv.setText(item.goalText);
        if (completedSection) {
            holder.nameTv.setTextColor(0xFF1E7134);
        } else {
            holder.nameTv.setTextColor(0xFF333333);
        }

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

    /**
     * 내 작업 목표 한 줄 뷰 들고 있는 ViewHolder임
     */
    static class GoalVH extends RecyclerView.ViewHolder {
        TextView nameTv;
        TextView deadlineTv;

        /**
         * 레이아웃에서 목표명·마감일 TextView 연결함
         * 완료 섹션이면 생성 시점에 마감일 뷰 숨김
         */
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

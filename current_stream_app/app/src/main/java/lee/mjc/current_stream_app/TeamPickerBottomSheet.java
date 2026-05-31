package lee.mjc.current_stream_app;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

// 팀 선택 바텀시트 (팀 전환, 새 팀 만들기)
public final class TeamPickerBottomSheet {

    public interface OnTeamSelectedListener {
        void onTeamSelected(TeamItem team);
    }

    private TeamPickerBottomSheet() {
    }

    // 팀 목록 바텀시트 표시 (팀 선택, 새 팀 만들기)
    public static void show(
            AppCompatActivity activity,
            List<TeamItem> teams,
            @Nullable Long selectedTeamId,
            OnTeamSelectedListener listener,
            Runnable onCreateTeamClick
    ) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.main_bottom_sheet_teams, null);
        dialog.setContentView(view);

        RecyclerView rvTeams = view.findViewById(R.id.teams_recycler);
        Button createTeamBtn = view.findViewById(R.id.teams_sheet_create_btn);
        rvTeams.setLayoutManager(new LinearLayoutManager(activity));

        if (teams == null || teams.isEmpty()) {
            rvTeams.setVisibility(View.GONE);
        } else {
            rvTeams.setVisibility(View.VISIBLE);
            rvTeams.setAdapter(new TeamPickerAdapter(teams, selectedTeamId, team -> {
                if (listener != null) {
                    listener.onTeamSelected(team);
                }
                dialog.dismiss();
            }));
        }

        createTeamBtn.setOnClickListener(v -> {
            dialog.dismiss();
            if (onCreateTeamClick != null) {
                onCreateTeamClick.run();
            } else {
                activity.startActivity(new Intent(activity, CreateTeamActivity.class));
            }
        });

        dialog.show();
    }

    // 서버에서 팀 목록 불러온 뒤 바텀시트 표시
    public static void loadAndShow(
            AppCompatActivity activity,
            @Nullable Long selectedTeamId,
            OnTeamSelectedListener listener,
            Runnable onCreateTeamClick
    ) {
        String uid = SessionManager.getInstance().getUid();
        if (uid == null || uid.isEmpty()) {
            CommonDialog.showError(activity, "로그인 정보가 없습니다.");
            return;
        }

        Request request = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/team")
                .get()
                .addHeader("uid", uid)
                .build();

        ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                activity.runOnUiThread(() ->
                        CommonDialog.showError(activity, "팀 목록을 불러오지 못했습니다.")
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                activity.runOnUiThread(() -> {
                    if (!response.isSuccessful()) {
                        CommonDialog.showError(activity, "팀 목록을 불러오지 못했습니다.");
                        return;
                    }
                    try {
                        JSONObject root = new JSONObject(body);
                        if (!ApiHelper.isSuccess(root)) {
                            CommonDialog.showError(
                                    activity,
                                    ApiHelper.getMessage(root, "팀 목록을 불러오지 못했습니다.")
                            );
                            return;
                        }
                        List<TeamItem> teams = TeamItem.parseList(body);
                        show(activity, teams, selectedTeamId, listener, onCreateTeamClick);
                    } catch (Exception e) {
                        CommonDialog.showError(activity, "팀 목록을 처리하지 못했습니다.");
                    }
                });
            }
        });
    }

    private static class TeamPickerAdapter extends RecyclerView.Adapter<TeamPickerAdapter.TeamVH> {
        private final List<TeamItem> items;
        private final Long selectedTeamId;
        private final OnTeamSelectedListener listener;

        // 팀 목록 어댑터 생성
        TeamPickerAdapter(List<TeamItem> items, Long selectedTeamId, OnTeamSelectedListener listener) {
            this.items = items;
            this.selectedTeamId = selectedTeamId;
            this.listener = listener;
        }

        @Override
        // 팀 항목 ViewHolder 생성
        public TeamVH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.main_bottom_sheet_team_item, parent, false);
            return new TeamVH(v);
        }

        @Override
        // 팀 이름, D-day, 선택 뱃지 바인딩
        public void onBindViewHolder(TeamVH holder, int position) {
            TeamItem item = items.get(position);
            boolean selected = selectedTeamId != null && selectedTeamId == item.id;
            holder.nameTv.setText(item.teamName);
            holder.deadlineTv.setText(DateTimeUtil.formatDday(item.endDate));
            holder.selectedBadge.setVisibility(selected ? View.VISIBLE : View.GONE);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTeamSelected(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class TeamVH extends RecyclerView.ViewHolder {
            TextView nameTv;
            TextView deadlineTv;
            TextView selectedBadge;

            // ViewHolder 뷰 연결
            TeamVH(View itemView) {
                super(itemView);
                nameTv = itemView.findViewById(R.id.item_team_name);
                deadlineTv = itemView.findViewById(R.id.item_team_deadline);
                selectedBadge = itemView.findViewById(R.id.item_team_selected);
            }
        }
    }
}

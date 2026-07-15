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

/**
 * 팀 선택 바텀시트 띄우고 현재 팀 바꾸기·새 팀 만들기 제공함
 * 이미 불러온 팀 목록 넘기거나 loadAndShow로 서버에서 GET /api/team 후 띄움
 */
public final class TeamPickerBottomSheet {

    /**
     * 바텀시트에서 팀 골랐을 때 상위 화면에 알려주는 콜백임
     */
    public interface OnTeamSelectedListener {
        void onTeamSelected(TeamItem team);
    }

    private TeamPickerBottomSheet() {
    }

    /**
     * 팀 목록 바텀시트 띄움
     * 목록 비어 있으면 RecyclerView 숨기고 새 팀 만들기 버튼만 보임
     */
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

    /**
     * GET /api/team 으로 내 팀 목록 불러온 뒤 바텀시트 띄움
     * uid 없으면 API 안 치고 로그인 에러 대화상자만 띄움
     */
    public static void loadAndShow(
            AppCompatActivity activity,
            @Nullable Long selectedTeamId,
            OnTeamSelectedListener listener,
            Runnable onCreateTeamClick
    ) {
        // [중요] API 호출 전 로그인 uid 확인 — 내 팀 목록은 uid 헤더 필수
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

        // [중요] GET /api/team — 로그인한 사용자가 속한 팀 목록 조회
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

    /**
     * 바텀시트 안 팀 목록 RecyclerView 어댑터임
     * 지금 선택된 팀에 뱃지 보여서 어느 팀 보고 있는지 알 수 있게 함
     */
    private static class TeamPickerAdapter extends RecyclerView.Adapter<TeamPickerAdapter.TeamVH> {
        private final List<TeamItem> items;
        private final Long selectedTeamId;
        private final OnTeamSelectedListener listener;

        /**
         * 팀 목록·현재 선택 teamId·선택 콜백 받아서 만듦
         */
        TeamPickerAdapter(List<TeamItem> items, Long selectedTeamId, OnTeamSelectedListener listener) {
            this.items = items;
            this.selectedTeamId = selectedTeamId;
            this.listener = listener;
        }

        /**
         * 팀 한 줄 레이아웃 inflate해서 ViewHolder 만듦
         */
        @Override
        public TeamVH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.main_bottom_sheet_team_item, parent, false);
            return new TeamVH(v);
        }

        /**
         * 팀 이름·D-day·선택 뱃지 바인딩하고 클릭하면 선택 콜백 호출함
         */
        @Override
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

        /**
         * 팀 선택 한 줄 뷰 들고 있는 ViewHolder임
         */
        static class TeamVH extends RecyclerView.ViewHolder {
            TextView nameTv;
            TextView deadlineTv;
            TextView selectedBadge;

            /**
             * 레이아웃에서 팀명·마감일·선택 뱃지 TextView 연결함
             */
            TeamVH(View itemView) {
                super(itemView);
                nameTv = itemView.findViewById(R.id.item_team_name);
                deadlineTv = itemView.findViewById(R.id.item_team_deadline);
                selectedBadge = itemView.findViewById(R.id.item_team_selected);
            }
        }
    }
}

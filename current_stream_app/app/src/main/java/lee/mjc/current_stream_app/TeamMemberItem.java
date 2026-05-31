package lee.mjc.current_stream_app;

import java.util.ArrayList;
import java.util.List;

public class TeamMemberItem {
    public long userId;
    public String name;
    public String tag;
    public String userColor;
    public boolean leader;
    public final List<TeamGoalItem> ongoingGoals = new ArrayList<>();
    public final List<TeamGoalItem> completedGoals = new ArrayList<>();
    public boolean ongoingExpanded = true;
    public boolean completedExpanded = true;

    public TeamMemberItem(long userId, String name, String tag, boolean leader, String userColor) {
        this.userId = userId;
        this.name = name;
        this.tag = tag;
        this.leader = leader;
        this.userColor = userColor;
    }

    public static List<TeamMemberItem> parseList(String body) throws Exception {
        List<TeamMemberItem> result = new ArrayList<>();
        org.json.JSONObject root = new org.json.JSONObject(body);
        org.json.JSONArray arr = root.optJSONArray("responseData");
        if (arr == null) return result;

        for (int i = 0; i < arr.length(); i++) {
            org.json.JSONObject obj = arr.getJSONObject(i);
            result.add(new TeamMemberItem(
                    obj.getLong("userId"),
                    obj.optString("name", ""),
                    obj.optString("tag", ""),
                    obj.optBoolean("leader", false),
                    obj.optString("userColor", "#E8E8E8")
            ));
        }
        return result;
    }
}

package lee.mjc.current_stream_app;

import java.util.ArrayList;
import java.util.List;

// 팀 멤버 한 명과 그 멤버의 목표 목록을 담는 모델
public class TeamMemberItem {
    public long userId;                              // DB user ID
    public String name;                              // 표시 이름
    public String tag;                               // 사용자 tag
    public String userColor;                         // 멤버 색상 (HEX)
    public boolean leader;                           // 팀장 여부
    public final List<TeamGoalItem> ongoingGoals = new ArrayList<>();    // 진행 중 목표
    public final List<TeamGoalItem> completedGoals = new ArrayList<>();  // 완료 목표
    public boolean ongoingExpanded = true;           // 진행 중 섹션 펼침
    public boolean completedExpanded = false;        // 완료 섹션 펼침

    // 팀 멤버 항목 생성
    public TeamMemberItem(long userId, String name, String tag, boolean leader, String userColor) {
        this.userId = userId;
        this.name = name;
        this.tag = tag;
        this.leader = leader;
        this.userColor = userColor;
    }

    // API 팀 멤버 JSON 응답을 TeamMemberItem 리스트로 파싱
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

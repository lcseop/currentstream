package lee.mjc.current_stream_app;

import java.util.ArrayList;
import java.util.List;

/**
 * 팀원 한 명이랑 그 사람 진행/완료 목표 목록 담는 모델임.
 * 펼침 상태도 들고 있어서 RecyclerView 스크롤해도 UI 유지함.
 */
public class TeamMemberItem {
    /** user ID */
    public long userId;
    /** 닉네임 */
    public String name;
    /** tag */
    public String tag;
    /** 팀원 색상 */
    public String userColor;
    /** 팀장 여부 */
    public boolean leader;
    /** 진행 중 목표 목록 */
    public final List<TeamGoalItem> ongoingGoals = new ArrayList<>();
    /** 완료 목표 목록 */
    public final List<TeamGoalItem> completedGoals = new ArrayList<>();
    /** 진행 중 섹션 펼침 여부 */
    public boolean ongoingExpanded = true;
    /** 완료 섹션 펼침 여부 */
    public boolean completedExpanded = false;

    /** 팀원 항목 생성함. 목표 목록은 빈 리스트로 시작 */
    public TeamMemberItem(long userId, String name, String tag, boolean leader, String userColor) {
        this.userId = userId;
        this.name = name;
        this.tag = tag;
        this.leader = leader;
        this.userColor = userColor;
    }

    /** 팀원 JSON 응답을 TeamMemberItem 리스트로 파싱함 */
    public static List<TeamMemberItem> parseList(String body) throws Exception {
        List<TeamMemberItem> result = new ArrayList<>();
        org.json.JSONObject root = new org.json.JSONObject(body);
        org.json.JSONArray arr = root.optJSONArray("responseData");
        // responseData 없으면 빈 리스트
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

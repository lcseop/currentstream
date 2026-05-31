package lee.mjc.current_stream_app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

// API 팀 목록 응답을 담는 모델
public class TeamItem {
    public final long id;           // 팀 ID
    public final String teamName;   // 팀 이름
    public final String endDate;    // 팀 목표 마감일
    public final long leaderId;     // 팀장 user ID

    // 팀 정보 항목 생성
    public TeamItem(long id, String teamName, String endDate, long leaderId) {
        this.id = id;
        this.teamName = teamName;
        this.endDate = endDate;
        this.leaderId = leaderId;
    }

    // API 팀 목록 JSON 응답을 TeamItem 리스트로 파싱
    public static List<TeamItem> parseList(String body) throws Exception {
        List<TeamItem> result = new ArrayList<>();
        JSONObject root = new JSONObject(body);
        JSONArray arr = root.optJSONArray("responseData");
        if (arr == null) return result;

        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            if (!obj.has("id")) continue;
            String endDate = obj.has("endDate") && !obj.isNull("endDate")
                    ? obj.optString("endDate", null)
                    : null;
            result.add(new TeamItem(
                    obj.getLong("id"),
                    obj.optString("teamName", "이름 없는 팀"),
                    endDate,
                    obj.optLong("leaderId", -1)
            ));
        }
        return result;
    }
}

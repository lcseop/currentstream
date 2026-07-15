package lee.mjc.current_stream_app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 팀 목록 응답 담는 팀 정보 모델임.
 * parseList로 JSON 배열을 TeamItem 리스트로 바꿈.
 */
public class TeamItem {
    /** 팀 ID */
    public final long id;
    /** 팀 이름 */
    public final String teamName;
    /** 팀 목표 마감일 (yyyy-MM-dd, null 가능) */
    public final String endDate;
    /** 팀장 user ID */
    public final long leaderId;

    /** 팀 정보 항목 생성함 */
    public TeamItem(long id, String teamName, String endDate, long leaderId) {
        this.id = id;
        this.teamName = teamName;
        this.endDate = endDate;
        this.leaderId = leaderId;
    }

    /** API 팀 목록 JSON을 TeamItem 리스트로 파싱함 */
    public static List<TeamItem> parseList(String body) throws Exception {
        List<TeamItem> result = new ArrayList<>();
        JSONObject root = new JSONObject(body);
        JSONArray arr = root.optJSONArray("responseData");
        // responseData 없으면 빈 리스트
        if (arr == null) return result;

        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            // id 없는 항목은 건너뜀
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

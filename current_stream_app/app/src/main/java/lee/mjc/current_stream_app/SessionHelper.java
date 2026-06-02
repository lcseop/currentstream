package lee.mjc.current_stream_app;

import org.json.JSONObject;

// 로그인 API 응답을 SessionManager에 반영하는 공통 유틸
public final class SessionHelper {

    private SessionHelper() {}

    // responseCode가 _ok이고 uid가 있을 때만 세션 저장
    public static boolean applyLoginResponse(String responseBody) {
        try {
            JSONObject root = new JSONObject(responseBody);
            if (!ApiHelper.isSuccess(root)) {
                return false;
            }

            JSONObject data = root.optJSONObject("responseData");
            if (data == null) {
                return false;
            }

            String uid = data.optString("uid", "");
            if (uid.isEmpty()) {
                return false;
            }

            SessionManager sm = SessionManager.getInstance();
            sm.setUid(uid);
            sm.setTag(data.optString("tag", ""));
            sm.setUserName(data.optString("name", ""));
            if (data.has("id") && !data.isNull("id")) {
                sm.setUserId(data.getLong("id"));
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}

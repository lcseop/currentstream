package lee.mjc.current_stream_app;

import org.json.JSONObject;

/**
 * 로그인 API 응답 JSON 파싱해서 SessionManager에 넣는 유틸임.
 * Splash·Login에서 똑같은 조건으로 세션 저장하게 맞춰둠.
 */
public final class SessionHelper {

    /** new 막음 */
    private SessionHelper() {}

    /**
     * [중요] 로그인 응답 파싱해서 세션에 유저 정보 저장함.
     * responseCode가 _ok이고 uid가 있을 때만 true.
     * uid 없이 세션 열면 이후 API가 401 나서 uid 꼭 검사함
     */
    public static boolean applyLoginResponse(String responseBody) {
        try {
            JSONObject root = new JSONObject(responseBody);
            // [중요] _ok 아니면 실패 응답이라 세션 저장 안 함
            if (!ApiHelper.isSuccess(root)) {
                return false;
            }

            JSONObject data = root.optJSONObject("responseData");
            // responseData 없으면 파싱 불가
            if (data == null) {
                return false;
            }

            String uid = data.optString("uid", "");
            // [중요] uid가 API 인증 헤더 핵심임. 없으면 로그인 처리 안 함
            if (uid.isEmpty()) {
                return false;
            }

            SessionManager sm = SessionManager.getInstance();
            sm.setUid(uid);
            sm.setTag(data.optString("tag", ""));
            sm.setUserName(data.optString("name", ""));
            // id 있으면 DB user id도 저장
            if (data.has("id") && !data.isNull("id")) {
                sm.setUserId(data.getLong("id"));
            }
            return true;
        } catch (Exception ignored) {
            // JSON 깨지면 로그인 실패로 봄
            return false;
        }
    }
}

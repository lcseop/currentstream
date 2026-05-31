package lee.mjc.current_stream_app;

import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;

// 백엔드 API 호출에 공통으로 쓰는 HTTP 클라이언트·응답 처리 유틸
public final class ApiHelper {

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final OkHttpClient CLIENT = new OkHttpClient();

    private ApiHelper() {
    }

    // responseCode가 insert_ok, select_ok 같은 형식이면 true
    public static boolean isSuccess(JSONObject root) {
        return root.optString("responseCode", "").endsWith("_ok");
    }

    // 서버 message가 없으면 fallback 문구 사용
    public static String getMessage(JSONObject root, String fallback) {
        String message = root.optString("message", "");
        return message.isEmpty() ? fallback : message;
    }

    // JSON body에 넣을 문자열 이스케이프
    public static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // uid 헤더가 필요한 GET/POST 등에 쓰는 Request.Builder
    public static Request.Builder uidRequest(String url) {
        String uid = SessionManager.getInstance().getUid();
        return new Request.Builder()
                .url(url)
                .addHeader("uid", uid != null ? uid : "");
    }
}

package lee.mjc.current_stream_app;

import android.app.Activity;

import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;

// 백엔드 서버 호출할 때 공통으로 쓰는 HTTP 클라이언트, 응답 처리 유틸
public final class ApiHelper {

    // JSON MediaType static으로 저장
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    // OkHttpClient를 미리 static으로 할당
    public static final OkHttpClient CLIENT = new OkHttpClient();

    private ApiHelper() {}

    // optString과 endsWith 함수를 통해 responseCode가 insert_ok, select_ok 같은 형식이면 true 반환
    // getString과 달리 optString은 해당 문자열이 없으면 예외 대신에 빈 문자열(fallback)을 출력함.
    public static boolean isSuccess(JSONObject root) {
        return root.optString("responseCode", "").endsWith("_ok");
    }

    // 서버 message가 없으면 fallback 문구 사용
    public static String getMessage(JSONObject root, String fallback) {
        String message = root.optString("message", "");
        return message.isEmpty() ? fallback : message;
    }

    // JSON body에 넣을 문자열 이스케이프 함수
    public static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // uid 헤더가 필요한 GET/POST 등에 쓰는 Request.Builder
    // Request 클래스는 OkHttpClient에서 가져옴.
    public static Request.Builder uidRequest(String url) {
        String uid = SessionManager.getInstance().getUid();
        return new Request.Builder()
                .url(url)
                .addHeader("uid", uid != null ? uid : "");
    }

    // Activity가 종료 중이면 UI 콜백을 실행하지 않음 (WindowLeaked 방지)
    public static boolean isActivityAlive(Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    public static void runOnUiThreadSafe(Activity activity, Runnable action) {
        if (!isActivityAlive(activity) || action == null) {
            return;
        }
        activity.runOnUiThread(() -> {
            if (isActivityAlive(activity)) {
                action.run();
            }
        });
    }
}

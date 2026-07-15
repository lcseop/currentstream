package lee.mjc.current_stream_app;

import android.app.Activity;

import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * API 호출할 때 자주 쓰는 HTTP 도우미임.
 * OkHttp, JSON, uid 세션 헤더, UI 스레드 처리를 한곳에 모아둠.
 */
public final class ApiHelper {

    /** JSON 요청·응답 본문 타입 */
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /** 앱 전역에서 같이 쓰는 OkHttpClient */
    public static final OkHttpClient CLIENT = new OkHttpClient();

    /** 유틸 클래스라 new 막음 */
    private ApiHelper() {}

    /**
     * [중요] 서버 응답이 성공인지 봄.
     * responseCode가 _ok로 끝나야 성공으로 처리함.
     * 이거 안 맞으면 세션 저장·화면 갱신 하면 안 됨
     */
    public static boolean isSuccess(JSONObject root) {
        return root.optString("responseCode", "").endsWith("_ok");
    }

    /** 서버 message 읽어옴. 비어 있으면 기본 문구 씀 */
    public static String getMessage(JSONObject root, String fallback) {
        String message = root.optString("message", "");
        return message.isEmpty() ? fallback : message;
    }

    /** JSON 문자열 안 특수문자 이스케이프함. 수동 JSON 조립할 때 파싱 안 깨지게 */
    public static String escapeJson(String value) {
        // null이면 빈 문자열로 처리
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * [중요] uid 헤더 넣은 API 요청 빌더 만듦.
     * 백엔드가 이 헤더로 로그인한 유저를 구분함.
     * uid 없으면 401 나서 SessionManager에서 꼭 꺼내 넣음
     */
    public static Request.Builder uidRequest(String url) {
        String uid = SessionManager.getInstance().getUid();
        return new Request.Builder()
                .url(url)
                .addHeader("uid", uid != null ? uid : "");
    }

    /** Activity가 아직 살아있는지 확인함. finish 됐으면 다이얼로그 띄우면 안 됨 */
    public static boolean isActivityAlive(Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    /** Activity 살아있을 때만 UI 스레드에서 실행함 */
    public static void runOnUiThreadSafe(Activity activity, Runnable action) {
        // Activity나 action 없으면 그냥 안 함
        if (!isActivityAlive(activity) || action == null) {
            return;
        }
        activity.runOnUiThread(() -> {
            // [중요] 큐에 올린 뒤 실행 시점엔 Activity가 이미 finish 됐을 수 있음
            // 그때 Toast·다이얼로그 띄우면 WindowLeaked 남
            if (isActivityAlive(activity)) {
                action.run();
            }
        });
    }
}

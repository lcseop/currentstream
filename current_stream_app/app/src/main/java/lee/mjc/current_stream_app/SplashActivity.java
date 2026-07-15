package lee.mjc.current_stream_app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 앱 켜질 때 맨 처음 뜨는 화면임
 * Firebase 세션 있으면 토큰 갱신 후 서버 자동 로그인 시도함
 */
public class SplashActivity extends AppCompatActivity {

    /**
     * Firebase 로그인 여부 확인하고 자동 로그인 또는 로그인 화면으로 보냄
     * 레이아웃 없이 인증 분기만 해서 cold start 지연 줄임
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Firebase에 저장된 현재 사용자 가져옴
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            // --- Firebase 세션 있음: 토큰 갱신 후 서버 연동 ---
            // [중요] getIdToken(true)로 만료된 ID 토큰을 강제 갱신함
            // 서버 /api/user/login은 최신 JWT만 검증하니까 갱신 안 하면 401 날 수 있음
            // 갱신 성공하면 SessionManager에 토큰 넣고 백엔드 자동 로그인 호출함
            user.getIdToken(true).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // 갱신된 토큰 꺼내서 메모리 세션에 저장함
                    String idToken = task.getResult().getToken();
                    SessionManager.getInstance().setIdToken(idToken);
                    sendTokenToServer(idToken);
                } else {
                    // 토큰 갱신 실패면 세션 깨진 거라 로그인 화면으로 보냄
                    moveToLogin();
                }
            });
        } else {
            // --- Firebase 미로그인 ---
            // 저장된 계정 없으면 바로 로그인 화면으로 감
            moveToLogin();
        }
    }

    /**
     * Firebase ID 토큰을 POST /api/user/login으로 보내서 백엔드 세션(uid 등) 맞춤
     */
    private void sendTokenToServer(String idToken) {
        // 서버가 기대하는 JSON body 만듦
        String json = "{\"idToken\":\"" + idToken + "\"}";

        RequestBody body = RequestBody.create(json, ApiHelper.JSON);

        // 로그인 API 요청 객체 조립함
        Request request = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/user/login")
                .post(body)
                .build();

        // 네트워크 콜은 백그라운드 스레드에서 돌아감
        ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 네트워크 끊기면 자동 로그인 포기하고 로그인 화면으로 감
                moveToLogin();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 응답 body 문자열로 읽어둠 (null 방어)
                String responseBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    // [중요] HTTP 2xx이고 SessionHelper가 uid까지 파싱 성공해야 메인 진입함
                    // SessionHelper.applyLoginResponse가 JSON에서 userId, name 등 꺼내 SessionManager에 넣음
                    // 파싱 실패나 4xx면 Firebase만 로그인된 불완전 상태라 로그인 화면으로 돌려보냄
                    if (response.isSuccessful() && SessionHelper.applyLoginResponse(responseBody)) {
                        moveToMain();
                    } else {
                        moveToLogin();
                    }
                });
            }
        });
    }

    /**
     * 로그인·세션 확립 성공 시 메인 화면으로 이동하고 Splash 종료함
     */
    private void moveToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    /**
     * 자동 로그인 실패·미로그인 시 LoginActivity로 보냄
     * Firebase signOut + SessionManager.clear로 찌꺼기 세션 싹 지움
     */
    private void moveToLogin() {
        // Firebase 로컬 세션 끊음
        FirebaseAuth.getInstance().signOut();
        // 앱 메모리에 남은 토큰·uid 등도 비움
        SessionManager.getInstance().clear();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}

package lee.mjc.current_stream_app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

// 앱의 시작을 알리는 액티비티
public class SplashActivity extends AppCompatActivity {

    // Firebase 로그인 상태 확인 후 자동 로그인 또는 로그인 화면 이동
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            // 이미 로그인 상태라면 서버 자동 로그인
            user.getIdToken(true).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String idToken = task.getResult().getToken();
                    SessionManager.getInstance().setIdToken(idToken);
                    sendTokenToServer(idToken);
                } else {
                    moveToLogin();
                }
            });
        } else {
            // 로그인 안됨
            moveToLogin();
        }
    }

    // Firebase 토큰을 서버에 보내 자동 로그인 시도
    private void sendTokenToServer(String idToken) {
        String json = "{\"idToken\":\"" + idToken + "\"}";

        RequestBody body = RequestBody.create(json, ApiHelper.JSON);

        Request request = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/user/login")
                .post(body)
                .build();

        ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                moveToLogin();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        saveUserInfoFromLogin(responseBody);
                        moveToMain();
                    } else {
                        moveToLogin();
                    }
                });
            }
        });
    }

    // 로그인 응답에서 uid, tag, userId를 SessionManager에 저장
    private void saveUserInfoFromLogin(String responseBody) {
        try {
            JSONObject root = new JSONObject(responseBody);
            JSONObject data = root.optJSONObject("responseData");
            if (data == null) return;

            SessionManager sm = SessionManager.getInstance();
            sm.setUid(data.optString("uid", ""));
            sm.setTag(data.optString("tag", ""));
            if (data.has("id") && !data.isNull("id")) {
                sm.setUserId(data.getLong("id"));
            }
        } catch (Exception ignored) {
        }
    }

    // 로그인 성공 시 MainActivity로 이동
    private void moveToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    // 로그인 실패·미로그인 시 LoginActivity로 이동
    private void moveToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}

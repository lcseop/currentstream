package lee.mjc.current_stream_app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// 앱의 시작을 알리는 액티비티
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            // 이미 로그인 상태라면 서버 자동 로그인
            user.getIdToken(true).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String idToken = task.getResult().getToken();
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

    private void sendTokenToServer(String idToken) {
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();

        String json = "{\"idToken\":\"" + idToken + "\"}";

        okhttp3.RequestBody body = okhttp3.RequestBody.create(
                json,
                okhttp3.MediaType.parse("application/json")
        );

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("http://10.0.2.2:8080/api/user/login")
                .post(body)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                moveToLogin();
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                runOnUiThread(() -> moveToMain());
            }
        });
    }

    private void moveToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void moveToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}

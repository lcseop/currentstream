package lee.mjc.current_stream_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.net.HttpCookie;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    EditText loginIdEdit, loginPwEdit;
    Button loginBtn, regiBtn;
    ImageButton googleBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginIdEdit = findViewById(R.id.login_id_edit);
        loginPwEdit = findViewById(R.id.login_pw_edit);
        loginBtn = findViewById(R.id.login_button);
        regiBtn = findViewById(R.id.login_register_btn);
        googleBtn = findViewById(R.id.login_google_button);


        // Firebase 로그인 버튼 클릭 시 백엔드로 토큰 검사 보냄
        loginBtn.setOnClickListener((v) -> {
            String email = loginIdEdit.getText().toString();
            String password = loginPwEdit.getText().toString();
            if (email.isEmpty() || password.isEmpty()) return;
            // Firebase 로그인
            FirebaseAuth.getInstance()
                    // 이메일, 비밀번호 기입
                    .signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                            if (user == null) return;

                            user.getIdToken(true).addOnCompleteListener(tokenTask -> {
                                if (tokenTask.isSuccessful()) {
                                    String idToken = tokenTask.getResult().getToken();
                                    // 세션 싱글톤 클래스에 로그인 정보 저장
                                    SessionManager.getInstance().setIdToken(idToken);
                                    sendTokenToServer(idToken);
                                } else {
                                    Log.e("LOGIN", "토큰 발급 실패");
                                }
                                
                            });
                        } else {
                            // 로그인 실패 대화상자 넣어야 함
                            Log.e("LOGIN", "로그인 실패");
                        }
                    });
        });

    }

    // 로그인 정보를 토큰을 통해 서버로 보내는 메소드
    private void sendTokenToServer(String idToken) {

        // OkHttp를 이용해 서버로 보내기 위해 객체 생성
        OkHttpClient client = new OkHttpClient();
        // token을 json 형태로 담음
        String json = "{\"idToken\":\"" + idToken + "\"}";
        // json을 requestBody로 변환
        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json")
        );
        // requestBody를 url을 통해 post 메소드로 요청함
        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/api/user/login")
                .post(body)
                .build();

        // 요청 받은 응답을 콜백으로 받음
        client.newCall(request).enqueue(new Callback() {
            // 실패 시
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("API", "서버 요청 : 실패", e);
            }
            
            // 성공 시 메인으로 액티비티 이동
            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String responseBody = response.body().string();
                Log.d("API", "code=" + response.code());
                Log.d("API", responseBody);

                if (response.isSuccessful()) {
                    runOnUiThread(() -> moveToMain());
                } else {
                    Log.e("API", "서버 : 실패");
                }
            }
        });
    }

    // MainActivity로 Intent 이동
    private void moveToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }


}
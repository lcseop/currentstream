package lee.mjc.current_stream_app;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {

    EditText editId, editPw, editPwCheck, editName;
    TextView idWarn, pwWarn, pwCheckWarn, nameWarn;
    Button registerBtn;
    ImageButton backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 뷰 변수에 할당
        editId = findViewById(R.id.register_id_edit);
        editPw = findViewById(R.id.register_pw_edit);
        editPwCheck = findViewById(R.id.register_pw_check_edit);
        editName = findViewById(R.id.register_name_edit);
        idWarn = findViewById(R.id.register_id_warn);
        pwWarn = findViewById(R.id.register_pw_warn);
        pwCheckWarn = findViewById(R.id.register_pw_check_warn);
        nameWarn = findViewById(R.id.register_name_warn);
        registerBtn = findViewById(R.id.register_btn);
        backBtn = findViewById(R.id.register_header_back);

        editId.setText(getIntent().getStringExtra("email"));

        // 뒤로가기 버튼 클릭 시 액티비티 종료
        backBtn.setOnClickListener(v -> finish());

        registerBtn.setOnClickListener(v -> {

            String email = editId.getText().toString();
            String pw = editPw.getText().toString();
            String pwCheck = editPwCheck.getText().toString();
            String name = editName.getText().toString();
            
            // EditText에 빈 칸 있으면 예외 처리
            if (email.isEmpty() || pw.isEmpty() || pwCheck.isEmpty() || name.isEmpty()) {
                return;
            }
            
            // 비밀번호, 비밀번호 확인이 일치하지 않으면 예외 처리
            if (!pw.equals(pwCheck)) {
                return;
            }

            FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email, pw)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {

                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                            if (user == null) return;
                            
                            // 이메일로 인증 메일 발송
                            user.sendEmailVerification()
                                    .addOnCompleteListener(mailTask -> {
                                        if (mailTask.isSuccessful()) {
                                            Log.d("EMAIL", "인증 메일 발송 완료");
                                        } else {
                                            Log.e("EMAIL", "인증 메일 실패");
                                        }
                                    });

                            user.getIdToken(true).addOnSuccessListener(result -> {
                                String idToken = result.getToken();
                                Log.d("TOKEN", "토큰 발급 성공");
                                sendSignupToServer(idToken, name);
                            });

                        } else {
                            Log.e("LOGIN", "회원가입 실패");
                        }
                    });
        });

    }

    private void sendSignupToServer(String idToken, String name) {
        // OkHttp를 이용해 서버로 보내기 위해 객체 생성
        OkHttpClient client = new OkHttpClient();
        // token과 name을 json 형태로 담음 (name은 firebase가 아닌 DB에 따로 저장됨)
        String json = "{"
                + "\"idToken\":\"" + idToken + "\","
                + "\"name\":\"" + name + "\""
                + "}";
        
        // json 설정
        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json")
        );

        // requestBody를 url을 통해 post 메소드로 요청함
        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/api/user/signup")
                .post(body)
                .build();

        // 요청 받은 응답을 콜백으로 받음
        client.newCall(request).enqueue(new Callback() {
            
            // 실패 시
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("API", "서버 응답 실패 : " + e);
            }

            // 성공 시 로그인 액티비티로 다시 이동
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("API", "signup code=" + response.code());
                if (response.isSuccessful()) {
                    runOnUiThread(() -> finish());
                }
            }
        });
    }

}
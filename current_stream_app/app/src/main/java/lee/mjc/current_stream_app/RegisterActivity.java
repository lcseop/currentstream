package lee.mjc.current_stream_app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.internal.service.Common;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
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

    FrameLayout loadingOverlay;
    EditText editId, editPw, editPwCheck, editName;
    TextView idWarn, pwWarn, pwCheckWarn, nameWarn;
    Button registerBtn;
    ImageButton backBtn;

    boolean[] check = {false, false, false, false};

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
        loadingOverlay = findViewById(R.id.loading_overlay);

        editId.setText(getIntent().getStringExtra("email"));

        editId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
                String text = s.toString();

                if (s.length() < 1) {
                    idWarn.setText("이메일을 입력해주세요.");
                    idWarn.setVisibility(View.VISIBLE);
                    check[0] = false;
                } else if (! text.matches(emailPattern)) {
                    idWarn.setText("이메일 형식으로 입력해주세요.");
                    idWarn.setVisibility(View.VISIBLE);
                    check[0] = false;
                } else {
                    idWarn.setVisibility(View.INVISIBLE);
                    check[0] = true;
                }

                updateSignupButton();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        editPw.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString();

                if (s.length() < 1) {
                    pwWarn.setText("비밀번호를 입력해주세요.");
                    pwWarn.setVisibility(View.VISIBLE);
                    check[1] = false;
                } else if (s.length() < 8 || s.length() > 20) {
                    pwWarn.setText("비밀번호는 8~20자로 입력해주세요.");
                    pwWarn.setVisibility(View.VISIBLE);
                    check[1] = false;
                } else {
                    pwWarn.setVisibility(View.INVISIBLE);
                    check[1] = true;
                }

                updateSignupButton();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        editPwCheck.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString();

                if (!text.equals(editPw.getText().toString())) {
                    pwCheckWarn.setText("비밀번호가 일치하지 않아요.");
                    pwCheckWarn.setVisibility(View.VISIBLE);
                    check[2] = false;
                } else if (text.isEmpty()) {
                    pwCheckWarn.setVisibility(View.INVISIBLE);
                    check[2] = false;
                } else {
                    pwCheckWarn.setVisibility(View.INVISIBLE);
                    check[2] = true;
                }

                updateSignupButton();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        editName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString();

                if (s.length() < 1) {
                    nameWarn.setText("닉네임을 입력해주세요.");
                    nameWarn.setVisibility(View.VISIBLE);
                    check[3] = false;
                } else if (s.length() < 2 || s.length() > 50) {
                    nameWarn.setText("닉네임은 2~50자로 입력해주세요.");
                    nameWarn.setVisibility(View.VISIBLE);
                    check[3] = false;
                } else {
                    nameWarn.setVisibility(View.INVISIBLE);
                    check[3] = true;
                }

                updateSignupButton();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

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
                        runOnUiThread(() -> {
                            loadingOverlay.setVisibility(View.VISIBLE);
                        });
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
                            loadingOverlay.setVisibility(View.GONE);
                            Exception e = task.getException();

                            if (e instanceof FirebaseAuthUserCollisionException) {
                                runOnUiThread(() -> {
                                    CommonDialog dig = new CommonDialog(RegisterActivity.this, "이미 가입된 이메일입니다.", "확인");
                                    dig.setOnConfirmListener(view -> {
                                        dig.dismiss();
                                    });
                                    dig.show();
                                });
                            } else {
                                runOnUiThread(() -> {
                                    loadingOverlay.setVisibility(View.GONE);
                                    CommonDialog dig = new CommonDialog(RegisterActivity.this, "알 수 없는 이유로 회원가입이 실패했어요.", "확인");
                                    dig.setOnConfirmListener(view -> {
                                        dig.dismiss();
                                    });
                                    dig.show();
                                });
                            }
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
                runOnUiThread(() -> {
                    loadingOverlay.setVisibility(View.GONE);
                    CommonDialog dig = new CommonDialog(RegisterActivity.this, "서버와 응답이 되지 않습니다.", "확인");
                    dig.setOnConfirmListener(view -> {
                        dig.dismiss();
                    });
                    dig.show();
                });
                Log.e("API", "서버 응답 실패 : " + e);
            }

            // 성공 시 이메일 인증 안내 후, ㅅ로그인 액티비티로 다시 이동
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("API", "signup code=" + response.code());
                runOnUiThread(() -> {
                    if (!response.isSuccessful()) {
                        loadingOverlay.setVisibility(View.GONE);
                        return;
                    }
                    loadingOverlay.setVisibility(View.GONE);
                    CommonDialog dig = new CommonDialog(RegisterActivity.this, "이메일로 인증 메일이 발송되었습니다. 링크 클릭 후 로그인 해주세요.", "확인");
                    dig.setOnConfirmListener(v -> {
                        dig.dismiss();
                        finish();
                    });
                    dig.show();
                });

            }
        });
    }

    private void updateSignupButton() {

        for (boolean b : check) {
            if (!b) {
                registerBtn.setEnabled(false);
                return;
            }
        }

        registerBtn.setEnabled(true);
    }

}
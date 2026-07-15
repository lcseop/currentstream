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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 회원가입 화면임
 * Firebase 계정 만들고 → 인증 메일 보내고 → idToken으로 백엔드 signup 호출하는 2단계 흐름임
 */
public class RegisterActivity extends AppCompatActivity {

    // 로딩 오버레이
    FrameLayout loadingOverlay;
    // 입력 필드들
    EditText editId, editPw, editPwCheck, editName;
    // 경고 문구 TextView들
    TextView idWarn, pwWarn, pwCheckWarn, nameWarn;
    // 가입·뒤로가기 버튼
    Button registerBtn;
    ImageButton backBtn;

    /** 0: 이메일, 1: 비밀번호, 2: 비밀번호 확인, 3: 닉네임 — 각각 통과 여부 */
    boolean[] check = {false, false, false, false};

    /**
     * 회원가입 화면 초기화하고 입력 검증·Firebase 가입 버튼 연결함
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // --- 뷰 바인딩 ---
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

        // 로그인 화면에서 넘어온 이메일 있으면 미리 채워둠
        editId.setText(getIntent().getStringExtra("email"));

        // --- 이메일 형식 실시간 검증 ---
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

        // --- 비밀번호 길이 실시간 검증 ---
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

        // --- 비밀번호 확인 일치 검증 ---
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

        // --- 닉네임 길이 실시간 검증 ---
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

        // 뒤로가기 누르면 이 화면 닫음
        backBtn.setOnClickListener(v -> finish());

        // --- 가입 버튼 클릭 ---
        registerBtn.setOnClickListener(v -> {

            String email = editId.getText().toString();
            String pw = editPw.getText().toString();
            String pwCheck = editPwCheck.getText().toString();
            String name = editName.getText().toString();
            
            // 빈 값이면 리턴함 (버튼 비활성화돼 있긴 한데 이중 체크)
            if (email.isEmpty() || pw.isEmpty() || pwCheck.isEmpty() || name.isEmpty()) {
                return;
            }
            
            // 비밀번호 불일치면 리턴함
            if (!pw.equals(pwCheck)) {
                return;
            }

            // [중요] Firebase createUserWithEmailAndPassword로 Auth 계정 먼저 만듦
            // 성공하면 자동 로그인 상태가 되고, 그 다음 이메일 인증·서버 signup 순서로 감
            // Firebase만 성공하고 서버 실패해도 Auth에는 계정 남아 있을 수 있음
            FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email, pw)
                    .addOnCompleteListener(task -> {
                        runOnUiThread(() -> {
                            loadingOverlay.setVisibility(View.VISIBLE);
                        });
                        if (task.isSuccessful()) {

                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                            if (user == null) return;
                            
                            // [중요] sendEmailVerification으로 인증 메일 발송함
                            // 로그인할 때 서버가 403 주는 건 이메일 미인증 때문임
                            // 메일 링크 클릭 전까지는 로그인 막히는 게 정상 흐름임
                            user.sendEmailVerification()
                                    .addOnCompleteListener(mailTask -> {
                                        if (mailTask.isSuccessful()) {
                                            Log.d("EMAIL", "인증 메일 발송 완료");
                                        } else {
                                            Log.e("EMAIL", "인증 메일 실패");
                                        }
                                    });

                            // [중요] getIdToken(true)로 백엔드 signup에 쓸 JWT 발급함
                            // 닉네임은 Firebase에 안 넣고 서버 DB에만 저장함
                            // 토큰 받은 뒤 sendSignupToServer로 name이랑 같이 POST함
                            user.getIdToken(true).addOnSuccessListener(result -> {
                                String idToken = result.getToken();
                                Log.d("TOKEN", "토큰 발급 성공");
                                sendSignupToServer(idToken, name);
                            });

                        } else {
                            loadingOverlay.setVisibility(View.GONE);
                            Exception e = task.getException();

                            // 이미 가입된 이메일이면 따로 안내함
                            if (e instanceof FirebaseAuthUserCollisionException) {
                                runOnUiThread(() -> {
                                    CommonDialog dig = new CommonDialog(RegisterActivity.this, "이미 가입된 이메일입니다.", "확인");
                                    dig.setOnConfirmListener(view -> {
                                        dig.dismiss();
                                    });
                                    dig.show();
                                });
                            } else {
                                // 그 외 Firebase 가입 실패
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

    /**
     * Firebase idToken이랑 닉네임을 POST /api/user/signup으로 보냄
     * 닉네임은 Firebase 말고 DB에 따로 저장함
     */
    private void sendSignupToServer(String idToken, String name) {
        // signup API용 JSON body 조립함
        String json = "{"
                + "\"idToken\":\"" + idToken + "\","
                + "\"name\":\"" + name + "\""
                + "}";

        RequestBody body = RequestBody.create(json, ApiHelper.JSON);

        Request request = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/user/signup")
                .post(body)
                .build();

        // [중요] POST /api/user/signup — 백엔드에 유저 row 생성함
        // 서버가 idToken 검증하고 name 넣어서 DB에 저장함
        // 성공해도 이메일 인증 전엔 로그인 안 되니까 안내 다이얼로그 띄우고 화면 닫음
        ApiHelper.CLIENT.newCall(request).enqueue(new Callback() {
            
            @Override
            public void onFailure(Call call, IOException e) {
                // 네트워크 실패 시 오버레이 끄고 에러 다이얼로그
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

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("API", "signup code=" + response.code());
                runOnUiThread(() -> {
                    // HTTP 실패면 오버레이만 끄고 끝냄
                    if (!response.isSuccessful()) {
                        loadingOverlay.setVisibility(View.GONE);
                        return;
                    }
                    loadingOverlay.setVisibility(View.GONE);
                    // 가입 성공 안내 — 인증 메일 확인하라고 알려줌
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

    /**
     * 이메일·비밀번호·닉네임 검증 다 통과했을 때만 가입 버튼 켬
     */
    private void updateSignupButton() {

        // check 배열 하나라도 false면 버튼 비활성화함
        for (boolean b : check) {
            if (!b) {
                registerBtn.setEnabled(false);
                return;
            }
        }

        // 전부 true면 가입 가능
        registerBtn.setEnabled(true);
    }

}

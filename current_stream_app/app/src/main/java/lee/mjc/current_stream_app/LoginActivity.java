package lee.mjc.current_stream_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import org.json.JSONObject;

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

    FrameLayout loadingOverlay;
    EditText loginIdEdit, loginPwEdit;
    Button loginBtn, regiBtn;
    ImageButton googleBtn;

    GoogleSignInClient googleSignInClient;

    // 구글 로그인 창 띄우고 결과를 받는 콜백
    private ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            Intent data = result.getData();
                            handleGoogleResult(data);
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 뷰 변수에 할당
        loginIdEdit = findViewById(R.id.login_id_edit);
        loginPwEdit = findViewById(R.id.login_pw_edit);
        loginBtn = findViewById(R.id.login_button);
        regiBtn = findViewById(R.id.login_register_btn);
        googleBtn = findViewById(R.id.login_google_button);
        loadingOverlay = findViewById(R.id.loading_overlay);

        // 회원가입 버튼 클릭
        regiBtn.setOnClickListener((v) -> {

            String email = loginIdEdit.getText().toString();

            Intent intent = new Intent(this, RegisterActivity.class);

            // 회원가입 화면으로 값 전달
            intent.putExtra("email", email);

            startActivity(intent);
        });


        // Firebase 로그인 버튼 클릭 시 백엔드로 토큰 검사 보냄
        loginBtn.setOnClickListener((v) -> {
            String email = loginIdEdit.getText().toString();
            String password = loginPwEdit.getText().toString();
            if (email.isEmpty() || password.isEmpty()) return;
            runOnUiThread(() -> {
                loadingOverlay.setVisibility(View.VISIBLE);
            });

            // Firebase 로그인
            FirebaseAuth.getInstance()
                    // 이메일, 비밀번호 기입
                    .signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        // 로그인 정상 작동되는지 확인
                        if (task.isSuccessful()) {
                            // Firebase에 로그인
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                            if (user == null) return;

                            // 현재 접속한 유저의 Firebase 토큰을 가져옴
                            user.getIdToken(true).addOnCompleteListener(tokenTask -> {
                                if (tokenTask.isSuccessful()) {
                                    String idToken = tokenTask.getResult().getToken();
                                    // 세션 싱글톤 클래스에 로그인 정보 저장
                                    Log.d("TOKEN", idToken);
                                    // 세션 매니저에 토큰을 저장하여 모든 액티비티에서 사용자 정보 사용
                                    SessionManager.getInstance().setIdToken(idToken);
                                    // 서버에 토큰을 보냄
                                    sendTokenToServer(idToken);
                                } else {
                                    Log.e("LOGIN", "토큰 발급 실패");
                                }
                                
                            });
                        } else {
                            // 예외에 각 로그인 오류를 받아 적절한 에러 메시지를 띄워줌
                            Exception e = task.getException();
                            runOnUiThread(() -> {
                                String errorMessage = "";
                                if (e instanceof FirebaseAuthInvalidUserException) {
                                    errorMessage = "가입되지 않은 이메일입니다.";
                                } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                    errorMessage = "이메일 또는 비밀번호가 틀렸습니다.";
                                } else if (e instanceof FirebaseTooManyRequestsException) {
                                    errorMessage = "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.";
                                } else {
                                    errorMessage = "알 수 없는 이유로 로그인에 실패했습니다.";
                                    Log.e("LOGIN", "로그인 실패", e);
                                }
                                loadingOverlay.setVisibility(View.GONE);
                                CommonDialog dig = new CommonDialog(LoginActivity.this, errorMessage, "확인");
                                dig.setOnConfirmListener(view -> {
                                    dig.dismiss();
                                });
                                dig.show();
                            });
                        }
                    });
        });

        // 구글 로그인 옵션을 담는 변수를 Builder 패턴으로 생성
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // ✅ 중요
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleBtn.setOnClickListener(v -> {
            runOnUiThread(() -> {
                loadingOverlay.setVisibility(View.VISIBLE);
            });
            Intent intent = googleSignInClient.getSignInIntent();
            googleLauncher.launch(intent);
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
                runOnUiThread(() -> {
                    loadingOverlay.setVisibility(View.GONE);
                    CommonDialog dig = new CommonDialog(LoginActivity.this, "서버와 응답이 되지 않습니다.", "확인");
                    dig.setOnConfirmListener(view -> {
                        dig.dismiss();
                    });
                    dig.show();
                });
            }
            
            // 성공 시 메인으로 액티비티 이동
            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String responseBody =
                        response.body() != null ? response.body().string() : "";

                int code = response.code();

                Log.d("API", "code=" + code);
                Log.d("API", responseBody);

                runOnUiThread(() -> {

                    loadingOverlay.setVisibility(View.GONE);
                    String errorMessage = "";

                    if (code >= 200 && code < 300) {
                        saveUserInfoFromLogin(responseBody);
                        moveToMain();
                    }
                    else if (code == 401) {
                        errorMessage = "로그인 인증이 만료되었습니다.";
                        FirebaseAuth.getInstance().signOut();
                    }
                    else if (code == 403) {
                        errorMessage = "이메일 인증이 필요합니다.";
                        FirebaseAuth.getInstance().signOut();
                    }
                    else if (code >= 500) {
                        errorMessage = "서버가 응답을 받지 않습니다.";
                    }
                    else {
                        errorMessage = "로그인 실패";
                    }
                    loadingOverlay.setVisibility(View.GONE);
                    if (! errorMessage.isEmpty()) {
                        CommonDialog dig = new CommonDialog(LoginActivity.this, errorMessage, "확인");
                        dig.setOnConfirmListener(view -> {
                            dig.dismiss();
                        });
                        dig.show();
                    }
                });
            }
        });
    }

    // 로그인 성공 시 MainActivity로 Intent 이동
    private void moveToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

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

    /**
     * 구글 로그인 결과를 처리하는 메소드
     * @param data
     */
    private void handleGoogleResult(Intent data) {
        // 구글 로그인 결과를 가져옴
        Task<GoogleSignInAccount> task =
                GoogleSignIn.getSignedInAccountFromIntent(data);

        // 구글 로그인 결과를 Firebase에 전달
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            Log.e("GOOGLE", "로그인 실패", e);
        }
    }

    /**
     * 구글 로그인 결과를 Firebase에 전달하는 메소드
     * @param idToken
     */
    private void firebaseAuthWithGoogle(String idToken) {

        // Google 로그인에서 받은 ID 토큰으로 Firebase 인증에 사용할 Credential 생성
        AuthCredential credential =
                GoogleAuthProvider.getCredential(idToken, null);
        // 생성한 Credential을 이용해서 Firebase 로그인 수행
        FirebaseAuth.getInstance()
                .signInWithCredential(credential)
                // Firebase 로그인 작업 완료 후 실행되는 콜백
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // 현재 로그인된 Firebase 사용자 객체 가져오기
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user == null) return;
                        user.getIdToken(true)
                                .addOnSuccessListener(result -> {
                                    // 실제 Firebase JWT 토큰 문자열 추출
                                    String firebaseToken = result.getToken();

                                    // 앱 내부 SessionManager에 토큰 저장
                                    SessionManager.getInstance().setIdToken(firebaseToken);

                                    // Spring 서버로 Firebase 토큰 전송
                                    sendTokenToServer(firebaseToken);
                                });
                    } else {
                        runOnUiThread(() -> {
                            loadingOverlay.setVisibility(View.GONE);
                            CommonDialog dig = new CommonDialog(LoginActivity.this, "구글 로그인에 실패했습니다.", "확인");
                            dig.setOnConfirmListener(view -> {
                                dig.dismiss();
                            });
                            dig.show();
                        });
                    }
                });
    }




}
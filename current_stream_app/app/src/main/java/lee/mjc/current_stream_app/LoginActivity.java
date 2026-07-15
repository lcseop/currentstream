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
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 이메일·비밀번호랑 Google 로그인 화면임
 * Firebase로 클라 인증한 뒤 ID 토큰을 Spring 서버에 넘겨서 앱 세션 맞춤
 */
public class LoginActivity extends AppCompatActivity {

    // 로딩 중 터치 막는 오버레이
    FrameLayout loadingOverlay;
    // 이메일·비밀번호 입력칸
    EditText loginIdEdit, loginPwEdit;
    // 로그인·회원가입 버튼
    Button loginBtn, regiBtn;
    // 구글 로그인 버튼
    ImageButton googleBtn;

    // Google Sign-In SDK 클라이언트
    GoogleSignInClient googleSignInClient;

    /**
     * Google Sign-In Intent 결과 받아서 handleGoogleResult로 넘김
     */
    private ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        // 구글 계정 선택 성공했을 때만 후처리함
                        if (result.getResultCode() == RESULT_OK) {
                            Intent data = result.getData();
                            handleGoogleResult(data);
                        }
                    }
            );

    /**
     * 로그인 UI 바인딩하고 버튼 리스너 등록함
     * GoogleSignInOptions는 Firebase 연동용 Web Client ID 필요함
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // --- 뷰 초기화 ---
        loginIdEdit = findViewById(R.id.login_id_edit);
        loginPwEdit = findViewById(R.id.login_pw_edit);
        loginBtn = findViewById(R.id.login_button);
        regiBtn = findViewById(R.id.login_register_btn);
        googleBtn = findViewById(R.id.login_google_button);
        loadingOverlay = findViewById(R.id.loading_overlay);

        // --- 회원가입 화면 이동 ---
        regiBtn.setOnClickListener((v) -> {
            // 로그인 화면에 써둔 이메일 넘겨서 회원가입 입력 편하게 함
            String email = loginIdEdit.getText().toString();

            Intent intent = new Intent(this, RegisterActivity.class);

            intent.putExtra("email", email);

            startActivity(intent);
        });


        // --- 이메일/비밀번호 로그인 ---
        loginBtn.setOnClickListener((v) -> {
            String email = loginIdEdit.getText().toString();
            String password = loginPwEdit.getText().toString();
            // 빈 칸이면 그냥 리턴함
            if (email.isEmpty() || password.isEmpty()) return;

            // [중요] loadingOverlay로 로그인 중 UI 잠금함
            // Firebase·서버 응답 올 때까지 중복 탭이나 입력 막는 용도임
            // 실패·성공 시 반드시 GONE으로 돌려놔야 함
            runOnUiThread(() -> {
                loadingOverlay.setVisibility(View.VISIBLE);
            });

            // Firebase 이메일/비밀번호 로그인 시도함
            FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                            // 유저 null이면 이상한 상태라 오버레이만 끄고 끝냄
                            if (user == null) {
                                runOnUiThread(() -> loadingOverlay.setVisibility(View.GONE));
                                return;
                            }

                            // [중요] Firebase 로그인만으로는 앱 세션 안 됨 — 서버 login API 필수임
                            // getIdToken(true)로 최신 JWT 받아서 SessionManager에 넣고 sendTokenToServer 호출함
                            // 서버가 uid·DB user id 파싱해줘야 MainActivity 갈 수 있음
                            user.getIdToken(true).addOnCompleteListener(tokenTask -> {
                                if (tokenTask.isSuccessful()) {
                                    String idToken = tokenTask.getResult().getToken();
                                    Log.d("TOKEN", idToken);
                                    SessionManager.getInstance().setIdToken(idToken);
                                    sendTokenToServer(idToken);
                                } else {
                                    Log.e("LOGIN", "토큰 발급 실패");
                                    runOnUiThread(() -> loadingOverlay.setVisibility(View.GONE));
                                }
                                
                            });
                        } else {
                            // --- Firebase 로그인 실패 처리 ---
                            Exception e = task.getException();
                            runOnUiThread(() -> {
                                String errorMessage = "";
                                // 예외 타입별로 사용자한테 보여줄 메시지 나눔
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

        // --- Google 로그인 설정 ---
        // [중요] requestIdToken에 default_web_client_id 넣어야 Firebase랑 토큰 맞음
        // 이 ID 틀리면 구글 로그인 후 Firebase credential 연결에서 터짐
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // 구글 버튼 누르면 계정 선택 Intent 띄움
        googleBtn.setOnClickListener(v -> {
            runOnUiThread(() -> {
                loadingOverlay.setVisibility(View.VISIBLE);
            });
            Intent intent = googleSignInClient.getSignInIntent();
            googleLauncher.launch(intent);
        });
        

    }

    /**
     * Firebase ID 토큰을 JSON body로 POST해서 백엔드 사용자·세션 정보 받음
     */
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
                // 서버 연결 실패 시 오버레이 끄고 다이얼로그 띄움
                // ApiHelper의 runOnUiThreadSafe를 사용해 UI 스레드에서 실행함
                ApiHelper.runOnUiThreadSafe(LoginActivity.this, () -> {
                    loadingOverlay.setVisibility(View.GONE);
                    CommonDialog dig = new CommonDialog(LoginActivity.this, "서버와 응답이 되지 않습니다.", "확인");
                    dig.setOnConfirmListener(view -> dig.dismiss());
                    dig.show();
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String responseBody =
                        response.body() != null ? response.body().string() : "";

                int code = response.code();

                Log.d("response", "code: " + code);
                Log.d("response", responseBody);

                // ApiHelper의 runOnUiThreadSafe를 사용해 UI 스레드에서 실행함
                ApiHelper.runOnUiThreadSafe(LoginActivity.this, () -> {
                    // 응답 왔으면 일단 로딩 오버레이 내림
                    loadingOverlay.setVisibility(View.GONE);
                    String errorMessage = "";

                    // [중요] 2xx + SessionHelper 성공해야만 메인 진입함
                    // SessionHelper가 응답 JSON 파싱해서 uid·이름 등 SessionManager에 저장함
                    // HTTP는 성공인데 파싱 실패면 세션 없는 상태라 에러 메시지 보여줌
                    if (code >= 200 && code < 300 && SessionHelper.applyLoginResponse(responseBody)) {
                        moveToMain();
                        return;
                    }
                    // 상태 코드별 에러 메시지 분기함
                    if (code >= 200 && code < 300) {
                        errorMessage = "로그인 정보를 확인할 수 없습니다.";
                    } else if (code == 401) {
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

                    // 에러 있으면 다이얼로그로 알려줌
                    if (!errorMessage.isEmpty()) {
                        CommonDialog dig = new CommonDialog(LoginActivity.this, errorMessage, "확인");
                        dig.setOnConfirmListener(view -> dig.dismiss());
                        dig.show();
                    }
                });
            }
        });
    }

    /**
     * 로그인·세션 저장 성공 시 MainActivity로 가고 LoginActivity 종료함
     */
    private void moveToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * 로그인 응답 JSON을 SessionManager에 반영함 (SessionHelper에 위임)
     */
    private void saveUserInfoFromLogin(String responseBody) {
        SessionHelper.applyLoginResponse(responseBody);
    }

    /**
     * Google Sign-In Intent 결과에서 계정 꺼내서 Firebase 인증으로 연결함
     */
    private void handleGoogleResult(Intent data) {
        Task<GoogleSignInAccount> task =
                GoogleSignIn.getSignedInAccountFromIntent(data);

        try {
            // 구글 계정이랑 ID 토큰 추출함
            GoogleSignInAccount account = task.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            Log.e("GOOGLE", "로그인 실패", e);
            runOnUiThread(() -> loadingOverlay.setVisibility(View.GONE));
        }
    }

    /**
     * Google ID 토큰으로 Firebase GoogleAuthProvider 로그인 후 JWT를 서버에 보냄
     */
    private void firebaseAuthWithGoogle(String idToken) {

        // 구글 토큰을 Firebase credential로 바꿈
        AuthCredential credential =
                GoogleAuthProvider.getCredential(idToken, null);
        FirebaseAuth.getInstance()
                .signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user == null) {
                            runOnUiThread(() -> loadingOverlay.setVisibility(View.GONE));
                            return;
                        }
                        // Firebase JWT 발급받아서 서버 로그인 태움
                        user.getIdToken(true)
                                .addOnSuccessListener(result -> {
                                    String firebaseToken = result.getToken();

                                    SessionManager.getInstance().setIdToken(firebaseToken);

                                    // [중요] 서버는 Firebase JWT만 검증함 — Google ID 토큰 직접 못 씀
                                    // signInWithCredential 거친 뒤 getIdToken으로 받은 토큰을 login API에 넣어야 함
                                    // 이거 안 지키면 서버에서 토큰 검증 실패남
                                    sendTokenToServer(firebaseToken);
                                })
                                .addOnFailureListener(e -> runOnUiThread(() ->
                                        loadingOverlay.setVisibility(View.GONE)
                                ));
                    } else {
                        // Firebase credential 로그인 실패 처리함
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

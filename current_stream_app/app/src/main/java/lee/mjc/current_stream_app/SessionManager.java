package lee.mjc.current_stream_app;

// 로그인 상태를 저장하는 싱글톤 클래스
public class SessionManager {
    private static SessionManager instance;
    private String idToken;

    private SessionManager() {}

    // 싱글톤 생성
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setIdToken(String token) {
        this.idToken = token;
    }

    public String getIdToken() {
        return idToken;
    }
}

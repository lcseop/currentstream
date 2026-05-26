package lee.mjc.current_stream_app;

// 로그인 상태를 저장하는 싱글톤 클래스
public class SessionManager {
    private static SessionManager instance;
    private String idToken;
    private String uid;        // Firebase uid
    private String tag;
    private Long currentTeamId;

    private SessionManager() {}

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

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUid() {
        return uid;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }

    public void setCurrentTeamId(Long teamId) {
        this.currentTeamId = teamId;
    }

    public Long getCurrentTeamId() {
        return currentTeamId;
    }
}
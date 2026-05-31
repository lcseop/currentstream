package lee.mjc.current_stream_app;

// 로그인 상태를 저장하는 싱글톤 클래스
public class SessionManager {
    private static SessionManager instance;
    private String idToken;
    private String uid;        // Firebase uid
    private String tag;
    private Long userId;       // DB user id
    private Long currentTeamId;

    private SessionManager() {}

    // 싱글톤 인스턴스 반환
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // Firebase ID 토큰 저장
    public void setIdToken(String token) {
        this.idToken = token;
    }

    // Firebase ID 토큰 조회
    public String getIdToken() {
        return idToken;
    }

    // Firebase uid 저장
    public void setUid(String uid) {
        this.uid = uid;
    }

    // Firebase uid 조회
    public String getUid() {
        return uid;
    }

    // 사용자 tag 저장
    public void setTag(String tag) {
        this.tag = tag;
    }

    // 사용자 tag 조회
    public String getTag() {
        return tag;
    }

    // DB user id 저장
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    // DB user id 조회
    public Long getUserId() {
        return userId;
    }

    // 현재 선택된 팀 id 저장
    public void setCurrentTeamId(Long teamId) {
        this.currentTeamId = teamId;
    }

    // 현재 선택된 팀 id 조회
    public Long getCurrentTeamId() {
        return currentTeamId;
    }

    // 로그인 정보 전부 초기화 (로그아웃 시)
    public void clear() {
        idToken = null;
        uid = null;
        tag = null;
        userId = null;
        currentTeamId = null;
    }
}

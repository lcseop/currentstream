package lee.mjc.current_stream_app;

/**
 * 로그인 세션이랑 선택한 팀 정보를 메모리에 들고 있는 싱글톤임.
 * API uid 헤더랑 화면 간 유저 정보 공유할 때 씀.
 * 앱 끄면 다 사라짐
 */
public class SessionManager {
    private static SessionManager instance;
    /** Firebase ID 토큰 */
    private String idToken;
    /** Firebase uid */
    private String uid;
    /** 유저 tag */
    private String tag;
    /** 닉네임 */
    private String userName;
    /** DB user id */
    private Long userId;
    /** 지금 선택한 팀 id */
    private Long currentTeamId;

    /** 싱글톤이라 외부에서 new 막음 */
    private SessionManager() {}

    /** SessionManager 유일 인스턴스 반환함 */
    public static SessionManager getInstance() {
        // 아직 없으면 이때 만듦
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /** Firebase ID 토큰 저장함. 서버 로그인 검증할 때 씀 */
    public void setIdToken(String token) {
        this.idToken = token;
    }

    /** 저장된 Firebase ID 토큰 반환함. 미로그인이면 null */
    public String getIdToken() {
        return idToken;
    }

    /** Firebase uid 저장함 */
    public void setUid(String uid) {
        this.uid = uid;
    }

    /** Firebase uid 반환함 */
    public String getUid() {
        return uid;
    }

    /** 사용자 tag 저장함 */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /** 사용자 tag 반환함 */
    public String getTag() {
        return tag;
    }

    /** 닉네임 저장함 */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /** 닉네임 반환함 */
    public String getUserName() {
        return userName;
    }

    /** DB user id 저장함. 팀·목표 API에서 숫자 id로 씀 */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /** DB user id 반환함 */
    public Long getUserId() {
        return userId;
    }

    /** 지금 고른 팀 id 저장함. 메인·팀 로그 화면 기본 팀임 */
    public void setCurrentTeamId(Long teamId) {
        this.currentTeamId = teamId;
    }

    /** 현재 선택 팀 id 반환함 */
    public Long getCurrentTeamId() {
        return currentTeamId;
    }

    /**
     * [중요] 세션 필드 전부 null로 비움.
     * 로그아웃·인증 실패 때 호출해서 이전 유저 정보가 남으면
     * 다음 로그인 때 uid 헤더가 꼬일 수 있음
     */
    public void clear() {
        idToken = null;
        uid = null;
        tag = null;
        userName = null;
        userId = null;
        currentTeamId = null;
    }
}

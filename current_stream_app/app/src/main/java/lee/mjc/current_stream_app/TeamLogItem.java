package lee.mjc.current_stream_app;

// 팀 활동 로그 한 건 모델
public class TeamLogItem {
    public long id;                 // 로그 ID
    public String message;          // 로그 메시지
    public long createdAtMillis;    // 생성 시각 (epoch ms)

    // 팀 활동 로그 항목 생성
    public TeamLogItem(long id, String message, long createdAtMillis) {
        this.id = id;
        this.message = message;
        this.createdAtMillis = createdAtMillis;
    }
}

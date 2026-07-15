package lee.mjc.current_stream_app;

/**
 * 팀 활동 로그 한 건 나타내는 모델임.
 * createdAtMillis는 DateTimeUtil로 상대 시간 표시할 때 씀.
 */
public class TeamLogItem {
    /** 로그 ID */
    public long id;
    /** 로그 메시지 */
    public String message;
    /** 생성 시각 (ms) */
    public long createdAtMillis;

    /** 팀 활동 로그 항목 생성함 */
    public TeamLogItem(long id, String message, long createdAtMillis) {
        this.id = id;
        this.message = message;
        this.createdAtMillis = createdAtMillis;
    }
}

package lee.mjc.current_stream_app;

// 초대 알림 바텀시트에 쓰는 초대 데이터
public class InviteItem {
    public long id;
    public long teamId;
    public String teamName;
    public String inviterName;
    public String message;

    // 팀 초대 알림 항목 생성
    public InviteItem(long id, long teamId, String teamName, String inviterName, String message) {
        this.id = id;
        this.teamId = teamId;
        this.teamName = teamName;
        this.inviterName = inviterName;
        this.message = message;
    }
}

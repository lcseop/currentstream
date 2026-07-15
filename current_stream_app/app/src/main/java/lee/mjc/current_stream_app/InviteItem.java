package lee.mjc.current_stream_app;

/**
 * 팀 초대 알림 바텀시트에 표시하는 초대 데이터 모델임.
 * 팀 이름·초대한 사람·메시지 들고 있음.
 */
public class InviteItem {
    /** 초대 레코드 ID */
    public long id;
    /** 초대 대상 팀 ID */
    public long teamId;
    /** 팀 이름 (표시용) */
    public String teamName;
    /** 초대한 사람 이름 */
    public String inviterName;
    /** 서버에서 내려준 초대 메시지 (없으면 기본 문구 씀) */
    public String message;

    /** 팀 초대 알림 항목 생성함 */
    public InviteItem(long id, long teamId, String teamName, String inviterName, String message) {
        this.id = id;
        this.teamId = teamId;
        this.teamName = teamName;
        this.inviterName = inviterName;
        this.message = message;
    }
}

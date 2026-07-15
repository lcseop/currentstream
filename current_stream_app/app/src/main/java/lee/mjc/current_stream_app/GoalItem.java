package lee.mjc.current_stream_app;

/**
 * 메인 화면 '내 작업' 목록에 쓰는 개인 목표 데이터 모델임.
 * API 응답 필드 그대로 담아서 어댑터·상세 화면에서 공유함.
 */
public class GoalItem {
    /** 목표 ID */
    public long id;
    /** 담당 사용자 DB ID */
    public long userId;
    /** 목표 내용 텍스트 */
    public String goalText;
    /** 상태 (0: 진행 중, 1: 완료) */
    public int status;
    /** 비고·메모 */
    public String remark;
    /** 목표 마감일 (yyyy-MM-dd) */
    public String goalEndDate;

    /** 내 작업 목표 항목 생성함 */
    public GoalItem(long id, long userId, String goalText, int status, String remark, String goalEndDate) {
        this.id = id;
        this.userId = userId;
        this.goalText = goalText;
        this.status = status;
        this.remark = remark;
        this.goalEndDate = goalEndDate;
    }
}

package lee.mjc.current_stream_app;

/**
 * 팀원별 목표 항목 모델임.
 * TeamMemberItem의 ongoingGoals·completedGoals에 들어감.
 */
public class TeamGoalItem {
    /** 목표 ID */
    public long id;
    /** 담당 팀원 user ID */
    public long userId;
    /** 목표 내용 */
    public String goalText;
    /** 비고·메모 */
    public String remark;
    /** 상태 (0: 진행 중, 1: 완료) */
    public int status;
    /** 목표 마감일 (yyyy-MM-dd) */
    public String goalEndDate;

    /** 팀원 목표 항목 생성함 */
    public TeamGoalItem(long id, long userId, String goalText, String remark, int status, String goalEndDate) {
        this.id = id;
        this.userId = userId;
        this.goalText = goalText;
        this.remark = remark;
        this.status = status;
        this.goalEndDate = goalEndDate;
    }
}

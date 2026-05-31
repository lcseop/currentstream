package lee.mjc.current_stream_app;

// 팀 멤버별 목표 항목 모델
public class TeamGoalItem {
    public long id;              // 목표 ID
    public long userId;          // 담당 멤버 user ID
    public String goalText;      // 목표 내용
    public String remark;        // 비고
    public int status;           // 0: 진행 중, 1: 완료
    public String goalEndDate;   // 목표 마감일

    // 팀 멤버 목표 항목 생성
    public TeamGoalItem(long id, long userId, String goalText, String remark, int status, String goalEndDate) {
        this.id = id;
        this.userId = userId;
        this.goalText = goalText;
        this.remark = remark;
        this.status = status;
        this.goalEndDate = goalEndDate;
    }
}

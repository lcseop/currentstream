package lee.mjc.current_stream_app;

// 메인 화면 '내 작업' 목록에 쓰는 목표 데이터
public class GoalItem {
    public long id;
    public long userId;
    public String goalText;
    public int status;
    public String remark;
    public String goalEndDate;

    // 내 작업 목표 항목 생성
    public GoalItem(long id, long userId, String goalText, int status, String remark, String goalEndDate) {
        this.id = id;
        this.userId = userId;
        this.goalText = goalText;
        this.status = status;
        this.remark = remark;
        this.goalEndDate = goalEndDate;
    }
}

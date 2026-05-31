package lee.mjc.current_stream_app;

public class TeamGoalItem {
    public long id;
    public long userId;
    public String goalText;
    public String remark;
    public int status;
    public String goalEndDate;

    public TeamGoalItem(long id, long userId, String goalText, String remark, int status, String goalEndDate) {
        this.id = id;
        this.userId = userId;
        this.goalText = goalText;
        this.remark = remark;
        this.status = status;
        this.goalEndDate = goalEndDate;
    }
}

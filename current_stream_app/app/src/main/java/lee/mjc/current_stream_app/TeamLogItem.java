package lee.mjc.current_stream_app;

public class TeamLogItem {
    public long id;
    public String message;
    public long createdAtMillis;

    public TeamLogItem(long id, String message, long createdAtMillis) {
        this.id = id;
        this.message = message;
        this.createdAtMillis = createdAtMillis;
    }
}

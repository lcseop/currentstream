package lee.mjc.current_stream_app;

// 팀 UI 관련 상수 (앱 입력·표시 제한)
public final class TeamUiConstants {

    public static final int TEAM_NAME_MIN = 2;
    public static final int TEAM_NAME_MAX = 20;

    private TeamUiConstants() {
    }

    public static String teamNameLengthMessage() {
        return "팀 이름은 " + TEAM_NAME_MIN + "~" + TEAM_NAME_MAX + "자로 입력해주세요.";
    }
}

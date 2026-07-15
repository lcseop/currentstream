package lee.mjc.current_stream_app;

/**
 * 팀 관련 UI 입력·표시 제한 상수임.
 * 앱에서 먼저 길이 검증해서 잘못된 요청 줄임.
 */
public final class TeamUiConstants {

    /** 팀 이름 최소 글자 수 */
    public static final int TEAM_NAME_MIN = 2;

    /** 팀 이름 최대 글자 수 */
    public static final int TEAM_NAME_MAX = 20;

    /** new 막음 */
    private TeamUiConstants() {
    }

    /** 팀 이름 길이 틀렸을 때 보여줄 안내 문구 */
    public static String teamNameLengthMessage() {
        return "팀 이름은 " + TEAM_NAME_MIN + "~" + TEAM_NAME_MAX + "자로 입력해주세요.";
    }
}

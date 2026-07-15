package lee.mjc.current_stream_app;

/**
 * 팀 생성·초대 화면에서 API 호출 전 임시로 담아두는 초대 대상 정보임.
 * 화면엔 name 보여주고 API엔 tag 넣음.
 */
public class InviteMember {

    /** 화면에 보여줄 닉네임 (검색 API 응답 name) */
    public final String name;

    /** 초대 API에 넣을 고유 태그 (users.tag) */
    public final String tag;

    /** 초대 대상 생성함. name은 표시용, tag는 식별용 */
    public InviteMember(String name, String tag) {
        this.name = name;
        this.tag = tag;
    }
}

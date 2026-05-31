package lee.mjc.current_stream_app;

// 팀 생성/초대 시 임시로 담아 두는 멤버 정보
public class InviteMember {
    public final String name;   // 표시 이름
    public final String tag;    // 초대할 사용자 tag

    // 초대할 멤버 정보 생성
    public InviteMember(String name, String tag) {
        this.name = name;
        this.tag = tag;
    }
}

package com.mjc813.currentstreambackend.models.users;

/*
    UsersDto, UsersEntity를 잇게 해주는 인터페이스
 */
public interface UsersInterface {
    Long getId();
    void setId(Long id);

    String getUid();
    void setUid(String uid);

    String getName();
    void setName(String name);

    String getEmail();
    void setEmail(String email);

    String getTag();
    void setTag(String tag);

    /*
        Service에서 Entity를 바로 쓰면 생기는 영속성 문제, DTO로 반드시 리턴해야 하는 문제
        를 해결하기 위해 만든 default copy 복사 메소드
     */
    default UsersInterface copyMembers(UsersInterface src, boolean forced) {
        if (src == null) {
            return this;
        }
        if (forced || src.getId() != null) {
            this.setId(src.getId());
        }
        if (forced || src.getUid() != null) {
            this.setUid(src.getUid());
        }
        if (forced || src.getName() != null) {
            this.setName(src.getName());
        }
        if (forced || src.getEmail() != null) {
            this.setEmail(src.getEmail());
        }
        if (forced || src.getTag() != null) {
            this.setTag(src.getTag());
        }
        return this;
    }
}

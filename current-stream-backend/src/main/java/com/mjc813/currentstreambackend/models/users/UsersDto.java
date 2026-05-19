package com.mjc813.currentstreambackend.models.users;

import lombok.*;

// 기본적인 롬복 애너테이션 설정
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
/*
    Users의 데이터를 전달하는 DTO 객체
 */
public class UsersDto implements UsersInterface {
    // 기본적인 멤버변수 (속성) 생성
    private Long id;
    private String uid;
    private String name;
    private String email;
    private String tag;
}

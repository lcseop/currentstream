package com.mjc813.currentstreambackend.models.users;

import jakarta.persistence.*;
import lombok.*;

// 기본적인 롬복 애너테이션 설정
@Entity(name="users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
/*
    Users의 데이터베이스와 직접적으로 연결된 객체
 */
public class UsersEntity implements UsersInterface {
    // 각 속성에 애너테이션을 이용해 직접적으로 매핑
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, unique = true, nullable = false)
    private String uid;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(length = 50, nullable = false)
    private String email;

    @Column(length = 30, unique = true, nullable = false)
    private String tag;
}

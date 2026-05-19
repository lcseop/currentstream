package com.mjc813.currentstreambackend.models.invite;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

// 기본적인 롬복 애너테이션 설정
@Entity(name="team_invite")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
/*
    team_invite의 데이터베이스와 직접적으로 연결된 객체
 */
public class InviteEntity {
    // 각 속성에 애너테이션을 이용해 직접적으로 매핑
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    // 상태 (0 : 요청, 1 : 수락, 2 : 거절)
    private Integer status;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long teamId;
}

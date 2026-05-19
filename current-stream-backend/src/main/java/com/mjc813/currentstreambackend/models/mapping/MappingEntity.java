package com.mjc813.currentstreambackend.models.mapping;

import jakarta.persistence.*;
import lombok.*;

// 기본적인 롬복 애너테이션 설정
@Entity(name="member_mapping")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
/*
    MemberMapping의 데이터베이스와 직접적으로 연결된 객체
 */
public class MappingEntity {
    // 각 속성에 애너테이션을 이용해 직접적으로 매핑
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(length = 7, name = "user_color", nullable = false)
    private String userColor;
}

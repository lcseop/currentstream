package com.mjc813.currentstreambackend.models.teams;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

// 기본적인 롬복 애너테이션 설정
@Entity(name="teams")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
/*
    Teams의 데이터베이스와 직접적으로 연결된 객체
 */
public class TeamsEntity {
    // 각 속성에 애너테이션을 이용해 직접적으로 매핑
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, name = "team_name", nullable = false)
    private String teamName;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "leader_id", nullable = false)
    private Long leaderId;
}

package com.mjc813.currentstreambackend.models.goal;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

// 기본적인 롬복 애너테이션 설정
@Entity(name="goal")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
/*
    Goal의 데이터베이스와 직접적으로 연결된 객체
 */
public class GoalEntity {
    // 각 속성에 애너테이션을 이용해 직접적으로 매핑
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    // 목표 내용
    private String goalText;

    @Column(nullable = false)
    // 상태 (0, 1, 2)
    private Integer status;

    @Column(length = 255, nullable = false)
    // 특이사항
    private String remark;

    @Column(nullable = false)
    private LocalDate goalEndDate;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long teamId;
}

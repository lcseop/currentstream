package com.mjc813.currentstreambackend.models.goal;

import lombok.*;

import java.time.LocalDate;

// 기본적인 롬복 애너테이션 설정
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
/*
    Goal의 데이터를 전달하는 DTO 객체
 */
public class GoalDto {
    private Long id;
    private String goalText;
    private Integer status;
    private String remark;
    private LocalDate goalEndDate;
    private Long userId;
    private Long teamId;
}

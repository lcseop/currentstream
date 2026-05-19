package com.mjc813.currentstreambackend.models.teams;

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
    Teams의 데이터를 전달하는 DTO 객체
 */
public class TeamsDto {
    private Long id;
    private String teamName;
    private LocalDate endDate;
    private Long leaderId;

    public static TeamsDto fromEntity(TeamsEntity team) {
        TeamsDto dto = new TeamsDto();
        dto.setId(team.getId());
        dto.setTeamName(team.getTeamName());
        dto.setEndDate(team.getEndDate());
        dto.setLeaderId(team.getLeaderId());
        return dto;
    }
}

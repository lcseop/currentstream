package com.currentstreambackend.currentstreambackend.models.teamlogs;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class TeamLogsDto {

    private Long id;
    private Long teamId;
    private Long userId;
    private String message;
    private LocalDateTime createdAt;

    public static TeamLogsDto fromEntity(TeamLogsEntity log) {
        TeamLogsDto dto = new TeamLogsDto();

        dto.setId(log.getId());
        dto.setTeamId(log.getTeamId());
        dto.setUserId(log.getUserId());
        dto.setMessage(log.getMessage());
        dto.setCreatedAt(log.getCreatedAt());

        return dto;
    }
}
package com.currentstreambackend.currentstreambackend.models.teamlogs;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class TeamLogsDto {

    private static final ZoneId SERVER_ZONE = ZoneId.of("Asia/Seoul");

    private Long id;
    private Long teamId;
    private Long userId;
    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /** 클라이언트 시간 표시용 epoch millis (Asia/Seoul 기준) */
    private Long createdAtMillis;

    public static TeamLogsDto fromEntity(TeamLogsEntity log) {
        TeamLogsDto dto = new TeamLogsDto();

        dto.setId(log.getId());
        dto.setTeamId(log.getTeamId());
        dto.setUserId(log.getUserId());
        dto.setMessage(log.getMessage());
        dto.setCreatedAt(log.getCreatedAt());
        if (log.getCreatedAt() != null) {
            dto.setCreatedAtMillis(log.getCreatedAt().atZone(SERVER_ZONE).toInstant().toEpochMilli());
        }

        return dto;
    }
}
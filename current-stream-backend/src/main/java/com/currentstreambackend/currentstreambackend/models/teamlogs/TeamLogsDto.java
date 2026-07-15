package com.currentstreambackend.currentstreambackend.models.teamlogs;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 팀 활동 로그 API 응답용 DTO.
 * <p>
 * 엔티티({@link TeamLogsEntity})를 클라이언트 친화적 형태로 변환하며,
 * ISO 날짜 문자열과 epoch millis를 함께 제공해 앱의 시간 표시 요구를 충족한다.
 * </p>
 */
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

    /**
     * {@link TeamLogsEntity}를 DTO로 변환한다.
     * <p>
     * 비즈니스 원칙: {@code createdAtMillis}는 Asia/Seoul 기준으로 계산해
     * 클라이언트 타임존 변환 없이 일관된 시각을 제공한다.
     * </p>
     *
     * @param log 변환할 엔티티
     * @return API 응답용 DTO
     */
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

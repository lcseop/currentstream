package com.mjc813.currentstreambackend.models.invite;

import com.mjc813.currentstreambackend.models.teams.TeamsDto;
import com.mjc813.currentstreambackend.models.teams.TeamsEntity;
import lombok.*;

// 기본적인 롬복 애너테이션 설정
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
/*
    team_invite의 데이터를 전달하는 DTO 객체
 */
public class InviteDto {
    private Long id;
    private Integer status;
    private Long userId;
    private Long teamId;


    public static InviteDto fromEntity(InviteEntity invite) {
        InviteDto dto = new InviteDto();
        dto.setId(invite.getId());
        dto.setStatus(invite.getStatus());
        dto.setUserId(invite.getUserId());
        dto.setTeamId(invite.getTeamId());
        return dto;
    }
}

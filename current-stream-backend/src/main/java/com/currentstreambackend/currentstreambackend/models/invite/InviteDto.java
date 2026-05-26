package com.currentstreambackend.currentstreambackend.models.invite;

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
    private String teamName;
    private String inviterName;


    public static InviteDto fromEntity(InviteEntity invite) {
        InviteDto dto = new InviteDto();
        dto.setId(invite.getId());
        dto.setStatus(invite.getStatus());
        dto.setUserId(invite.getUserId());
        dto.setTeamId(invite.getTeamId());
        dto.setTeamName(invite.getTeamName());
        dto.setInviterName(invite.getInviterName());
        return dto;
    }
}

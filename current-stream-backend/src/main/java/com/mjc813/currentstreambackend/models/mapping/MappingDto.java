package com.mjc813.currentstreambackend.models.mapping;

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
    MemberMapping의 데이터를 전달하는 DTO 객체
 */
public class MappingDto {
    private Long id;
    private Long userId;
    private Long teamId;
    private String userColor;


    public static MappingDto fromEntity(MappingEntity mapping) {
        MappingDto dto = new MappingDto();
        dto.setId(mapping.getId());
        dto.setUserId(mapping.getUserId());
        dto.setTeamId(mapping.getTeamId());
        dto.setUserColor(mapping.getUserColor());
        return dto;
    }
}

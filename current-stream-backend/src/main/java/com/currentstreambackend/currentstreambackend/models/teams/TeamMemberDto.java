package com.currentstreambackend.currentstreambackend.models.teams;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class TeamMemberDto {
    private Long userId;
    private String name;
    private String tag;
    private String userColor;
    private boolean leader;
}

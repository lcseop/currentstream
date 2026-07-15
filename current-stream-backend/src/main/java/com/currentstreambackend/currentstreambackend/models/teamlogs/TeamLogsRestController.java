package com.currentstreambackend.currentstreambackend.models.teamlogs;

import com.currentstreambackend.currentstreambackend.models.common.ApiResponse;
import com.currentstreambackend.currentstreambackend.models.common.ResponseCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 팀 활동 로그 REST API 진입점.
 * <p>
 * 클라이언트(Android 앱)의 HTTP 요청을 수신하고 {@link TeamLogsService}에 위임한다.
 * 인증 식별은 {@code uid} 요청 헤더를 사용하며, 응답은 공통 {@link ApiResponse} 래퍼로 반환한다.
 * </p>
 */
@RestController
@RequestMapping("/api/team/log")
public class TeamLogsRestController {

    @Autowired
    private TeamLogsService teamLogsService;

    /**
     * 특정 팀의 최근 활동 로그를 조회한다.
     * <p>
     * 비즈니스 규칙: {@code uid} 헤더로 요청자를 식별하고, 서비스 계층에서 팀 멤버 여부를 검증한다.
     * </p>
     *
     * @param uid    Firebase UID (요청 헤더)
     * @param teamId 경로 변수로 전달된 팀 ID
     * @return 최근 로그 목록을 담은 성공 응답
     */
    @GetMapping("/{teamId}")
    public ResponseEntity<ApiResponse<List<TeamLogsDto>>> getLogs(
            @RequestHeader("uid") String uid,
            @PathVariable Long teamId
    ) {

        List<TeamLogsDto> result = teamLogsService.getTeamLogs(uid, teamId);

        return ResponseEntity.ok(
                ApiResponse.make(
                        ResponseCode.select_ok,
                        "team logs",
                        result
                )
        );
    }
}

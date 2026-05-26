package com.currentstreambackend.currentstreambackend.models.teamlogs;

import com.currentstreambackend.currentstreambackend.models.common.ApiResponse;
import com.currentstreambackend.currentstreambackend.models.common.ResponseCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team/log")
public class TeamLogsRestController {

    @Autowired
    private TeamLogsService teamLogsService;

    /**
     * 팀 로그 조회
     */
    @GetMapping("/{teamId}")
    public ResponseEntity<ApiResponse<List<TeamLogsDto>>> getLogs(
            @PathVariable Long teamId
    ) {

        List<TeamLogsDto> result = teamLogsService.getTeamLogs(teamId);

        return ResponseEntity.ok(
                ApiResponse.make(
                        ResponseCode.select_ok,
                        "team logs",
                        result
                )
        );
    }
}
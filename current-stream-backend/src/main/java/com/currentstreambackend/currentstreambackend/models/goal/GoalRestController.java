package com.currentstreambackend.currentstreambackend.models.goal;

import com.currentstreambackend.currentstreambackend.models.common.ApiResponse;
import com.currentstreambackend.currentstreambackend.models.common.ResponseCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 목표 CRUD·상태 변경 HTTP 엔드포인트.
 * <p>
 * 생성 시 {@code targetUserId}가 있으면 팀장이 다른 멤버에게 목표를 할당하는 흐름입니다.
 * 비즈니스 규칙(권한·길이 제한)은 {@link GoalService}에서 처리합니다.
 * </p>
 */
@RestController
@RequestMapping("/api/goal")
public class GoalRestController {
    @Autowired
    private GoalService goalService;

    @PostMapping
    public ResponseEntity<ApiResponse<GoalDto>> createGoal(@RequestHeader("uid") String uid, @RequestBody Map<String, String> body) {
        Long targetUserId = null;
        if (body.containsKey("targetUserId") && body.get("targetUserId") != null && !body.get("targetUserId").isEmpty()) {
            targetUserId = Long.parseLong(body.get("targetUserId"));
        }
        GoalDto result = goalService.createGoal(
                uid,
                Long.parseLong(body.get("teamId")),
                body.get("text"),
                body.get("remark"),
                LocalDate.parse(body.get("endDate")),
                targetUserId
        );
        return ResponseEntity.status(201).body(
                ApiResponse.make(ResponseCode.insert_ok, "goal created", result)
        );
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<ApiResponse<List<GoalDto>>> getGoals(@RequestHeader("uid") String uid, @PathVariable Long teamId) {
        List<GoalDto> result = goalService.getGoals(uid, teamId);
        return ResponseEntity.ok(
                ApiResponse.make(ResponseCode.select_ok, "goal list", result)
        );
    }

    @GetMapping("/team/{teamId}/all")
    public ResponseEntity<ApiResponse<List<GoalDto>>> getAllTeamGoals(@RequestHeader("uid") String uid, @PathVariable Long teamId) {
        List<GoalDto> result = goalService.getAllTeamGoals(uid, teamId);
        return ResponseEntity.ok(
                ApiResponse.make(ResponseCode.select_ok, "team goal list", result)
        );
    }

    @PatchMapping("/{goalId}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(@RequestHeader("uid") String uid, @PathVariable Long goalId, @RequestBody Map<String, String> body) {
        goalService.updateStatus(uid, goalId, Integer.parseInt(body.get("status")));
        return ResponseEntity.ok(
                ApiResponse.make(ResponseCode.update_ok, "status updated", null)
        );
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(@RequestHeader("uid") String uid, @PathVariable Long goalId) {
        goalService.deleteGoal(uid, goalId);
        return ResponseEntity.ok(
                ApiResponse.make(ResponseCode.delete_ok, "goal deleted", null)
        );
    }

}

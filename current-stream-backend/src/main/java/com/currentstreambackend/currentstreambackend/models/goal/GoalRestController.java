package com.currentstreambackend.currentstreambackend.models.goal;

import com.currentstreambackend.currentstreambackend.models.common.ApiResponse;
import com.currentstreambackend.currentstreambackend.models.common.ResponseCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goal")
public class GoalRestController {
    @Autowired
    private GoalService goalService;

    @PostMapping
    public ResponseEntity<ApiResponse<GoalDto>> createGoal(@RequestHeader("uid") String uid, @RequestBody Map<String, String> body) {
        GoalDto result = goalService.createGoal(uid, Long.parseLong(body.get("teamId")), body.get("text"), body.get("remark"), LocalDate.parse(body.get("endDate")));
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

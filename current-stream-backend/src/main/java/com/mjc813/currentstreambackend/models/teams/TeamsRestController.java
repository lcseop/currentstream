package com.mjc813.currentstreambackend.models.teams;

import com.mjc813.currentstreambackend.models.common.ApiResponse;
import com.mjc813.currentstreambackend.models.common.ResponseCode;
import com.mjc813.currentstreambackend.models.invite.InviteDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/team")
public class TeamsRestController {
    @Autowired
    private TeamsService teamsService;

    /**
     * 팀 생성
     * @param uid
     * @param body json 받아옴
     * @return 팀 정보
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TeamsDto>> createTeam(@RequestHeader("uid") String uid, @RequestBody Map<String, String> body) {
        TeamsDto result = teamsService.createTeam(uid, body.get("name"), LocalDate.parse(body.get("endDate")));
        return ResponseEntity.status(201).body(
                ApiResponse.make(ResponseCode.insert_ok, "team created", result)
        );
    }

    /**
     * 팀 목록 조회
     * @param uid
     * @return 사용자가 속한 팀 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TeamsDto>>> getMyTeams(@RequestHeader("uid") String uid) {
        List<TeamsDto> result = teamsService.getMyTeams(uid);
        return ResponseEntity.ok(
                ApiResponse.make(
                        ResponseCode.select_ok,
                        "teams list",
                        result
                )
        );
    }

    /**
     * 팀원 초대
     * @param uid
     * @param body json 받아옴
     * @return 결과 메시지만 전달
     */
    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<Void>> invite(@RequestHeader("uid") String uid, @RequestBody Map<String, String> body) {
        teamsService.inviteUser(uid, Long.parseLong(body.get("teamId")), body.get("tag"));
        return ResponseEntity.ok(
                ApiResponse.make(ResponseCode.insert_ok, "invite success", null)
        );
    }

    /**
     * 팀원 초대 수락
     * @param uid
     * @param inviteId PathVariable을 통해 url에서 초대 아이디를 가져옴
     * @return 결과 메시지만 전달
     */
    @PostMapping("/invite/{inviteId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptInvite(@RequestHeader("uid") String uid, @PathVariable Long inviteId) {
        teamsService.acceptInvite(uid, inviteId);
        return ResponseEntity.ok(
                ApiResponse.make(ResponseCode.update_ok, "invite accept", null)
        );
    }

    /**
     * 팀원 초대 거절
     * @param uid
     * @param inviteId PathVariable을 통해 url에서 초대 아이디를 가져옴
     * @return 결과 메시지만 전달
     */
    @PostMapping("/invite/{inviteId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectInvite(@RequestHeader("uid") String uid, @PathVariable Long inviteId) {
        teamsService.rejectInvite(uid, inviteId);
        return ResponseEntity.ok(
                ApiResponse.make(ResponseCode.update_ok, "invite reject", null)
        );
    }

    /**
     * 사용자가 받은 초대 목록 조회
     * @param uid
     * @return 받은 초대 목록 조회
     */
    @GetMapping("/invite")
    public ResponseEntity<ApiResponse<List<InviteDto>>> getInvites(@RequestHeader("uid") String uid) {
        List<InviteDto> result = teamsService.getMyInvites(uid);
        return ResponseEntity.ok(
                ApiResponse.make(ResponseCode.select_ok, "invite list",  result)
        );
    }

    /**
     * 팀 탈퇴
     * @param uid
     * @param teamId
     * @return 결과 메시지만 전달
     */
    @DeleteMapping("/{teamId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveTeam(@RequestHeader("uid") String uid, @PathVariable Long teamId) {
        teamsService.leaveTeam(uid, teamId);
        return ResponseEntity.ok(
                ApiResponse.make(ResponseCode.delete_ok, "leave team", null)
        );
    }

    /**
     * 팀 삭제
     * @param uid
     * @param teamId
     * @return 결과 메시지만 전달
     */
    @DeleteMapping("/{teamId}")
    public ResponseEntity<ApiResponse<Void>> deleteTeam(@RequestHeader("uid") String uid, @PathVariable Long teamId) {
        teamsService.deleteTeam(uid, teamId);
        return ResponseEntity.ok(
                ApiResponse.make(ResponseCode.delete_ok, "deleted team", null)
        );
    }
}

package com.currentstreambackend.currentstreambackend.models.goal;

import com.currentstreambackend.currentstreambackend.models.mapping.MappingRepository;
import com.currentstreambackend.currentstreambackend.models.teamlogs.TeamLogsService;
import com.currentstreambackend.currentstreambackend.models.teams.TeamsEntity;
import com.currentstreambackend.currentstreambackend.models.teams.TeamsRepository;
import com.currentstreambackend.currentstreambackend.models.users.UsersEntity;
import com.currentstreambackend.currentstreambackend.models.users.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 팀 목표(Goal) 도메인 비즈니스 로직을 담당하는 서비스 계층.
 * <p>
 * 목표 생성·상태 변경·삭제·조회를 처리하며, 팀 멤버십과 팀장 권한을 기준으로 접근을 제한한다.
 * 목표 상태: 0=진행 중, 1=달성, 2=삭제(소프트 삭제). 변경 이벤트는 {@link TeamLogsService}에 기록된다.
 * </p>
 */
@Service
public class GoalService {
    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private MappingRepository mappingRepository;

    @Autowired
    private TeamsRepository teamsRepository;

    @Autowired
    private TeamLogsService teamLogsService;

    /**
     * 팀에 새 목표를 생성한다.
     * <p>
     * 비즈니스 규칙:
     * <ul>
     *   <li>요청자는 팀 멤버여야 한다.</li>
     *   <li>다른 멤버에게 목표를 할당하려면 팀장 권한이 필요하다.</li>
     *   <li>목표 텍스트는 필수(최대 50자), 비고는 최대 255자.</li>
     *   <li>생성 시 status=0(진행 중)으로 초기화된다.</li>
     * </ul>
     * </p>
     *
     * @param uid          Firebase UID (요청자)
     * @param teamId       목표가 속할 팀 ID
     * @param text         목표 내용
     * @param remark       비고
     * @param endDate      목표 종료일
     * @param targetUserId 할당 대상 사용자 ID (null이면 본인에게 할당)
     * @return 생성된 목표 DTO
     */
    public GoalDto createGoal(String uid, Long teamId, String text, String remark, LocalDate endDate, Long targetUserId) {
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));
        TeamsEntity team = teamsRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team not found"));

        // [중요] 팀 멤버십 검증
        boolean isMember = mappingRepository.existsByUserIdAndTeamId(user.getId(), teamId);
        if (!isMember) throw new RuntimeException("Not team user");

        Long assignUserId = user.getId();
        if (targetUserId != null && !targetUserId.equals(user.getId())) {
            // [중요] 타인에게 목표 할당 시 팀장 권한 및 대상 멤버십 검증
            if (!team.getLeaderId().equals(user.getId())) {
                throw new RuntimeException("Not leader");
            }
            if (!mappingRepository.existsByUserIdAndTeamId(targetUserId, teamId)) {
                throw new RuntimeException("Target not in team");
            }
            assignUserId = targetUserId;
        }

        UsersEntity assignee = usersRepository.findById(assignUserId)
                .orElseThrow(() -> new RuntimeException("Assignee not found"));

        // [중요] 입력값 유효성 검증
        if (text == null || text.isBlank()) {
            throw new RuntimeException("Goal text required");
        }
        if (text.length() > 50) {
            throw new RuntimeException("Goal text too long");
        }
        if (remark == null) {
            remark = "";
        }
        if (remark.length() > 255) {
            throw new RuntimeException("Remark too long");
        }

        GoalEntity goal = new GoalEntity();
        goal.setGoalText(text);
        goal.setRemark(remark);
        goal.setStatus(0);
        goal.setGoalEndDate(endDate);
        goal.setUserId(assignUserId);
        goal.setTeamId(teamId);

        teamLogsService.createLog(
                teamId,
                user.getId(),
                assignee.getName() + "님에게 " + shorten(goal.getGoalText(), 10) + " 목표가 추가되었습니다."
        );

        return GoalDto.fromEntity(goalRepository.save(goal));
    }

    /**
     * 목표 상태를 변경한다.
     * <p>
     * 비즈니스 규칙:
     * <ul>
     *   <li>status는 0(진행 중), 1(달성), 2(삭제)만 허용.</li>
     *   <li>요청자는 팀 멤버이면서, 팀장 또는 목표 담당자여야 한다.</li>
     *   <li>상태별 활동 로그 메시지가 자동 생성된다.</li>
     * </ul>
     * </p>
     *
     * @param uid    Firebase UID
     * @param goalId 변경할 목표 ID
     * @param status 새 상태 값 (0, 1, 2)
     */
    public void updateStatus(String uid, Long goalId, Integer status) {
        // [중요] 상태 값 범위 검증
        if (status == null || status < 0 || status > 2) {
            throw new RuntimeException("Invalid status");
        }

        // 사용자, 목표, 팀 행을 불러옴
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));
        GoalEntity goal = goalRepository.findById(goalId).orElseThrow(() -> new RuntimeException("Goal not found"));
        TeamsEntity team = teamsRepository.findById(goal.getTeamId()).orElseThrow(() -> new RuntimeException("Team not found"));

        // [중요] 팀 멤버십 검증
        boolean isMember = mappingRepository.existsByUserIdAndTeamId(user.getId(), goal.getTeamId());
        if (!isMember) throw new RuntimeException("Not team user");

        // [중요] 권한 검증 — 팀장 또는 목표 담당자만 상태 변경 가능
        if (!team.getLeaderId().equals(user.getId())
                && !goal.getUserId().equals(user.getId())) {
            throw new RuntimeException("No premission");
        }

        switch(status) {
            case 0 :
                teamLogsService.createLog(
                        team.getId(),
                        user.getId(),
                        user.getName() + "님의 " + shorten(goal.getGoalText(), 10) + " 목표가 진행 중으로 바뀌었습니다."
                );
                break;
            case 1 :
                teamLogsService.createLog(
                        team.getId(),
                        user.getId(),
                        user.getName() + "님이 " + shorten(goal.getGoalText(), 10) + " 목표를 달성하셨습니다."
                );
                break;
            case 2 :
                teamLogsService.createLog(
                        team.getId(),
                        user.getId(),
                        user.getName() + "님의 " + shorten(goal.getGoalText(), 10) + " 목표가 삭제되었습니다."
                );
                break;
        }

        // 상태 변경
        goal.setStatus(status);
        goalRepository.save(goal);
    }

    /**
     * 목표를 물리적으로 삭제한다.
     * <p>
     * 비즈니스 규칙: 팀장 또는 목표 담당자만 삭제 가능.
     * 삭제 전 활동 로그에 기록한다.
     * </p>
     *
     * @param uid    Firebase UID
     * @param goalId 삭제할 목표 ID
     */
    public void deleteGoal(String uid, Long goalId) {
        // 사용자, 목표, 팀 행을 불러옴
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));
        GoalEntity goal = goalRepository.findById(goalId).orElseThrow(() -> new RuntimeException("Goal not found"));
        TeamsEntity team = teamsRepository.findById(goal.getTeamId()).orElseThrow(() -> new RuntimeException("Team not found"));

        // [중요] 권한 검증 — 팀장 또는 목표 담당자만 삭제 가능
        if (!team.getLeaderId().equals(user.getId())
                && !goal.getUserId().equals(user.getId())) {
            throw new RuntimeException("No premission");
        }

        teamLogsService.createLog(
                team.getId(),
                user.getId(),
                user.getName() + "님의 " + shorten(goal.getGoalText(), 10) + " 목표가 삭제되었습니다."
        );

        goalRepository.deleteById(goalId);
    }

    /**
     * 요청자 본인에게 할당된 팀 목표 목록을 조회한다.
     * <p>
     * 비즈니스 규칙: 팀 멤버만 조회 가능하며, 본인(userId)에게 할당된 목표만 반환한다.
     * </p>
     *
     * @param uid    Firebase UID
     * @param teamId 조회 대상 팀 ID
     * @return 본인 목표 DTO 목록
     */
    public List<GoalDto> getGoals(String uid, Long teamId) {
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));

        // [중요] 팀 멤버십 검증
        if (!mappingRepository.existsByUserIdAndTeamId(user.getId(), teamId)) {
            throw new RuntimeException("Not team user");
        }

        return goalRepository.findByTeamIdAndUserId(teamId, user.getId())
                .stream()
                .map(GoalDto::fromEntity)
                .toList();
    }

    /**
     * 팀 전체 목표를 조회한다 (삭제 상태 제외).
     * <p>
     * 비즈니스 규칙: 팀 멤버만 조회 가능.
     * status=2(삭제)인 목표는 필터링하여 제외한다.
     * </p>
     *
     * @param uid    Firebase UID
     * @param teamId 조회 대상 팀 ID
     * @return 팀 전체 목표 DTO 목록 (삭제 제외)
     */
    public List<GoalDto> getAllTeamGoals(String uid, Long teamId) {
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));

        // [중요] 팀 멤버십 검증
        if (!mappingRepository.existsByUserIdAndTeamId(user.getId(), teamId)) {
            throw new RuntimeException("Not team user");
        }

        return goalRepository.findByTeamId(teamId)
                .stream()
                .filter(g -> g.getStatus() != 2)
                .map(GoalDto::fromEntity)
                .toList();
    }

    /**
     * 로그 메시지용 문자열을 최대 길이로 잘라 표시한다.
     * <p>
     * 비즈니스 원칙: 활동 로그에 긴 목표 텍스트 전체를 노출하지 않고 요약한다.
     * </p>
     *
     * @param text 원본 문자열
     * @param max  최대 글자 수
     * @return 잘린 문자열 (초과 시 "..." 접미사)
     */
    private String shorten(String text, int max) {

        if (text.length() <= max) {
            return text;
        }

        return text.substring(0, max) + "...";
    }
}

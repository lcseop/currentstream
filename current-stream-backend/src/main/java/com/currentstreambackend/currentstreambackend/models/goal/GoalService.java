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
     * 목표 생성 서비스 로직
     * @param uid
     * @param teamId
     * @param text
     * @param remark
     * @param endDate
     * @return 생성한 목표 행을 불러옴
     */
    public GoalDto createGoal(String uid, Long teamId, String text, String remark, LocalDate endDate) {
        // 사용자 행을 불러옴
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));

        // 목표가 할당된 사용자가 해당 팀에 있는지 확인
        boolean isMember = mappingRepository.existsByUserIdAndTeamId(user.getId(), teamId);
        if (!isMember) throw new RuntimeException("Not team user");

        // 새로운 목표 엔티티 생성 후, 받아온 값들을 집어넣음
        GoalEntity goal = new GoalEntity();
        goal.setGoalText(text);
        goal.setRemark(remark);
        goal.setStatus(0);
        goal.setGoalEndDate(endDate);
        goal.setUserId(user.getId());
        goal.setTeamId(teamId);

        teamLogsService.createLog(
                teamId,
                user.getId(),
                user.getName() + "님에게 " + shorten(goal.getGoalText(), 10) + "목표가 추가되었습니다."
        );

        return GoalDto.fromEntity(goalRepository.save(goal));
    }

    /**
     * 목표의 상태 변경 로직
     * @param uid
     * @param goalId
     * @param status
     */
    public void updateStatus(String uid, Long goalId, Integer status) {
        // 사용자, 목표, 팀 행을 불러옴
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));
        GoalEntity goal = goalRepository.findById(goalId).orElseThrow(() -> new RuntimeException("Goal not found"));
        TeamsEntity team = teamsRepository.findById(goal.getTeamId()).orElseThrow(() -> new RuntimeException("Team not found"));

        // 변경할 상태의 목표가 할당된 사용자가 해당 팀에 있는지 확인
        boolean isMember = mappingRepository.existsByUserIdAndTeamId(user.getId(), goal.getTeamId());
        if (!isMember) throw new RuntimeException("Not team user");

        // 팀장, 해당 사용자만 상태를 변경할 수 있도록 예외 처리
        if (!team.getLeaderId().equals(user.getId())
                && !goal.getUserId().equals(user.getId())) {
            throw new RuntimeException("No premission");
        }

        switch(status) {
            case 0 :
                teamLogsService.createLog(
                        team.getId(),
                        user.getId(),
                        user.getName() + "님의 " + shorten(goal.getGoalText(), 10) + "목표가 진행 중으로 바뀌었습니다."
                );
                break;
            case 1 :
                teamLogsService.createLog(
                        team.getId(),
                        user.getId(),
                        user.getName() + "님이 " + shorten(goal.getGoalText(), 10) + "목표를 달성하셨습니다."
                );
                break;
            case 2 :
                teamLogsService.createLog(
                        team.getId(),
                        user.getId(),
                        user.getName() + "님의 " + shorten(goal.getGoalText(), 10) + "목표가 삭제되었습니다."
                );
                break;
        }

        // 상태 변경
        goal.setStatus(status);
        goalRepository.save(goal);
    }

    /**
     * 목표 삭제 로직
     * @param uid
     * @param goalId
     */
    public void deleteGoal(String uid, Long goalId) {
        // 사용자, 목표, 팀 행을 불러옴
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));
        GoalEntity goal = goalRepository.findById(goalId).orElseThrow(() -> new RuntimeException("Goal not found"));
        TeamsEntity team = teamsRepository.findById(goal.getTeamId()).orElseThrow(() -> new RuntimeException("Team not found"));

        // 팀장, 해당 사용자만 목표를 삭제할 수 있도록 예외 처리
        if (!team.getLeaderId().equals(user.getId())
                && !goal.getUserId().equals(user.getId())) {
            throw new RuntimeException("No premission");
        }

        teamLogsService.createLog(
                team.getId(),
                user.getId(),
                user.getName() + "님의 " + shorten(goal.getGoalText(), 10) + "목표가 삭제되었습니다."
        );

        goalRepository.deleteById(goalId);
    }

    /**
     * 목표 받아오기
     * @param teamId
     * @return 사용자가 가지고 있는 목표 리스트를 불러옴
     */
    public List<GoalDto> getGoals(String uid, Long teamId) {
        // 사용자 행을 불러옴
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));

        return goalRepository.findByTeamIdAndUserId(teamId, user.getId())
                .stream()
                .map(GoalDto::fromEntity)
                .toList();
    }

    private String shorten(String text, int max) {

        if (text.length() <= max) {
            return text;
        }

        return text.substring(0, max) + "...";
    }
}
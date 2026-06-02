package com.currentstreambackend.currentstreambackend.models.teamlogs;

import com.currentstreambackend.currentstreambackend.models.mapping.MappingRepository;
import com.currentstreambackend.currentstreambackend.models.users.UsersEntity;
import com.currentstreambackend.currentstreambackend.models.users.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class TeamLogsService {

    private static final ZoneId SERVER_ZONE = ZoneId.of("Asia/Seoul");

    @Autowired
    private TeamLogsRepository teamLogsRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private MappingRepository mappingRepository;

    /**
     * 로그 생성
     */
    public void createLog(Long teamId, Long userId, String message) {

        TeamLogsEntity log = new TeamLogsEntity();

        log.setTeamId(teamId);
        log.setUserId(userId);
        log.setMessage(message);
        log.setCreatedAt(LocalDateTime.now(SERVER_ZONE));

        teamLogsRepository.save(log);
    }

    /**
     * 팀 최근 로그 조회 (팀원 전용)
     */
    public List<TeamLogsDto> getTeamLogs(String uid, Long teamId) {
        UsersEntity user = usersRepository.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!mappingRepository.existsByUserIdAndTeamId(user.getId(), teamId)) {
            throw new RuntimeException("Not team user");
        }

        return teamLogsRepository
                .findTop10ByTeamIdOrderByCreatedAtDesc(teamId)
                .stream()
                .map(TeamLogsDto::fromEntity)
                .toList();
    }
}
package com.currentstreambackend.currentstreambackend.models.teamlogs;

import com.currentstreambackend.currentstreambackend.models.mapping.MappingRepository;
import com.currentstreambackend.currentstreambackend.models.users.UsersEntity;
import com.currentstreambackend.currentstreambackend.models.users.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 팀 활동 로그 비즈니스 로직을 담당하는 서비스 계층.
 * <p>
 * TeamsService, GoalService 등 다른 도메인 서비스에서 팀 이벤트(생성, 참가, 목표 변경 등) 발생 시
 * {@link #createLog}를 호출해 기록을 남기고, 클라이언트는 {@link #getTeamLogs}로 최근 이력을 조회한다.
 * </p>
 */
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
     * 팀 활동 로그를 생성해 저장한다.
     * <p>
     * 비즈니스 원칙: 생성 시각은 서버 기준 Asia/Seoul 타임존을 사용하며,
     * 호출 측(팀·목표 서비스)이 메시지 문구를 결정한다.
     * </p>
     *
     * @param teamId  로그가 속한 팀 ID
     * @param userId  행위를 수행한 사용자 ID
     * @param message 클라이언트에 표시할 활동 메시지
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
     * 팀의 최근 활동 로그 최대 10건을 조회한다.
     * <p>
     * 비즈니스 규칙: 요청자(uid)가 해당 팀의 멤버(mapping 존재)인 경우에만 조회를 허용한다.
     * 비멤버 접근 시 {@code Not team user} 예외가 발생한다.
     * </p>
     *
     * @param uid    Firebase UID (요청 헤더)
     * @param teamId 조회 대상 팀 ID
     * @return 생성 시각 내림차순 정렬된 로그 DTO 목록
     */
    public List<TeamLogsDto> getTeamLogs(String uid, Long teamId) {
        UsersEntity user = usersRepository.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // [중요] 팀 멤버십 검증 — 비멤버의 로그 열람 차단
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

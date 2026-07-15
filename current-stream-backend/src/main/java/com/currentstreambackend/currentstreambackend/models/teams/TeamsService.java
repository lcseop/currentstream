package com.currentstreambackend.currentstreambackend.models.teams;

import com.currentstreambackend.currentstreambackend.models.goal.GoalRepository;
import com.currentstreambackend.currentstreambackend.models.invite.InviteDto;
import com.currentstreambackend.currentstreambackend.models.invite.InviteEntity;
import com.currentstreambackend.currentstreambackend.models.invite.InviteRepository;
import com.currentstreambackend.currentstreambackend.models.mapping.MappingEntity;
import com.currentstreambackend.currentstreambackend.models.mapping.MappingRepository;
import com.currentstreambackend.currentstreambackend.models.teamlogs.TeamLogsRepository;
import com.currentstreambackend.currentstreambackend.models.teamlogs.TeamLogsService;
import com.currentstreambackend.currentstreambackend.models.users.UsersEntity;
import com.currentstreambackend.currentstreambackend.models.users.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 팀 도메인 비즈니스 로직을 담당하는 서비스 계층.
 * <p>
 * 팀 생성·조회·수정·삭제, 멤버 초대/수락/거절, 탈퇴 등 팀 생명주기 전반을 처리한다.
 * 사용자-팀 관계는 {@link MappingRepository}, 초대는 {@link InviteRepository}로 관리하며,
 * 주요 이벤트는 {@link TeamLogsService}를 통해 활동 로그로 기록한다.
 * </p>
 */
@Service
public class TeamsService {
    @Autowired
    private TeamsRepository teamsRepository;

    @Autowired
    private MappingRepository mappingRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private InviteRepository inviteRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private TeamLogsService teamLogsService;

    @Autowired
    private TeamLogsRepository teamLogsRepository;

    private static final String[] COLORS = {
            "#FF6B6B", "#4ECDC4", "#45B7D1",
            "#96CEB4", "#FFEAA7", "#DDA0DD",
            "#FF9F43", "#54A0FF"
    };


    /**
     * 새 팀을 생성하고 생성자를 팀장·첫 멤버로 등록한다.
     * <p>
     * 비즈니스 규칙: 팀 생성자가 자동으로 leaderId가 되며, mapping 테이블에 기본 색상으로 추가된다.
     * 팀 생성 이벤트는 활동 로그에 기록된다.
     * </p>
     *
     * @param uid     Firebase UID (요청자 = 팀장)
     * @param name    팀 이름
     * @param endDate 팀 종료 예정일
     * @return 생성된 팀 DTO
     */
    public TeamsDto createTeam(String uid, String name, LocalDate endDate) {
        // 사용자 행을 불러옴
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));

        // 새로운 팀 엔티티 생성 후, 받아온 값들을 집어넣음
        TeamsEntity team = new TeamsEntity();
        team.setTeamName(name);
        team.setEndDate(endDate);
        team.setLeaderId(user.getId());

        TeamsEntity saved = teamsRepository.save(team);

        // 매핑 테이블에도 추가함
        MappingEntity mapping = new MappingEntity();
        mapping.setUserId(user.getId());
        mapping.setTeamId(saved.getId());
        mapping.setUserColor("#757575");

        mappingRepository.save(mapping);

        teamLogsService.createLog(
                saved.getId(),
                user.getId(),
                user.getName() + "(" + user.getTag() + ")" + "님이 팀을 생성했습니다."
        );

        return TeamsDto.fromEntity(saved);
    }

    /**
     * 요청자가 소속된 모든 팀 목록을 조회한다.
     * <p>
     * 비즈니스 원칙: mapping 테이블을 기준으로 사용자-팀 관계를 역추적한다.
     * </p>
     *
     * @param uid Firebase UID
     * @return 소속 팀 DTO 목록
     */
    public List<TeamsDto> getMyTeams(String uid) {
        // 사용자 행을 불러옴
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));

        return mappingRepository.findByUserId(user.getId())
                .stream()
                .map(m -> teamsRepository.findById(m.getTeamId()).orElse(null))
                .filter(Objects::nonNull)
                .map(TeamsDto::fromEntity)
                .toList();
    }

    /**
     * 팀장이 태그로 사용자를 팀에 초대한다.
     * <p>
     * 비즈니스 규칙: 팀장만 초대 가능. 이미 팀원이거나 대기 중(status=0) 초대가 있으면 거부한다.
     * 초대는 status=0(대기)으로 저장되며, 목록 표시용 teamName·inviterName을 함께 보관한다.
     * </p>
     *
     * @param uid    Firebase UID (팀장)
     * @param teamId 초대할 팀 ID
     * @param tag    초대 대상 사용자 태그
     */
    public void inviteUser(String uid, Long teamId, String tag) {
        // 사용자와 팀 행을 불러옴
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));
        TeamsEntity team = teamsRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team not found"));

        // [중요] 팀장 권한 검증 — 팀장만 초대 가능
        if (!team.getLeaderId().equals(user.getId())) {
            throw new RuntimeException("Not Leader");
        }

        // 태그로 초대 대상을 찾음
        UsersEntity target = usersRepository.findByTag(tag).orElseThrow(() -> new RuntimeException("Target not found"));

        // [중요] 중복 팀원 검증
        if (mappingRepository.existsByUserIdAndTeamId(target.getId(), teamId)) {
            throw new RuntimeException("Already in team");
        }

        // [중요] 중복 초대 검증 — 대기(status=0) 중인 초대가 있으면 거부
        boolean exists = inviteRepository.existsByTeamIdAndUserIdAndStatus(teamId, target.getId(), 0);
        if (exists) throw new RuntimeException("Already invited");

        // 새로운 초대 객체를 생성
        InviteEntity invite = new InviteEntity();
        invite.setTeamId(teamId);
        invite.setUserId(target.getId());
        invite.setStatus(0);
        // 초대 목록에서 바로 보여주기 위한 표시용 값 저장
        invite.setTeamName(team.getTeamName());
        invite.setInviterName(user.getName());

        inviteRepository.save(invite);
    }

    /**
     * 초대를 수락하고 팀 멤버로 등록한다.
     * <p>
     * 비즈니스 규칙: 본인에게 온 대기(status=0) 초대만 수락 가능.
     * 수락 시 mapping에 고유 색상을 부여하고, 초대 상태를 1(수락)로 변경한다.
     * </p>
     *
     * @param uid      Firebase UID (초대 수신자)
     * @param inviteId 수락할 초대 ID
     */
    public void acceptInvite(String uid, Long inviteId) {
        // 사용자와 초대 행을 불러옴
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));
        InviteEntity invite = inviteRepository.findById(inviteId).orElseThrow(() -> new RuntimeException("Invite not found"));

        // [중요] 초대 수신자 본인 확인
        if (!invite.getUserId().equals(user.getId())) {
            throw new RuntimeException("Not your invited");
        }

        // [중요] 초대 상태 검증 — 대기(status=0)만 수락 가능
        if (invite.getStatus() != 0) {
            throw new RuntimeException("Invalid invite status");
        }

        if (mappingRepository.existsByUserIdAndTeamId(user.getId(), invite.getTeamId())) {
            throw new RuntimeException("Already in team");
        }

        // 팀 내 고유 컬러 선택
        String color = generateUniqueColor(invite.getTeamId());

        // 팀에 매핑 추가
        MappingEntity mapping = new MappingEntity();
        mapping.setUserId(user.getId());
        mapping.setTeamId(invite.getTeamId());
        mapping.setUserColor(color);

        mappingRepository.save(mapping);

        // 상태 값을 1 (수락)으로 변경
        invite.setStatus(1);
        inviteRepository.save(invite);

        teamLogsService.createLog(
                invite.getTeamId(),
                user.getId(),
                user.getName() + "님이 참가했습니다."
        );
    }

    /**
     * 초대를 거절한다.
     * <p>
     * 비즈니스 규칙: 본인에게 온 대기(status=0) 초대만 거절 가능하며, 상태를 2(거절)로 변경한다.
     * </p>
     *
     * @param uid      Firebase UID (초대 수신자)
     * @param inviteId 거절할 초대 ID
     */
    public void rejectInvite(String uid, Long inviteId) {
        // 사용자와 초대 행을 불러옴
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));
        InviteEntity invite = inviteRepository.findById(inviteId).orElseThrow(() -> new RuntimeException("Invite not found"));

        // [중요] 초대 수신자 본인 확인
        if (!invite.getUserId().equals(user.getId())) {
            throw new RuntimeException("Not your invited");
        }

        // [중요] 초대 상태 검증 — 대기(status=0)만 거절 가능
        if (!(invite.getStatus() == 0)) {
            throw new RuntimeException("Invalid invite status");
        }

        // 상태 값을 2 (거절)로 변경
        invite.setStatus(2);
        inviteRepository.save(invite);
    }

    /**
     * 요청자에게 온 대기 중인 초대 목록을 조회한다.
     * <p>
     * 비즈니스 원칙: status=0(대기)인 초대만 반환한다.
     * </p>
     *
     * @param uid Firebase UID
     * @return 수락/거절 가능한 초대 DTO 목록
     */
    public List<InviteDto> getMyInvites(String uid) {
        // 사용자 행을 불러옴
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));

        return inviteRepository.findByUserIdAndStatus(user.getId(), 0)
                .stream()
                .map(InviteDto::fromEntity)
                .toList();

    }

    /**
     * 팀 멤버 목록을 조회한다.
     * <p>
     * 비즈니스 규칙: 요청자가 해당 팀의 멤버인 경우에만 조회를 허용한다.
     * 각 멤버의 팀장 여부(leader)와 고유 색상(userColor)을 함께 반환한다.
     * </p>
     *
     * @param uid    Firebase UID
     * @param teamId 조회 대상 팀 ID
     * @return 팀 멤버 DTO 목록
     */
    public List<TeamMemberDto> getTeamMembers(String uid, Long teamId) {
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));
        TeamsEntity team = teamsRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team not found"));

        // [중요] 팀 멤버십 검증
        if (!mappingRepository.existsByUserIdAndTeamId(user.getId(), teamId)) {
            throw new RuntimeException("Not team user");
        }

        return mappingRepository.findByTeamId(teamId)
                .stream()
                .map(m -> {
                    UsersEntity member = usersRepository.findById(m.getUserId())
                            .orElseThrow(() -> new RuntimeException("Member not found"));
                    TeamMemberDto dto = new TeamMemberDto();
                    dto.setUserId(member.getId());
                    dto.setName(member.getName());
                    dto.setTag(member.getTag());
                    dto.setUserColor(m.getUserColor());
                    dto.setLeader(team.getLeaderId().equals(member.getId()));
                    return dto;
                })
                .toList();
    }

    /**
     * 팀 정보(이름, 종료일)를 수정한다.
     * <p>
     * 비즈니스 규칙: 팀장만 수정 가능하며, 변경 이벤트는 활동 로그에 기록된다.
     * </p>
     *
     * @param uid     Firebase UID (팀장)
     * @param teamId  수정 대상 팀 ID
     * @param name    새 팀 이름
     * @param endDate 새 종료 예정일
     * @return 수정된 팀 DTO
     */
    public TeamsDto updateTeam(String uid, Long teamId, String name, LocalDate endDate) {
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));
        TeamsEntity team = teamsRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team not found"));

        // [중요] 팀장 권한 검증
        if (!team.getLeaderId().equals(user.getId())) {
            throw new RuntimeException("Not leader");
        }

        team.setTeamName(name);
        team.setEndDate(endDate);

        teamLogsService.createLog(
                team.getId(),
                user.getId(),
                user.getName() + "님이 팀 정보를 수정했습니다."
        );

        return TeamsDto.fromEntity(teamsRepository.save(team));
    }

    /**
     * 팀에서 탈퇴한다.
     * <p>
     * 비즈니스 규칙: 팀장은 탈퇴할 수 없다(팀 삭제를 사용해야 함).
     * 탈퇴 시 mapping과 해당 팀의 개인 목표가 함께 삭제된다.
     * </p>
     *
     * @param uid    Firebase UID
     * @param teamId 탈퇴할 팀 ID
     */
    @Transactional
    public void leaveTeam(String uid, Long teamId) {
        // 사용자와 팀 행을 불러옴
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));
        TeamsEntity team = teamsRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team not found"));

        // [중요] 팀장 탈퇴 차단 — 팀장은 leave 불가, deleteTeam 사용
        if (team.getLeaderId().equals(user.getId())) {
            throw new RuntimeException("Leader cannot leave");
        }

        // [중요] 트랜잭션 내 멤버십·목표 일괄 삭제
        mappingRepository.deleteByUserIdAndTeamId(user.getId(), teamId);
        goalRepository.deleteByTeamIdAndUserId(teamId, user.getId());

        teamLogsService.createLog(
                team.getId(),
                user.getId(),
                user.getName() + "님이 팀을 탈퇴했습니다."
        );
    }

    /**
     * 팀과 연관 데이터를 모두 삭제한다.
     * <p>
     * 비즈니스 규칙: 팀장만 삭제 가능.
     * mapping, invite, goal, teamLog를 먼저 삭제한 뒤 팀 엔티티를 제거한다.
     * </p>
     *
     * @param uid    Firebase UID (팀장)
     * @param teamId 삭제할 팀 ID
     */
    @Transactional
    public void deleteTeam(String uid, Long teamId) {
        // 사용자와 팀 행을 불러옴
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));
        TeamsEntity team = teamsRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team not found"));

        // [중요] 팀장 권한 검증
        if (!team.getLeaderId().equals(user.getId())) {
            throw new RuntimeException("Not leader");
        }

        // [중요] 트랜잭션 내 연관 데이터 일괄 삭제 (참조 무결성)
        mappingRepository.deleteByTeamId(teamId);
        inviteRepository.deleteByTeamId(teamId);
        goalRepository.deleteByTeamId(teamId);
        teamLogsRepository.deleteByTeamId(teamId);

        // 팀 삭제
        teamsRepository.deleteById(teamId);

    }

    /**
     * 팀 내에서 멤버에게 부여할 고유 색상을 결정한다.
     * <p>
     * 비즈니스 원칙: 사전 정의 팔레트(COLORS)에서 미사용 색을 우선 선택하고,
     * 모두 사용 중이면 밝은 랜덤 RGB 색상을 생성한다.
     * </p>
     *
     * @param teamId 색상을 할당할 팀 ID
     * @return HEX 색상 코드
     */
    private String generateUniqueColor(Long teamId) {
        List<MappingEntity> mappings = mappingRepository.findByTeamId(teamId);

        // 사용중인 색들 세트로 만듦
        Set<String> usedColors = mappings.stream()
                .map(MappingEntity::getUserColor)
                .collect(Collectors.toSet());


        // 중복되지 않는 색 리스트로 만듦
        List<String> available = new ArrayList<>();

        for (String color : COLORS) {
            if (!usedColors.contains(color)) {
                available.add(color);
            }
        }

        // 남은 색이 있을 경우 그 중에서 랜덤 선택
        if (!available.isEmpty()) {
            int idx = (int)(Math.random() * available.size());
            return available.get(idx);
        }

        // 남은 색이 없을 경우 밝은 색 중에서 랜덤 선택
        int r = (int)(Math.random() * 156) + 100;
        int g = (int)(Math.random() * 156) + 100;
        int b = (int)(Math.random() * 156) + 100;

        return String.format("#%02X%02X%02X", r, g, b);
    }

}

package com.mjc813.currentstreambackend.models.teams;

import com.mjc813.currentstreambackend.models.goal.GoalRepository;
import com.mjc813.currentstreambackend.models.invite.InviteDto;
import com.mjc813.currentstreambackend.models.invite.InviteEntity;
import com.mjc813.currentstreambackend.models.invite.InviteRepository;
import com.mjc813.currentstreambackend.models.mapping.MappingEntity;
import com.mjc813.currentstreambackend.models.mapping.MappingRepository;
import com.mjc813.currentstreambackend.models.users.UsersEntity;
import com.mjc813.currentstreambackend.models.users.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    private static final String[] COLORS = {
            "#FF6B6B", "#4ECDC4", "#45B7D1",
            "#96CEB4", "#FFEAA7", "#DDA0DD",
            "#FF9F43", "#54A0FF"
    };


    /**
     * 팀 생성 서비스 로직
     * @param uid
     * @param name
     * @param endDate
     * @return TeamsDto -> 팀 행을 불러옴
     */
    public TeamsDto createTeam(String uid, String name, LocalDate endDate) {
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

        return TeamsDto.fromEntity(saved);
    }

    /**
     * 팀 조회 서비스 로직
     * @param uid
     * @return 내가 속한 팀들을 조회함
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
     * 팀에 유저 초대
     * @param uid
     * @param teamId
     * @param tag
     */
    public void inviteUser(String uid, Long teamId, String tag) {
        // 사용자와 팀 행을 불러옴
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));
        TeamsEntity team = teamsRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team not found"));

        // 초대 대상이 팀장인 경우 런타임 에러 발생
        if (!team.getLeaderId().equals(user.getId())) {
            throw new RuntimeException("Not Leader");
        }

        // 태그로 초대 대상을 찾음
        UsersEntity target = usersRepository.findByTag(tag).orElseThrow(() -> new RuntimeException("Target not found"));

        // 초대 대상이 팀에 이미 있는 경우 런타임 에러 발생
        boolean exists = inviteRepository.existsByTeamIdAndUserIdAndStatus(teamId, target.getId(), 0);
        if (exists) throw new RuntimeException("Already invited");

        // 새로운 초대 객체를 생성
        InviteEntity invite = new InviteEntity();
        invite.setTeamId(teamId);
        invite.setUserId(target.getId());
        invite.setStatus(0);

        inviteRepository.save(invite);
    }

    /**
     * 팀 초대 수락 로직
     * @param uid
     * @param inviteId
     */
    public void acceptInvite(String uid, Long inviteId) {
        // 사용자와 초대 행을 불러옴
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));
        InviteEntity invite = inviteRepository.findById(inviteId).orElseThrow(() -> new RuntimeException("Invite not found"));

        // 자신에게 할당된 초대가 맞는 지 예외 처리
        if (!invite.getUserId().equals(user.getId())) {
            throw new RuntimeException("Not your invited");
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
    }

    /**
     * 팀 초대 거절 로직
     * @param uid
     * @param inviteId
     */
    public void rejectInvite(String uid, Long inviteId) {
        // 사용자와 초대 행을 불러옴
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));
        InviteEntity invite = inviteRepository.findById(inviteId).orElseThrow(() -> new RuntimeException("Invite not found"));

        // 자신에게 할당된 초대가 맞는 지 예외 처리
        if (!invite.getUserId().equals(user.getId())) {
            throw new RuntimeException("Not your invited");
        }

        // 초대 상태 값이 '초대'가 아니고 수락, 거절일 경우 예외 처리
        if (!(invite.getStatus() == 0)) {
            throw new RuntimeException("Invalid invite status");
        }

        // 상태 값을 2 (거절)로 변경
        invite.setStatus(2);
        inviteRepository.save(invite);
    }

    /**
     * 팀 초대 목록 조회
     * @param uid
     * @return 수락/거절 가능한 초대들 불러옴
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
     * 팀 탈퇴
     * @param uid
     * @param teamId
     */
    public void leaveTeam(String uid, Long teamId) {
        // 사용자와 팀 행을 불러옴
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));
        TeamsEntity team = teamsRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team not found"));

        // 리더는 탈퇴 못하게 예외 처리
        if (team.getLeaderId().equals(user.getId())) {
            throw new RuntimeException("Leader cannot leave");
        }

        // mappingRepository를 이용하여 해당 사용자를 팀에서 제거함
        mappingRepository.deleteByUserIdAndTeamId(user.getId(), teamId);
    }

    /**
     * 팀 삭제 (팀장 전용)
     * @param uid
     * @param teamId
     */
    public void deleteTeam(String uid, Long teamId) {
        // 사용자와 팀 행을 불러옴
        UsersEntity user = usersRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("User not found"));
        TeamsEntity team = teamsRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team not found"));

        // 팀장만 삭제 가능하게 예외 처리
        if (!team.getLeaderId().equals(user.getId())) {
            throw new RuntimeException("Not leader");
        }

        // 연관된 데이터들을 삭제함
        mappingRepository.deleteByTeamId(teamId);
        inviteRepository.deleteByTeamId(teamId);
        goalRepository.deleteByTeamId(teamId);

        // 팀 삭제
        teamsRepository.deleteById(teamId);

    }

    /**
     * private 팀 내에서 색상 결정
     * @param teamId
     * @return 팀 내의 유저 고유 색상을 랜덤 설정하고 불러온 컬러 코드
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

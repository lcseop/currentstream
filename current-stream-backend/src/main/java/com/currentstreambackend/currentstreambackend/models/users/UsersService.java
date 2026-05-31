package com.currentstreambackend.currentstreambackend.models.users;

import com.currentstreambackend.currentstreambackend.models.common.EmailNotVerifiedException;
import com.currentstreambackend.currentstreambackend.models.goal.GoalRepository;
import com.currentstreambackend.currentstreambackend.models.invite.InviteRepository;
import com.currentstreambackend.currentstreambackend.models.mapping.MappingEntity;
import com.currentstreambackend.currentstreambackend.models.mapping.MappingRepository;
import com.currentstreambackend.currentstreambackend.models.teams.TeamsEntity;
import com.currentstreambackend.currentstreambackend.models.teams.TeamsRepository;
import com.currentstreambackend.currentstreambackend.models.teams.TeamsService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UsersService {
    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private MappingRepository mappingRepository;

    @Autowired
    private TeamsRepository teamsRepository;

    @Autowired
    private TeamsService teamsService;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private InviteRepository inviteRepository;

    /**
     * 회원가입 요청을 처리하는 로직
     * @param idToken
     * @param name
     * @return 새로운 유저 리턴
     * @throws Exception
     */
    public UsersDto signup(String idToken, String name) throws Exception {

        FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(idToken);

        String uid = token.getUid();
        String email = token.getEmail();

        if (usersRepository.findByUid(uid).isPresent()) {
            throw new RuntimeException("ALREADY_EXISTS");
        }

        UsersEntity user = new UsersEntity();
        user.setUid(uid);
        user.setEmail(email);
        user.setName(name);
        user.setTag(generateTag(email));

        usersRepository.save(user);

        return (UsersDto) new UsersDto().copyMembers(user, true);
    }

    /**
     * 로그인 요청을 처리하는 로직
     * @param idToken
     * @return 로그인한 유저 리턴
     * @throws Exception
     */
    public UsersDto login(String idToken) throws Exception {

        FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(idToken);

        String uid = token.getUid();
        String email = token.getEmail();

        Map<String, Object> firebaseClaims =
                (Map<String, Object>) token.getClaims().get("firebase");

        String provider = null;
        if (firebaseClaims != null) {
            provider = (String) firebaseClaims.get("sign_in_provider");
        }

        // 이메일 로그인만 인증 체크
        if ("password".equals(provider) && !token.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }

        Optional<UsersEntity> optional = usersRepository.findByUid(uid);

        UsersEntity user = optional.orElseGet(() -> {

            UsersEntity newUser = new UsersEntity();
            newUser.setUid(uid);
            newUser.setEmail(email);

            String name = token.getName();
            if (name == null || name.isEmpty()) {
                name = email.split("@")[0];
            }

            newUser.setName(name);
            newUser.setTag(generateTag(email));

            return usersRepository.save(newUser);
        });

        return (UsersDto) new UsersDto().copyMembers(user, true);
    }

    /**
     * tag로 사용자 조회 (팀원 초대 전 확인용)
     * @param tag
     * @return 해당 tag를 가진 사용자
     */
    public UsersDto findByTag(String tag) {
        UsersEntity user = usersRepository.findByTag(tag)
                .orElseThrow(() -> new RuntimeException("Target not found"));
        return (UsersDto) new UsersDto().copyMembers(user, true);
    }

    /**
     * 회원 탈퇴: 팀장인 팀은 삭제, 소속 팀은 탈퇴 처리 후 계정 삭제
     */
    @Transactional
    public void deleteAccount(String uid) {
        UsersEntity user = usersRepository.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<MappingEntity> mappings = new ArrayList<>(mappingRepository.findByUserId(user.getId()));

        for (MappingEntity mapping : mappings) {
            TeamsEntity team = teamsRepository.findById(mapping.getTeamId()).orElse(null);
            if (team == null) {
                mappingRepository.deleteByUserIdAndTeamId(user.getId(), mapping.getTeamId());
                continue;
            }
            if (team.getLeaderId().equals(user.getId())) {
                teamsService.deleteTeam(uid, team.getId());
            } else {
                goalRepository.findByTeamIdAndUserId(team.getId(), user.getId())
                        .forEach(goal -> goalRepository.deleteById(goal.getId()));
                mappingRepository.deleteByUserIdAndTeamId(user.getId(), team.getId());
            }
        }

        inviteRepository.deleteByUserId(user.getId());
        usersRepository.deleteById(user.getId());
    }

    /**
     * 태그 자동 생성
     * @param email
     * @return 랜덤 생성된 태그 리턴
     */
    private String generateTag(String email) {
        // 이메일에서 앞 부분 추출
        String prefix = email.split("@")[0];
        String tag;

        // Repository에서 태그 생성 후 중복 확인 반복
        do {
            int random = (int)(Math.random() * 9000) + 1000;
            tag = prefix + "#" + random;
        } while (usersRepository.existsByTag(tag));

        return tag;
    }

}

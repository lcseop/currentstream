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

/**
 * 사용자 계정 도메인 비즈니스 로직을 담당하는 서비스 계층.
 * <p>
 * Firebase ID 토큰 기반 인증(회원가입·로그인), 태그 조회, 회원 탈퇴를 처리한다.
 * 인증 식별자는 Firebase UID이며, 앱 내 고유 식별용 태그(email#랜덤)를 자동 생성한다.
 * </p>
 */
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
     * 신규 회원가입을 처리한다.
     * <p>
     * 비즈니스 규칙:
     * <ul>
     *   <li>Firebase ID 토큰을 검증해 uid·email을 추출한다.</li>
     *   <li>동일 uid가 이미 존재하면 {@code ALREADY_EXISTS} 예외를 던진다.</li>
     *   <li>태그는 이메일 접두사 + 랜덤 4자리로 자동 생성한다.</li>
     * </ul>
     * </p>
     *
     * @param idToken Firebase ID 토큰
     * @param name    사용자 표시 이름
     * @return 생성된 사용자 DTO
     */
    public UsersDto signup(String idToken, String name) throws Exception {

        // [중요] Firebase ID 토큰 검증 — 인증되지 않은 요청 차단
        FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(idToken);

        String uid = token.getUid();
        String email = token.getEmail();

        // [중요] 중복 가입 방지
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
     * 로그인을 처리하고 사용자 정보를 반환한다.
     * <p>
     * 비즈니스 규칙:
     * <ul>
     *   <li>Firebase ID 토큰 검증 후 uid로 사용자를 조회한다.</li>
     *   <li>이메일/비밀번호 로그인은 이메일 인증 완료 후에만 허용한다.</li>
     *   <li>미등록 사용자는 자동 생성(소셜 로그인 최초 접속 시).</li>
     * </ul>
     * </p>
     *
     * @param idToken Firebase ID 토큰
     * @return 로그인한 사용자 DTO
     */
    public UsersDto login(String idToken) throws Exception {

        // [중요] Firebase ID 토큰 검증
        FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(idToken);

        String uid = token.getUid();
        String email = token.getEmail();

        Map<String, Object> firebaseClaims =
                (Map<String, Object>) token.getClaims().get("firebase");

        String provider = null;
        if (firebaseClaims != null) {
            provider = (String) firebaseClaims.get("sign_in_provider");
        }

        // [중요] 이메일/비밀번호 로그인 시 이메일 인증 여부 검증
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
     * 태그로 사용자를 조회한다 (팀원 초대 전 확인용).
     * <p>
     * 비즈니스 원칙: 태그는 앱 내 고유 식별자이며, 초대 대상 검색에 사용된다.
     * </p>
     *
     * @param tag 조회할 사용자 태그
     * @return 해당 태그를 가진 사용자 DTO
     */
    public UsersDto findByTag(String tag) {
        UsersEntity user = usersRepository.findByTag(tag)
                .orElseThrow(() -> new RuntimeException("Target not found"));
        return (UsersDto) new UsersDto().copyMembers(user, true);
    }

    /**
     * 회원 탈퇴를 처리한다.
     * <p>
     * 비즈니스 규칙:
     * <ul>
     *   <li>팀장인 팀은 {@link TeamsService#deleteTeam}으로 전체 삭제한다.</li>
     *   <li>일반 멤버는 mapping·개인 목표를 삭제해 탈퇴 처리한다.</li>
     *   <li>대기 중인 초대와 사용자 계정을 최종 삭제한다.</li>
     * </ul>
     * </p>
     *
     * @param uid Firebase UID (탈퇴 대상)
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
                // [중요] 팀장인 경우 팀 전체 삭제 위임
                teamsService.deleteTeam(uid, team.getId());
            } else {
                // [중요] 일반 멤버 — 개인 목표·멤버십만 삭제
                goalRepository.findByTeamIdAndUserId(team.getId(), user.getId())
                        .forEach(goal -> goalRepository.deleteById(goal.getId()));
                mappingRepository.deleteByUserIdAndTeamId(user.getId(), team.getId());
            }
        }

        inviteRepository.deleteByUserId(user.getId());
        usersRepository.deleteById(user.getId());
    }

    /**
     * 이메일 접두사 기반 고유 태그를 자동 생성한다.
     * <p>
     * 비즈니스 원칙: {@code email접두사#랜덤4자리} 형식이며, DB 중복 시 재생성한다.
     * </p>
     *
     * @param email 사용자 이메일
     * @return 고유 태그 문자열
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

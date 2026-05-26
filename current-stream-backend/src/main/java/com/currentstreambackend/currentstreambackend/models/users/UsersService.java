package com.currentstreambackend.currentstreambackend.models.users;

import com.currentstreambackend.currentstreambackend.models.common.EmailNotVerifiedException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

@Service
public class UsersService {
    @Autowired
    private UsersRepository usersRepository;

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

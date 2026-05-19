package com.mjc813.currentstreambackend.models.users;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsersService {
    @Autowired
    private UsersRepository usersRepository;

    // 로그인, 회원가입 요청을 처리하는 서비스
    public UsersDto loginOrRegister(String idToken) throws FirebaseAuthException {
        FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(idToken);

        String uid = token.getUid();
        String email = token.getEmail();

        if (!token.isEmailVerified()) {
            throw new RuntimeException("EMAIL_NOT_VERIFIED");
        }

        Optional<UsersEntity> optional = usersRepository.findByUid(uid);

        // 회원가입 부분
        UsersEntity user = optional.orElseGet(() -> {
           UsersEntity newUser = new UsersEntity();
           newUser.setUid(uid);
           newUser.setEmail(email);
           newUser.setTag(generateTag(email));
           return usersRepository.save(newUser);
        });

        return (UsersDto) new UsersDto().copyMembers(user, true);
    }



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

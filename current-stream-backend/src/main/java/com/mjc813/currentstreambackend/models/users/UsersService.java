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

        UsersEntity user = optional.orElseGet(() -> {
           UsersEntity newUser = new UsersEntity();
           newUser.setUid(uid);
           newUser.setEmail(email);
           return usersRepository.save(newUser);
        });

        return (UsersDto) new UsersDto().copyMembers(user, true);
    }
}

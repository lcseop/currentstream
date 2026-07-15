package com.currentstreambackend.currentstreambackend.models.users;

import com.google.firebase.auth.FirebaseAuthException;
import com.currentstreambackend.currentstreambackend.models.common.ApiResponse;
import com.currentstreambackend.currentstreambackend.models.common.ResponseCode;
import com.currentstreambackend.currentstreambackend.models.common.TokenRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 회원가입·로그인·태그 검색·회원 탈퇴 HTTP 엔드포인트.
 * <p>
 * 로그인은 Firebase ID 토큰을 받아 {@link UsersService}에서 Admin SDK로 검증한 뒤
 * DB 사용자 정보(uid, tag, userId)를 {@link ApiResponse}로 반환합니다.
 * </p>
 */
@RestController
@RequestMapping("/api/user")
public class UsersRestController {
    @Autowired
    private UsersService usersService;

    /**
     * 회원가입 요청 처리
     * @param body
     * @return 새로 회원가입된 유저를 리턴
     * @throws Exception
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UsersDto>> signup(
            @RequestBody Map<String, String> body) throws Exception {

        UsersDto result = usersService.signup(
                body.get("idToken"),
                body.get("name")
        );

        return ResponseEntity.status(201).body(
                ApiResponse.make(ResponseCode.insert_ok, "signup success", result)
        );
    }

    /**
     * 로그인 요청 처리
     * @param request
     * @return 로그인된 유저를 리턴
     * @throws Exception
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UsersDto>> login(
            @RequestBody TokenRequest request) throws Exception {

        UsersDto result = usersService.login(request.getIdToken());

        return ResponseEntity.ok(
                ApiResponse.make(ResponseCode.select_ok, "login success", result)
        );
    }

    /**
     * tag로 사용자 존재 여부 확인
     * @param tag
     * @return tag에 해당하는 사용자 정보
     */
    @GetMapping("/tag")
    public ResponseEntity<ApiResponse<UsersDto>> findByTag(@RequestParam String tag) {
        UsersDto result = usersService.findByTag(tag);
        return ResponseEntity.ok(
                ApiResponse.make(ResponseCode.select_ok, "user found", result)
        );
    }

    /**
     * 회원 탈퇴
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@RequestHeader("uid") String uid) {
        usersService.deleteAccount(uid);
        return ResponseEntity.ok(
                ApiResponse.make(ResponseCode.delete_ok, "account deleted", null)
        );
    }
}

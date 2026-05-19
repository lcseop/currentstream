package com.mjc813.currentstreambackend.models.users;

import com.google.firebase.auth.FirebaseAuthException;
import com.mjc813.currentstreambackend.models.common.ApiResponse;
import com.mjc813.currentstreambackend.models.common.ResponseCode;
import com.mjc813.currentstreambackend.models.common.TokenRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UsersRestController {
    @Autowired
    private UsersService usersService;

    @PostMapping("/login")
        public ResponseEntity<ApiResponse<UsersDto>> login (@RequestBody TokenRequest request)
                                throws FirebaseAuthException {
        UsersDto result = this.usersService.loginOrRegister(request.getIdToken());
        return ResponseEntity.status(201).body(ApiResponse.make(
                ResponseCode.select_ok, "login success", result
        ));
    }
}

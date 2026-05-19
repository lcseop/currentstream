package com.mjc813.currentstreambackend.models.common;

import com.google.firebase.auth.FirebaseAuthException;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/*
    요청 응답 후 오류 발생 시 사용자는 오류 전체를 받을 필요가 없으므로
    오류를 그대로 받지 않고 대체해서 받음
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FirebaseAuthException.class)
    public ResponseEntity<ApiResponse<String>> handleFirebase() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.make(ResponseCode.failed, "firebase Error", null)
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<String>> handleRuntime(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.make(ResponseCode.failed, "runtime error", e.getMessage())
        );
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiResponse<String>> handleException(Throwable ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.make(ResponseCode.failed, "response failed", ex.getMessage())
        );
    }
}

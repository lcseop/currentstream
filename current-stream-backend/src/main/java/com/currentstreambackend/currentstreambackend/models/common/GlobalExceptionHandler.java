package com.currentstreambackend.currentstreambackend.models.common;

import com.google.firebase.auth.FirebaseAuthException;
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

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiResponse<String>> handleEmailNotVerified() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.make(ResponseCode.failed, "email not verified", "EMAIL_NOT_VERIFIED")
        );
    }

    @ExceptionHandler(FirebaseAuthException.class)
    public ResponseEntity<ApiResponse<String>> handleFirebase() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.make(ResponseCode.failed, "firebase Error", null)
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<String>> handleRuntime(RuntimeException e) {
        HttpStatus status = mapBusinessStatus(e.getMessage());
        String clientMessage = toClientMessage(e.getMessage());
        return ResponseEntity.status(status).body(
                ApiResponse.make(ResponseCode.failed, clientMessage, null)
        );
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiResponse<String>> handleException(Throwable ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.make(ResponseCode.failed, "요청 처리 중 오류가 발생했습니다.", null)
        );
    }

    private static HttpStatus mapBusinessStatus(String message) {
        if (message == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return switch (message) {
            case "User not found", "Team not found", "Target not found",
                 "Invite not found", "Member not found", "Goal not found" -> HttpStatus.NOT_FOUND;
            case "Not leader", "Not Leader", "Leader cannot leave", "Not your invited",
                 "Not team user", "No premission" -> HttpStatus.FORBIDDEN;
            case "Already in team", "Already invited", "Invalid invite status" -> HttpStatus.CONFLICT;
            case "Invalid status" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private static String toClientMessage(String message) {
        if (message == null) {
            return "요청 처리 중 오류가 발생했습니다.";
        }
        return switch (message) {
            case "User not found" -> "사용자를 찾을 수 없습니다.";
            case "Team not found" -> "팀을 찾을 수 없습니다.";
            case "Target not found" -> "초대 대상을 찾을 수 없습니다.";
            case "Invite not found" -> "초대를 찾을 수 없습니다.";
            case "Member not found" -> "멤버를 찾을 수 없습니다.";
            case "Goal not found" -> "목표를 찾을 수 없습니다.";
            case "Not leader", "Not Leader" -> "팀장만 수행할 수 있습니다.";
            case "Leader cannot leave" -> "팀장은 팀을 탈퇴할 수 없습니다.";
            case "Not your invited" -> "본인에게 온 초대만 처리할 수 있습니다.";
            case "Not team user" -> "팀 멤버만 접근할 수 있습니다.";
            case "No premission" -> "권한이 없습니다.";
            case "Already in team" -> "이미 팀에 속해 있습니다.";
            case "Already invited" -> "이미 초대된 사용자입니다.";
            case "Invalid invite status" -> "처리할 수 없는 초대 상태입니다.";
            case "Invalid status" -> "올바르지 않은 목표 상태입니다.";
            default -> "요청을 처리할 수 없습니다.";
        };
    }
}

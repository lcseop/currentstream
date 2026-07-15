package com.currentstreambackend.currentstreambackend.models.common;

import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기.
 * <p>
 * 컨트롤러·서비스에서 발생한 예외를 일관된 {@link ApiResponse} 형식으로 변환한다.
 * 내부 예외 메시지(영문)를 HTTP 상태 코드와 한국어 클라이언트 메시지로 매핑해
 * 스택 트레이스 등 민감 정보가 클라이언트에 노출되지 않도록 한다.
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 이메일 미인증 로그인 시도를 처리한다.
     * <p>
     * 비즈니스 규칙: 이메일/비밀번호 로그인은 Firebase 이메일 인증 완료 후에만 허용한다.
     * </p>
     */
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiResponse<String>> handleEmailNotVerified() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.make(ResponseCode.failed, "email not verified", "EMAIL_NOT_VERIFIED")
        );
    }

    /**
     * Firebase 인증 관련 오류를 처리한다.
     * <p>
     * 토큰 검증 실패 등 Firebase SDK 예외를 500 응답으로 통일한다.
     * </p>
     */
    @ExceptionHandler(FirebaseAuthException.class)
    public ResponseEntity<ApiResponse<String>> handleFirebase() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.make(ResponseCode.failed, "firebase Error", null)
        );
    }

    /**
     * 비즈니스 로직에서 던지는 {@link RuntimeException}을 처리한다.
     * <p>
     * 비즈니스 원칙: 예외 메시지 문자열을 키로 HTTP 상태와 한국어 메시지를 결정한다.
     * 서비스 계층은 의도적으로 영문 메시지를 사용해야 이 핸들러와 연동된다.
     * </p>
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<String>> handleRuntime(RuntimeException e) {
        HttpStatus status = mapBusinessStatus(e.getMessage());
        String clientMessage = toClientMessage(e.getMessage());
        return ResponseEntity.status(status).body(
                ApiResponse.make(ResponseCode.failed, clientMessage, null)
        );
    }

    /**
     * 그 외 모든 예기치 않은 예외를 처리한다.
     * <p>
     * 처리되지 않은 예외는 내부 오류(500)로 응답하며, 상세 내용은 클라이언트에 노출하지 않는다.
     * </p>
     */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiResponse<String>> handleException(Throwable ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.make(ResponseCode.failed, "요청 처리 중 오류가 발생했습니다.", null)
        );
    }

    /**
     * 서비스 계층 예외 메시지를 HTTP 상태 코드로 매핑한다.
     * <p>
     * 비즈니스 규칙: 404(미존재), 403(권한 없음), 409(충돌), 400(잘못된 요청)으로 분류한다.
     * </p>
     */
    private static HttpStatus mapBusinessStatus(String message) {
        if (message == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        // [중요] 예외 메시지 → HTTP 상태 코드 매핑 (서비스 계층과 문자열 일치 필수)
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

    /**
     * 서비스 계층 예외 메시지를 클라이언트용 한국어 메시지로 변환한다.
     * <p>
     * 비즈니스 원칙: 사용자에게는 내부 영문 메시지 대신 이해하기 쉬운 한국어 안내를 제공한다.
     * </p>
     */
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

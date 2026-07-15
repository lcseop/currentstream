package com.currentstreambackend.currentstreambackend.models.common;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder

/**
 * 모든 REST API의 공통 응답 래퍼.
 * <p>
 * Android {@code ApiHelper.isSuccess}는 responseCode가 {@code *_ok} 인지로 성공을 판별합니다.
 * 실패 시에도 HTTP 200이 아닐 수 있으며, message에 사용자용 문구가 담깁니다.
 * </p>
 *
 * @param <T> responseData 페이로드 타입 (DTO, List, Void 등)
 */
public class ApiResponse<T> {
    private ResponseCode responseCode;
    private String message;
    private T responseData;

    /** Controller에서 일관된 JSON 형태로 응답을 조립할 때 사용 */
    public static <T> ApiResponse<T> make(ResponseCode responseCode, String message, T responseData) {
        return new ApiResponse<T>(responseCode, message, responseData);
    }
}

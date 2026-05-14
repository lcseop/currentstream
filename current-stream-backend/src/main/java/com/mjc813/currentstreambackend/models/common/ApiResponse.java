package com.mjc813.currentstreambackend.models.common;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder

/*
    데이터를 받을 때 오류 코드, 메시지, 데이터(JSON) 등을 전달받음
 */
public class ApiResponse<T> {
    private ResponseCode responseCode;
    private String message;
    private T responseData;

    public static <T> ApiResponse<T> make(ResponseCode responseCode, String message, T responseData) {
        return new ApiResponse<T>(responseCode, message, responseData);
    }
}

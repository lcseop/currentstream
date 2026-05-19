package com.mjc813.currentstreambackend.models.common;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor

// 안드로이드에서 보내오는 JSON을 받아들이는 클래스
public class TokenRequest {
    private String idToken;
}

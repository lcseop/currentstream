package com.currentstreambackend.currentstreambackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security HTTP 보안 설정.
 * <p>
 * 모바일 앱은 쿠키 세션이 아니라 요청마다 {@code uid} 헤더로 사용자를 식별합니다.
 * 현재는 모든 /api/** 를 permitAll 하고, 권한 검증은 각 Service에서 수행합니다.
 * </p>
 */
@Configuration
public class SecurityConfig {

    /**
     * CSRF 비활성화 + API 전 경로 허용 필터 체인.
     * <p>
     * [중요] permitAll 이므로 uid 헤더 위조가 가능합니다.
     * 장기적으로는 Firebase ID 토큰 검증 필터를 이 체인 앞에 두는 것이 안전합니다.
     * </p>
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").permitAll()
                        .anyRequest().permitAll()
                );

        return http.build();
    }

}
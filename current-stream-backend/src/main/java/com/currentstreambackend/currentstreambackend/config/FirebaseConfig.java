package com.currentstreambackend.currentstreambackend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Firebase Admin SDK 초기화 설정.
 * <p>
 * 서버에서 Firebase ID 토큰 검증(로그인 API)에 사용합니다.
 * credentials는 환경변수 {@code FIREBASE_CREDENTIALS_PATH} 또는 classpath json을 읽습니다.
 * </p>
 */
@Configuration
public class FirebaseConfig {

    /**
     * 앱 기동 시 FirebaseApp 싱글톤을 등록합니다.
     * 이미 초기화된 경우 중복 등록을 피합니다.
     */
    @PostConstruct
    public void init() throws IOException {
        InputStream serviceAccount = openCredentialsStream();

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }

    private InputStream openCredentialsStream() throws IOException {
        String credentialsPath = System.getenv("FIREBASE_CREDENTIALS_PATH");
        if (credentialsPath != null && !credentialsPath.isBlank()) {
            Path path = Path.of(credentialsPath);
            if (!Files.isRegularFile(path)) {
                throw new IOException("Firebase credentials file not found: " + credentialsPath);
            }
            return new FileInputStream(path.toFile());
        }

        return new ClassPathResource("firebase/serviceAccountKey.json").getInputStream();
    }
}

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

@Configuration
public class FirebaseConfig {

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

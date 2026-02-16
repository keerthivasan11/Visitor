package com.smartsecurity.system.config;

import org.springframework.context.annotation.Configuration;

import org.springframework.core.io.ClassPathResource;

import com.google.firebase.FirebaseOptions;
import com.google.firebase.FirebaseApp;
import org.springframework.beans.factory.annotation.Value;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.FileInputStream;
import java.io.InputStream;

import javax.annotation.PostConstruct;

@Configuration

public class FirebaseConfig {

    @Value("${firebase.credentials}")
    private String firebaseCredentialsPath;

    @PostConstruct
    public void init() throws Exception {

        // InputStream serviceAccount = new
        // ClassPathResource("firebase-service-account.json").getInputStream();
        InputStream serviceAccount = new FileInputStream(firebaseCredentialsPath);
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }
}

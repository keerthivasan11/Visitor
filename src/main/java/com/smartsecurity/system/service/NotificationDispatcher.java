package com.smartsecurity.system.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationService notificationService;

    @Async
    public void sendAsync(String token, String title, String body) {
        try {
            notificationService.sendToToken(token, title, body);
        } catch (Exception e) {
            System.out.println("Notification failed: " + e.getMessage());
        }
    }
}

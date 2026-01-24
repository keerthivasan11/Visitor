package com.smartsecurity.system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.smartsecurity.system.dto.NotificationRequest;
import com.smartsecurity.system.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor

public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<String> send(@RequestBody NotificationRequest request)
            throws FirebaseMessagingException {

        String response = notificationService.sendToToken(
                request.getToken(),
                request.getTitle(),
                request.getBody());

        return ResponseEntity.ok(response);
    }
}

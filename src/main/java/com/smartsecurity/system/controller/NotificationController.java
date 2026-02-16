package com.smartsecurity.system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.smartsecurity.system.dto.NotificationRequest;
import com.smartsecurity.system.entity.User;
import com.smartsecurity.system.repository.UserRepository;
import com.smartsecurity.system.service.NotificationDispatcher;


import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor

public class NotificationController {

        private final NotificationDispatcher notificationDispatcher;
        private final UserRepository userRepository;

        @PostMapping("/send")
        public ResponseEntity<String> send(
                        @RequestBody NotificationRequest request) {
                User user = userRepository.findById(request.getUserId())
                                .orElseThrow(() -> new RuntimeException("User not found"));
                if (user.getFcmToken() == null) {
                        return ResponseEntity.badRequest().body("User has no FCM token");
                }
                notificationDispatcher.sendAsync(
                                user.getFcmToken(),
                                request.getTitle(),
                                request.getBody());
                return ResponseEntity.ok("Notification sent");
        }

}

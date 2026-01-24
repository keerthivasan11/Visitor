package com.smartsecurity.system.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;

import org.springframework.stereotype.Service;
import com.google.firebase.messaging.Message;

import com.google.firebase.messaging.Notification;

@Service

public class NotificationService {

    public String sendToToken(String token, String title, String body)
            throws FirebaseMessagingException {

        Message message = Message.builder()
                .setToken(token)
                .setNotification(
                        Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                .putData("click_action", "FLUTTER_NOTIFICATION_CLICK")
                .build();

        return FirebaseMessaging.getInstance().send(message);
    }
}

package com.example.EcoGo.service.chatbot;

import com.example.EcoGo.model.ChatNotification;
import com.example.EcoGo.repository.ChatNotificationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChatNotificationService {

    private final ChatNotificationRepository notificationRepository;

    public ChatNotificationService(ChatNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public String createNotification(String userId, String type, String title, String body) {
        String notificationId = "nt_" + UUID.randomUUID().toString().substring(0, 11);
        ChatNotification notification = new ChatNotification();
        notification.setNotificationId(notificationId);
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);
        return notificationId;
    }

    public List<ChatNotification> listNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}

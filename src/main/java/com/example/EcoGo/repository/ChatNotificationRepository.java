package com.example.EcoGo.repository;

import com.example.EcoGo.model.ChatNotification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatNotificationRepository extends MongoRepository<ChatNotification, String> {

    List<ChatNotification> findByUserIdOrderByCreatedAtDesc(String userId);

    List<ChatNotification> findByUserIdAndReadFalseOrderByCreatedAtDesc(String userId);
}

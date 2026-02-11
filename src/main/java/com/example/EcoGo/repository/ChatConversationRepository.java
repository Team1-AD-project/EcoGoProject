package com.example.EcoGo.repository;

import com.example.EcoGo.model.ChatConversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ChatConversationRepository extends MongoRepository<ChatConversation, String> {

    Optional<ChatConversation> findByConversationId(String conversationId);

    List<ChatConversation> findByUserIdOrderByUpdatedAtDesc(String userId);
}

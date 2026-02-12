package com.example.EcoGo.repository;

import com.example.EcoGo.model.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    List<AuditLog> findByActorUserIdOrderByCreatedAtDesc(String actorUserId);

    List<AuditLog> findAllByOrderByCreatedAtDesc();
}

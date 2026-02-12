package com.example.EcoGo.service.chatbot;

import com.example.EcoGo.model.AuditLog;
import com.example.EcoGo.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public String createAuditLog(String actorUserId, String action,
                                  String targetUserId, Map<String, Object> details) {
        String auditId = "au_" + UUID.randomUUID().toString().substring(0, 11);
        AuditLog log = new AuditLog();
        log.setAuditId(auditId);
        log.setActorUserId(actorUserId);
        log.setAction(action);
        log.setTargetUserId(targetUserId);
        log.setDetails(details != null ? details : Map.of());
        log.setCreatedAt(Instant.now());
        auditLogRepository.save(log);
        return auditId;
    }
}

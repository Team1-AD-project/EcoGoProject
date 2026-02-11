package com.example.EcoGo.service.chatbot;

import com.example.EcoGo.model.AuditLog;
import com.example.EcoGo.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        lenient().when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- createAuditLog ----------
    @Test
    void createAuditLog_shouldSaveAndReturnId() {
        Map<String, Object> details = Map.of("nickname", "NewName");

        String auditId = auditLogService.createAuditLog("admin_001", "update_user", "u_002", details);

        assertNotNull(auditId);
        assertTrue(auditId.startsWith("au_"));

        verify(auditLogRepository).save(argThat(log ->
                log.getActorUserId().equals("admin_001") &&
                log.getAction().equals("update_user") &&
                log.getTargetUserId().equals("u_002") &&
                log.getDetails().containsKey("nickname") &&
                log.getCreatedAt() != null
        ));
    }

    @Test
    void createAuditLog_nullDetails_shouldSaveEmptyMap() {
        String auditId = auditLogService.createAuditLog("admin_001", "delete_user", "u_003", null);

        assertNotNull(auditId);

        verify(auditLogRepository).save(argThat(log ->
                log.getDetails() != null && log.getDetails().isEmpty()
        ));
    }

    @Test
    void createAuditLog_shouldGenerateUniqueIds() {
        String id1 = auditLogService.createAuditLog("a1", "action1", "t1", Map.of());
        String id2 = auditLogService.createAuditLog("a1", "action2", "t2", Map.of());

        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2);
    }
}

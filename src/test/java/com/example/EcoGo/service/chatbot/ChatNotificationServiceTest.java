package com.example.EcoGo.service.chatbot;

import com.example.EcoGo.model.ChatNotification;
import com.example.EcoGo.repository.ChatNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatNotificationServiceTest {

    @Mock private ChatNotificationRepository notificationRepository;

    @InjectMocks private ChatNotificationService chatNotificationService;

    @BeforeEach
    void setUp() {
        lenient().when(notificationRepository.save(any(ChatNotification.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- createNotification ----------
    @Test
    void createNotification_shouldSaveAndReturnId() {
        String notifId = chatNotificationService.createNotification(
                "u_001", "profile_updated", "Profile Updated", "Your profile has been updated by admin.");

        assertNotNull(notifId);
        assertTrue(notifId.startsWith("nt_"));

        verify(notificationRepository).save(argThat(n ->
                n.getUserId().equals("u_001") &&
                n.getType().equals("profile_updated") &&
                n.getTitle().equals("Profile Updated") &&
                n.getBody().equals("Your profile has been updated by admin.") &&
                !n.isRead() &&
                n.getCreatedAt() != null
        ));
    }

    @Test
    void createNotification_shouldGenerateUniqueIds() {
        String id1 = chatNotificationService.createNotification("u_001", "type1", "Title1", "Body1");
        String id2 = chatNotificationService.createNotification("u_001", "type2", "Title2", "Body2");

        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2);
    }

    // ---------- listNotifications ----------
    @Test
    void listNotifications_shouldDelegateToRepository() {
        ChatNotification n1 = new ChatNotification();
        n1.setNotificationId("nt_1");
        n1.setUserId("u_001");
        n1.setType("profile_updated");
        n1.setTitle("Title1");
        n1.setCreatedAt(Instant.now());

        ChatNotification n2 = new ChatNotification();
        n2.setNotificationId("nt_2");
        n2.setUserId("u_001");
        n2.setType("booking");
        n2.setTitle("Title2");
        n2.setCreatedAt(Instant.now());

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("u_001")).thenReturn(List.of(n1, n2));

        List<ChatNotification> result = chatNotificationService.listNotifications("u_001");

        assertEquals(2, result.size());
        assertEquals("nt_1", result.get(0).getNotificationId());
        assertEquals("nt_2", result.get(1).getNotificationId());
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc("u_001");
    }

    @Test
    void listNotifications_noNotifications_shouldReturnEmptyList() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("u_999")).thenReturn(List.of());

        List<ChatNotification> result = chatNotificationService.listNotifications("u_999");

        assertTrue(result.isEmpty());
    }
}

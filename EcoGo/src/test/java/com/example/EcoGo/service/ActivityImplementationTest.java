package com.example.EcoGo.service;

import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.model.Activity;
import com.example.EcoGo.repository.ActivityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityImplementationTest {

    @Mock private ActivityRepository activityRepository;

    @InjectMocks private ActivityImplementation activityService;

    // ---------- helper ----------
    private static Activity buildActivity(String id, String title, String type, String status,
                                          Integer maxParticipants, Integer currentParticipants) {
        Activity a = new Activity();
        a.setId(id);
        a.setTitle(title);
        a.setType(type);
        a.setStatus(status);
        a.setMaxParticipants(maxParticipants);
        a.setCurrentParticipants(currentParticipants);
        a.setRewardCredits(50);
        a.setParticipantIds(new ArrayList<>());
        a.setStartTime(LocalDateTime.now());
        a.setEndTime(LocalDateTime.now().plusDays(7));
        return a;
    }

    // ---------- getAllActivities ----------
    @Test
    void getAllActivities_success() {
        Activity a1 = buildActivity("a1", "Beach Cleanup", "OFFLINE", "PUBLISHED", 50, 10);
        Activity a2 = buildActivity("a2", "Tree Planting", "OFFLINE", "ONGOING", 100, 30);
        when(activityRepository.findAll()).thenReturn(List.of(a1, a2));

        List<Activity> result = activityService.getAllActivities();

        assertEquals(2, result.size());
        verify(activityRepository).findAll();
    }

    @Test
    void getAllActivities_empty() {
        when(activityRepository.findAll()).thenReturn(List.of());

        List<Activity> result = activityService.getAllActivities();

        assertTrue(result.isEmpty());
    }

    // ---------- getActivityById ----------
    @Test
    void getActivityById_success() {
        Activity a = buildActivity("a1", "Beach Cleanup", "OFFLINE", "PUBLISHED", 50, 10);
        when(activityRepository.findById("a1")).thenReturn(Optional.of(a));

        Activity result = activityService.getActivityById("a1");

        assertEquals("a1", result.getId());
        assertEquals("Beach Cleanup", result.getTitle());
    }

    @Test
    void getActivityById_notFound() {
        when(activityRepository.findById("x")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.getActivityById("x"));
        assertEquals(ErrorCode.ACTIVITY_NOT_FOUND.getCode(), ex.getCode());
    }

    // ---------- createActivity ----------
    @Test
    void createActivity_success_setsDefaults() {
        Activity input = new Activity();
        input.setTitle("New Activity");
        input.setType("ONLINE");

        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> {
            Activity a = inv.getArgument(0);
            a.setId("a3");
            return a;
        });

        Activity result = activityService.createActivity(input);

        assertNotNull(result);
        assertEquals("a3", result.getId());
        assertEquals("DRAFT", result.getStatus());
        assertEquals(0, result.getCurrentParticipants());
        assertNotNull(result.getParticipantIds());
        assertTrue(result.getParticipantIds().isEmpty());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    void createActivity_preservesExistingStatus() {
        Activity input = new Activity();
        input.setTitle("Published Activity");
        input.setStatus("PUBLISHED");
        input.setCurrentParticipants(5);
        input.setParticipantIds(List.of("u1", "u2"));

        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        Activity result = activityService.createActivity(input);

        assertEquals("PUBLISHED", result.getStatus());
        assertEquals(5, result.getCurrentParticipants());
        assertEquals(2, result.getParticipantIds().size());
    }

    // ---------- updateActivity ----------
    @Test
    void updateActivity_success() {
        Activity existing = buildActivity("a1", "Old Title", "OFFLINE", "DRAFT", 50, 0);
        when(activityRepository.findById("a1")).thenReturn(Optional.of(existing));
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        Activity update = new Activity();
        update.setTitle("New Title");
        update.setStatus("PUBLISHED");
        update.setMaxParticipants(100);

        Activity result = activityService.updateActivity("a1", update);

        assertEquals("New Title", result.getTitle());
        assertEquals("PUBLISHED", result.getStatus());
        assertEquals(100, result.getMaxParticipants());
        // Unchanged fields
        assertEquals("OFFLINE", result.getType());
        assertEquals(50, result.getRewardCredits());
    }

    @Test
    void updateActivity_partialUpdate_nullFieldsNotOverwritten() {
        Activity existing = buildActivity("a1", "Beach Cleanup", "OFFLINE", "PUBLISHED", 50, 10);
        existing.setDescription("A great activity");
        existing.setLocationName("Beach");
        when(activityRepository.findById("a1")).thenReturn(Optional.of(existing));
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        Activity update = new Activity();
        update.setTitle("Updated Title");
        // Other fields are null → should not overwrite

        Activity result = activityService.updateActivity("a1", update);

        assertEquals("Updated Title", result.getTitle());
        assertEquals("A great activity", result.getDescription());
        assertEquals("Beach", result.getLocationName());
        assertEquals("OFFLINE", result.getType());
    }

    @Test
    void updateActivity_notFound() {
        when(activityRepository.findById("x")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.updateActivity("x", new Activity()));
        assertEquals(ErrorCode.ACTIVITY_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void updateActivity_locationFields() {
        Activity existing = buildActivity("a1", "Activity", "OFFLINE", "DRAFT", 50, 0);
        when(activityRepository.findById("a1")).thenReturn(Optional.of(existing));
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        Activity update = new Activity();
        update.setLatitude(1.23);
        update.setLongitude(4.56);
        update.setLocationName("Central Park");

        Activity result = activityService.updateActivity("a1", update);

        assertEquals(1.23, result.getLatitude());
        assertEquals(4.56, result.getLongitude());
        assertEquals("Central Park", result.getLocationName());
    }

    // ---------- deleteActivity ----------
    @Test
    void deleteActivity_success() {
        when(activityRepository.existsById("a1")).thenReturn(true);

        activityService.deleteActivity("a1");

        verify(activityRepository).deleteById("a1");
    }

    @Test
    void deleteActivity_notFound() {
        when(activityRepository.existsById("x")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.deleteActivity("x"));
        assertEquals(ErrorCode.ACTIVITY_NOT_FOUND.getCode(), ex.getCode());
    }

    // ---------- getActivitiesByStatus ----------
    @Test
    void getActivitiesByStatus_success() {
        Activity a = buildActivity("a1", "Beach Cleanup", "OFFLINE", "PUBLISHED", 50, 10);
        when(activityRepository.findByStatus("PUBLISHED")).thenReturn(List.of(a));

        List<Activity> result = activityService.getActivitiesByStatus("PUBLISHED");

        assertEquals(1, result.size());
        assertEquals("PUBLISHED", result.get(0).getStatus());
    }

    @Test
    void getActivitiesByStatus_empty() {
        when(activityRepository.findByStatus("ENDED")).thenReturn(List.of());

        List<Activity> result = activityService.getActivitiesByStatus("ENDED");

        assertTrue(result.isEmpty());
    }

    // ---------- publishActivity ----------
    @Test
    void publishActivity_success() {
        Activity existing = buildActivity("a1", "Beach Cleanup", "OFFLINE", "DRAFT", 50, 0);
        when(activityRepository.findById("a1")).thenReturn(Optional.of(existing));
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        Activity result = activityService.publishActivity("a1");

        assertEquals("PUBLISHED", result.getStatus());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    void publishActivity_notFound() {
        when(activityRepository.findById("x")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.publishActivity("x"));
        assertEquals(ErrorCode.ACTIVITY_NOT_FOUND.getCode(), ex.getCode());
    }

    // ---------- joinActivity ----------
    @Test
    void joinActivity_success() {
        Activity a = buildActivity("a1", "Beach Cleanup", "OFFLINE", "PUBLISHED", 50, 10);
        when(activityRepository.findById("a1")).thenReturn(Optional.of(a));
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        Activity result = activityService.joinActivity("a1", "user001");

        assertEquals(11, result.getCurrentParticipants());
        assertTrue(result.getParticipantIds().contains("user001"));
    }

    @Test
    void joinActivity_activityFull() {
        Activity a = buildActivity("a1", "Beach Cleanup", "OFFLINE", "PUBLISHED", 10, 10);
        when(activityRepository.findById("a1")).thenReturn(Optional.of(a));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.joinActivity("a1", "user001"));
        assertEquals(ErrorCode.ACTIVITY_FULL.getCode(), ex.getCode());
    }

    @Test
    void joinActivity_alreadyJoined() {
        Activity a = buildActivity("a1", "Beach Cleanup", "OFFLINE", "PUBLISHED", 50, 10);
        a.getParticipantIds().add("user001");
        when(activityRepository.findById("a1")).thenReturn(Optional.of(a));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.joinActivity("a1", "user001"));
        assertEquals(ErrorCode.ALREADY_JOINED.getCode(), ex.getCode());
    }

    @Test
    void joinActivity_notFound() {
        when(activityRepository.findById("x")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.joinActivity("x", "user001"));
        assertEquals(ErrorCode.ACTIVITY_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void joinActivity_nullParticipantIds_initializesList() {
        Activity a = buildActivity("a1", "Beach Cleanup", "OFFLINE", "PUBLISHED", 50, 0);
        a.setParticipantIds(null);
        when(activityRepository.findById("a1")).thenReturn(Optional.of(a));
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        Activity result = activityService.joinActivity("a1", "user001");

        assertNotNull(result.getParticipantIds());
        assertTrue(result.getParticipantIds().contains("user001"));
        assertEquals(1, result.getCurrentParticipants());
    }

    @Test
    void joinActivity_noMaxLimit() {
        Activity a = buildActivity("a1", "Beach Cleanup", "OFFLINE", "PUBLISHED", null, 100);
        when(activityRepository.findById("a1")).thenReturn(Optional.of(a));
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        Activity result = activityService.joinActivity("a1", "user001");

        assertEquals(101, result.getCurrentParticipants());
    }

    // ---------- leaveActivity ----------
    @Test
    void leaveActivity_success() {
        Activity a = buildActivity("a1", "Beach Cleanup", "OFFLINE", "PUBLISHED", 50, 10);
        a.getParticipantIds().add("user001");
        when(activityRepository.findById("a1")).thenReturn(Optional.of(a));
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        Activity result = activityService.leaveActivity("a1", "user001");

        assertEquals(9, result.getCurrentParticipants());
        assertFalse(result.getParticipantIds().contains("user001"));
    }

    @Test
    void leaveActivity_userNotInList_noChange() {
        Activity a = buildActivity("a1", "Beach Cleanup", "OFFLINE", "PUBLISHED", 50, 10);
        when(activityRepository.findById("a1")).thenReturn(Optional.of(a));
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        Activity result = activityService.leaveActivity("a1", "user999");

        // Should still save but no change to participants
        assertEquals(10, result.getCurrentParticipants());
        verify(activityRepository).save(a);
    }

    @Test
    void leaveActivity_nullParticipantIds_noChange() {
        Activity a = buildActivity("a1", "Beach Cleanup", "OFFLINE", "PUBLISHED", 50, 10);
        a.setParticipantIds(null);
        when(activityRepository.findById("a1")).thenReturn(Optional.of(a));
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

        Activity result = activityService.leaveActivity("a1", "user001");

        assertEquals(10, result.getCurrentParticipants());
        verify(activityRepository).save(a);
    }

    @Test
    void leaveActivity_notFound() {
        when(activityRepository.findById("x")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> activityService.leaveActivity("x", "user001"));
        assertEquals(ErrorCode.ACTIVITY_NOT_FOUND.getCode(), ex.getCode());
    }
}

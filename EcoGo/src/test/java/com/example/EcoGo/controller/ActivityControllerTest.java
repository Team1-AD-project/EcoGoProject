package com.example.EcoGo.controller;

import com.example.EcoGo.dto.ActivityRequestDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.interfacemethods.ActivityInterface;
import com.example.EcoGo.model.Activity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ActivityControllerTest {

    private ActivityInterface activityService;
    private ActivityController controller;

    @BeforeEach
    void setUp() throws Exception {
        activityService = mock(ActivityInterface.class);
        controller = new ActivityController();

        Field f = ActivityController.class.getDeclaredField("activityService");
        f.setAccessible(true);
        f.set(controller, activityService);
    }

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

    // ========== Web Endpoints ==========

    // ---------- getAllWebActivities ----------
    @Test
    void getAllWebActivities_success() {
        Activity a1 = buildActivity("a1", "Beach Cleanup", "OFFLINE", "PUBLISHED", 50, 10);
        Activity a2 = buildActivity("a2", "Tree Planting", "OFFLINE", "ONGOING", 100, 30);
        when(activityService.getAllActivities()).thenReturn(List.of(a1, a2));

        ResponseMessage<List<Activity>> resp = controller.getAllWebActivities();

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(2, resp.getData().size());
        verify(activityService).getAllActivities();
    }

    @Test
    void getAllWebActivities_emptyList() {
        when(activityService.getAllActivities()).thenReturn(List.of());

        ResponseMessage<List<Activity>> resp = controller.getAllWebActivities();

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertTrue(resp.getData().isEmpty());
    }

    // ---------- getWebActivityById ----------
    @Test
    void getWebActivityById_success() {
        Activity a = buildActivity("a1", "Beach Cleanup", "OFFLINE", "PUBLISHED", 50, 10);
        when(activityService.getActivityById("a1")).thenReturn(a);

        ResponseMessage<Activity> resp = controller.getWebActivityById("a1");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals("a1", resp.getData().getId());
        assertEquals("Beach Cleanup", resp.getData().getTitle());
    }

    // ---------- createWebActivity ----------
    @Test
    void createWebActivity_success() {
        ActivityRequestDto dto = new ActivityRequestDto();
        dto.setTitle("New Activity");
        dto.setType("ONLINE");
        dto.setMaxParticipants(200);
        Activity created = buildActivity("a3", "New Activity", "ONLINE", "DRAFT", 200, 0);
        when(activityService.createActivity(any(Activity.class))).thenReturn(created);

        ResponseMessage<Activity> resp = controller.createWebActivity(dto);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals("a3", resp.getData().getId());
        verify(activityService).createActivity(any(Activity.class));
    }

    // ---------- updateWebActivity ----------
    @Test
    void updateWebActivity_success() {
        ActivityRequestDto dto = new ActivityRequestDto();
        dto.setTitle("Updated Title");
        Activity updated = buildActivity("a1", "Updated Title", "OFFLINE", "PUBLISHED", 50, 10);
        when(activityService.updateActivity(eq("a1"), any(Activity.class))).thenReturn(updated);

        ResponseMessage<Activity> resp = controller.updateWebActivity("a1", dto);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals("Updated Title", resp.getData().getTitle());
    }

    // ---------- deleteWebActivity ----------
    @Test
    void deleteWebActivity_success() {
        doNothing().when(activityService).deleteActivity("a1");

        ResponseMessage<Void> resp = controller.deleteWebActivity("a1");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        verify(activityService).deleteActivity("a1");
    }

    // ---------- getWebActivitiesByStatus ----------
    @Test
    void getWebActivitiesByStatus_success() {
        Activity a = buildActivity("a1", "Beach Cleanup", "OFFLINE", "PUBLISHED", 50, 10);
        when(activityService.getActivitiesByStatus("PUBLISHED")).thenReturn(List.of(a));

        ResponseMessage<List<Activity>> resp = controller.getWebActivitiesByStatus("PUBLISHED");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(1, resp.getData().size());
    }

    @Test
    void getWebActivitiesByStatus_empty() {
        when(activityService.getActivitiesByStatus("ENDED")).thenReturn(List.of());

        ResponseMessage<List<Activity>> resp = controller.getWebActivitiesByStatus("ENDED");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertTrue(resp.getData().isEmpty());
    }

    // ---------- publishWebActivity ----------
    @Test
    void publishWebActivity_success() {
        Activity published = buildActivity("a1", "Beach Cleanup", "OFFLINE", "PUBLISHED", 50, 0);
        when(activityService.publishActivity("a1")).thenReturn(published);

        ResponseMessage<Activity> resp = controller.publishWebActivity("a1");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals("PUBLISHED", resp.getData().getStatus());
    }

    // ========== Mobile Endpoints ==========

    // ---------- getAllMobileActivities ----------
    @Test
    void getAllMobileActivities_success() {
        Activity a = buildActivity("a1", "Beach Cleanup", "OFFLINE", "PUBLISHED", 50, 10);
        when(activityService.getAllActivities()).thenReturn(List.of(a));

        ResponseMessage<List<Activity>> resp = controller.getAllMobileActivities();

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(1, resp.getData().size());
    }

    // ---------- getMobileActivityById ----------
    @Test
    void getMobileActivityById_success() {
        Activity a = buildActivity("a1", "Beach Cleanup", "OFFLINE", "PUBLISHED", 50, 10);
        when(activityService.getActivityById("a1")).thenReturn(a);

        ResponseMessage<Activity> resp = controller.getMobileActivityById("a1");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals("a1", resp.getData().getId());
    }

    // ---------- joinMobileActivity ----------
    @Test
    void joinMobileActivity_success() {
        Activity joined = buildActivity("a1", "Beach Cleanup", "OFFLINE", "PUBLISHED", 50, 11);
        joined.getParticipantIds().add("user001");
        when(activityService.joinActivity("a1", "user001")).thenReturn(joined);

        ResponseMessage<Activity> resp = controller.joinMobileActivity("a1", "user001");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(11, resp.getData().getCurrentParticipants());
        assertTrue(resp.getData().getParticipantIds().contains("user001"));
    }

    // ---------- leaveMobileActivity ----------
    @Test
    void leaveMobileActivity_success() {
        Activity left = buildActivity("a1", "Beach Cleanup", "OFFLINE", "PUBLISHED", 50, 9);
        when(activityService.leaveActivity("a1", "user001")).thenReturn(left);

        ResponseMessage<Activity> resp = controller.leaveMobileActivity("a1", "user001");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(9, resp.getData().getCurrentParticipants());
    }
}

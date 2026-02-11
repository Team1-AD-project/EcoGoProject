package com.example.EcoGo.controller;

import com.example.EcoGo.dto.ActivityRequestDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.interfacemethods.ActivityInterface;
import com.example.EcoGo.model.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1") // Base path is /api/v1
public class ActivityController {
    private static final Logger logger = LoggerFactory.getLogger(ActivityController.class);

    @Autowired
    private ActivityInterface activityService;

    // === Web Endpoints ===

    @GetMapping("/web/activities")
    public ResponseMessage<List<Activity>> getAllWebActivities() {
        logger.info("[WEB] Fetching all activities");
        return ResponseMessage.success(activityService.getAllActivities());
    }

    @GetMapping("/web/activities/{id}")
    public ResponseMessage<Activity> getWebActivityById(@PathVariable String id) {
        logger.info("[WEB] Fetching activity by ID: {}", id);
        return ResponseMessage.success(activityService.getActivityById(id));
    }

    @PostMapping("/web/activities")
    public ResponseMessage<Activity> createWebActivity(@RequestBody ActivityRequestDto dto) {
        logger.info("[WEB] Creating new activity: {}", dto.getTitle());
        return ResponseMessage.success(activityService.createActivity(dto.toEntity()));
    }

    @PutMapping("/web/activities/{id}")
    public ResponseMessage<Activity> updateWebActivity(@PathVariable String id, @RequestBody ActivityRequestDto dto) {
        logger.info("[WEB] Updating activity: {}", id);
        return ResponseMessage.success(activityService.updateActivity(id, dto.toEntity()));
    }

    @DeleteMapping("/web/activities/{id}")
    public ResponseMessage<Void> deleteWebActivity(@PathVariable String id) {
        logger.info("[WEB] Deleting activity: {}", id);
        activityService.deleteActivity(id);
        return ResponseMessage.success(null);
    }

    @GetMapping("/web/activities/status/{status}")
    public ResponseMessage<List<Activity>> getWebActivitiesByStatus(@PathVariable String status) {
        logger.info("[WEB] Fetching activities by status: {}", status);
        return ResponseMessage.success(activityService.getActivitiesByStatus(status));
    }

    @PostMapping("/web/activities/{id}/publish")
    public ResponseMessage<Activity> publishWebActivity(@PathVariable String id) {
        logger.info("[WEB] Publishing activity: {}", id);
        return ResponseMessage.success(activityService.publishActivity(id));
    }

    // === Mobile Endpoints ===

    @GetMapping("/mobile/activities")
    public ResponseMessage<List<Activity>> getAllMobileActivities() {
        logger.info("[Mobile] Fetching all activities");
        return ResponseMessage.success(activityService.getAllActivities());
    }

    @GetMapping("/mobile/activities/{id}")
    public ResponseMessage<Activity> getMobileActivityById(@PathVariable String id) {
        logger.info("[Mobile] Fetching activity by ID: {}", id);
        return ResponseMessage.success(activityService.getActivityById(id));
    }

    @PostMapping("/mobile/activities/{id}/join")
    public ResponseMessage<Activity> joinMobileActivity(@PathVariable String id, @RequestParam String userId) {
        logger.info("[Mobile] User {} joining activity {}", userId, id);
        return ResponseMessage.success(activityService.joinActivity(id, userId));
    }

    @PostMapping("/mobile/activities/{id}/leave")
    public ResponseMessage<Activity> leaveMobileActivity(@PathVariable String id, @RequestParam String userId) {
        logger.info("[Mobile] User {} leaving activity {}", userId, id);
        return ResponseMessage.success(activityService.leaveActivity(id, userId));
    }
}

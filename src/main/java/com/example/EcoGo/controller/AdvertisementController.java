package com.example.EcoGo.controller;

import com.example.EcoGo.dto.AdvertisementRequestDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.interfacemethods.AdvertisementInterface;
import com.example.EcoGo.model.Advertisement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/v1") // Base path is /api/v1
public class AdvertisementController {
    private static final Logger logger = LoggerFactory.getLogger(AdvertisementController.class);

    @Autowired
    private AdvertisementInterface advertisementService;

    // === Web Endpoints ===

    @GetMapping("/web/advertisements")
    public ResponseMessage<Page<Advertisement>> getAllWebAdvertisements(@RequestParam(defaultValue = "") String name, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        logger.info("[WEB] Fetching all advertisements");
        Pageable pageable = PageRequest.of(page, size);
        return ResponseMessage.success(advertisementService.getAllAdvertisements(name, pageable));
    }

    @GetMapping("/web/advertisements/{id}")
    public ResponseMessage<Advertisement> getWebAdvertisementById(@PathVariable String id) {
        logger.info("[WEB] Fetching advertisement by ID: {}", id);
        return ResponseMessage.success(advertisementService.getAdvertisementById(id));
    }

    @PostMapping("/web/advertisements")
    public ResponseMessage<Advertisement> createWebAdvertisement(@RequestBody AdvertisementRequestDto dto) {
        logger.info("[WEB] Creating new advertisement: {}", dto.getName());
        return ResponseMessage.success(advertisementService.createAdvertisement(dto.toEntity()));
    }

    @PutMapping("/web/advertisements/{id}")
    public ResponseMessage<Advertisement> updateWebAdvertisement(@PathVariable String id, @RequestBody AdvertisementRequestDto dto) {
        logger.info("[WEB] Updating advertisement: {}", id);
        return ResponseMessage.success(advertisementService.updateAdvertisement(id, dto.toEntity()));
    }

    @DeleteMapping("/web/advertisements/{id}")
    public ResponseMessage<Void> deleteWebAdvertisement(@PathVariable String id) {
        logger.info("[WEB] Deleting advertisement: {}", id);
        advertisementService.deleteAdvertisement(id);
        return ResponseMessage.success(null);
    }

    @PatchMapping("/web/advertisements/{id}/status")
    public ResponseMessage<Advertisement> updateWebAdvertisementStatus(@PathVariable String id, @RequestParam String status) {
        logger.info("[WEB] Updating advertisement status: {}, new status: {}", id, status);
        return ResponseMessage.success(advertisementService.updateAdvertisementStatus(id, status));
    }

    // === Public Endpoint (no auth required) ===

    @GetMapping("/advertisements/active")
    public ResponseMessage<List<Advertisement>> getActiveAdvertisements() {
        logger.info("Fetching active advertisements (public)");
        return ResponseMessage.success(advertisementService.getActiveAdvertisements());
    }
}

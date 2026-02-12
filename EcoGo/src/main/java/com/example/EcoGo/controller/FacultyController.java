package com.example.EcoGo.controller;

import com.example.EcoGo.dto.FacultyStatsDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.service.FacultyServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class FacultyController {

    @Autowired
    private FacultyServiceImpl facultyService;

    // --- List Faculties ---

    /**
     * Public: Get All Faculties
     * GET /api/v1/faculties
     */
    @GetMapping("/faculties")
    public ResponseMessage<List<String>> getAllFaculties() {
        return ResponseMessage.success(facultyService.getAllFacultyNames());
    }

    /**
     * Admin: Get All Faculties
     * GET /api/v1/admin/faculties
     */
    @GetMapping("/admin/faculties")
    public ResponseMessage<List<String>> getAllFacultiesAdmin() {
        return ResponseMessage.success(facultyService.getAllFacultyNames());
    }

    // --- Monthly Carbon Stats ---

    /**
     * Public: Get Monthly Carbon Stats per Faculty
     * GET /api/v1/faculties/stats/carbon/monthly
     */
    @GetMapping("/faculties/stats/carbon/monthly")
    public ResponseMessage<List<FacultyStatsDto.CarbonResponse>> getMonthlyFacultyCarbon() {
        return ResponseMessage.success(facultyService.getMonthlyFacultyCarbonStats());
    }

    /**
     * Admin: Get Monthly Carbon Stats per Faculty
     * GET /api/v1/admin/faculties/stats/carbon/monthly
     */
    @GetMapping("/admin/faculties/stats/carbon/monthly")
    public ResponseMessage<List<FacultyStatsDto.CarbonResponse>> getMonthlyFacultyCarbonAdmin() {
        return ResponseMessage.success(facultyService.getMonthlyFacultyCarbonStats());
    }

    /**
     * Web: Get Monthly Carbon Stats per Faculty (for web frontend with baseURL /api/v1/web)
     * GET /api/v1/web/faculties/stats/carbon/monthly
     */
    @GetMapping("/web/faculties/stats/carbon/monthly")
    public ResponseMessage<List<FacultyStatsDto.CarbonResponse>> getWebMonthlyFacultyCarbon() {
        return ResponseMessage.success(facultyService.getMonthlyFacultyCarbonStats());
    }

    // --- Mobile Endpoints ---

    /**
     * Mobile: Get All Faculties
     * GET /api/v1/mobile/faculties
     */
    @GetMapping("/mobile/faculties")
    public ResponseMessage<List<String>> getAllFacultiesMobile() {
        return ResponseMessage.success(facultyService.getAllFacultyNames());
    }

    /**
     * Mobile: Get Monthly Carbon Stats per Faculty
     * GET /api/v1/mobile/faculties/stats/carbon/monthly
     */
    @GetMapping("/mobile/faculties/stats/carbon/monthly")
    public ResponseMessage<List<FacultyStatsDto.CarbonResponse>> getMobileMonthlyFacultyCarbon() {
        return ResponseMessage.success(facultyService.getMonthlyFacultyCarbonStats());
    }
}

package com.example.EcoGo.controller;

import com.example.EcoGo.dto.FacultyStatsDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.service.FacultyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacultyControllerTest {

    @Mock
    private FacultyServiceImpl facultyService;

    @InjectMocks
    private FacultyController facultyController;

    private List<String> mockFacultyNames;
    private List<FacultyStatsDto.CarbonResponse> mockCarbonStats;

    @BeforeEach
    void setUp() {
        mockFacultyNames = Arrays.asList("Engineering", "Science");
        mockCarbonStats = Arrays.asList(
                new FacultyStatsDto.CarbonResponse("Science", 100.0),
                new FacultyStatsDto.CarbonResponse("Engineering", 50.0));
    }

    @Test
    void getAllFaculties_success() {
        when(facultyService.getAllFacultyNames()).thenReturn(mockFacultyNames);

        ResponseMessage<List<String>> response = facultyController.getAllFaculties();

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertEquals(2, response.getData().size());
        verify(facultyService).getAllFacultyNames();
    }

    @Test
    void getAllFacultiesAdmin_success() {
        when(facultyService.getAllFacultyNames()).thenReturn(mockFacultyNames);

        ResponseMessage<List<String>> response = facultyController.getAllFacultiesAdmin();

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertEquals(2, response.getData().size());
        verify(facultyService).getAllFacultyNames();
    }

    @Test
    void getAllFacultiesMobile_success() {
        when(facultyService.getAllFacultyNames()).thenReturn(mockFacultyNames);

        ResponseMessage<List<String>> response = facultyController.getAllFacultiesMobile();

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertEquals(2, response.getData().size());
        verify(facultyService).getAllFacultyNames();
    }

    @Test
    void getMonthlyFacultyCarbon_success() {
        when(facultyService.getMonthlyFacultyCarbonStats()).thenReturn(mockCarbonStats);

        ResponseMessage<List<FacultyStatsDto.CarbonResponse>> response = facultyController.getMonthlyFacultyCarbon();

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertEquals(2, response.getData().size());
        assertEquals("Science", response.getData().get(0).faculty);
        verify(facultyService).getMonthlyFacultyCarbonStats();
    }

    @Test
    void getMonthlyFacultyCarbonAdmin_success() {
        when(facultyService.getMonthlyFacultyCarbonStats()).thenReturn(mockCarbonStats);

        ResponseMessage<List<FacultyStatsDto.CarbonResponse>> response = facultyController
                .getMonthlyFacultyCarbonAdmin();

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertEquals(2, response.getData().size());
        verify(facultyService).getMonthlyFacultyCarbonStats();
    }

    @Test
    void getWebMonthlyFacultyCarbon_success() {
        when(facultyService.getMonthlyFacultyCarbonStats()).thenReturn(mockCarbonStats);

        ResponseMessage<List<FacultyStatsDto.CarbonResponse>> response = facultyController.getWebMonthlyFacultyCarbon();

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertEquals(2, response.getData().size());
        verify(facultyService).getMonthlyFacultyCarbonStats();
    }

    @Test
    void getMobileMonthlyFacultyCarbon_success() {
        when(facultyService.getMonthlyFacultyCarbonStats()).thenReturn(mockCarbonStats);

        ResponseMessage<List<FacultyStatsDto.CarbonResponse>> response = facultyController
                .getMobileMonthlyFacultyCarbon();

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertEquals(2, response.getData().size());
        verify(facultyService).getMonthlyFacultyCarbonStats();
    }
}

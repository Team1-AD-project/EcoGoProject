package com.example.EcoGo.service;

import com.example.EcoGo.dto.FacultyStatsDto;
import com.example.EcoGo.dto.LeaderboardEntry;
import com.example.EcoGo.model.Faculty;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.FacultyRepository;
import com.example.EcoGo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacultyServiceImplTest {

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private FacultyServiceImpl facultyService;

    @Test
    void getAllFacultyNames_success() {
        Faculty f1 = new Faculty();
        f1.setName("Engineering");
        Faculty f2 = new Faculty();
        f2.setName("Science");

        when(facultyRepository.findAll()).thenReturn(Arrays.asList(f1, f2));

        List<String> result = facultyService.getAllFacultyNames();

        assertEquals(2, result.size());
        assertTrue(result.contains("Engineering"));
        assertTrue(result.contains("Science"));
    }

    @Test
    void getMonthlyFacultyCarbonStats_success() {
        // Mock Aggregation Results
        LeaderboardEntry entry1 = new LeaderboardEntry();
        entry1.setUserId("user1");
        entry1.setTotalCarbonSaved(10.5);

        LeaderboardEntry entry2 = new LeaderboardEntry();
        entry2.setUserId("user2");
        entry2.setTotalCarbonSaved(20.0);

        AggregationResults<LeaderboardEntry> aggregationResults = mock(AggregationResults.class);
        when(aggregationResults.getMappedResults()).thenReturn(Arrays.asList(entry1, entry2));

        when(mongoTemplate.aggregate(any(Aggregation.class), eq("trips"), eq(LeaderboardEntry.class)))
                .thenReturn(aggregationResults);

        // Mock User Faculties
        User user1 = new User();
        user1.setUserid("user1");
        user1.setFaculty("Engineering");

        User user2 = new User();
        user2.setUserid("user2");
        user2.setFaculty("Science");

        when(userRepository.findByUseridIn(any())).thenReturn(Arrays.asList(user1, user2));

        // Mock Faculty Names (to ensure all families are present even if 0)
        // If getAllFacultyNames is called internally
        Faculty f1 = new Faculty();
        f1.setName("Engineering");
        Faculty f2 = new Faculty();
        f2.setName("Science");
        Faculty f3 = new Faculty();
        f3.setName("Arts");
        when(facultyRepository.findAll()).thenReturn(Arrays.asList(f1, f2, f3));

        List<FacultyStatsDto.CarbonResponse> result = facultyService.getMonthlyFacultyCarbonStats();

        // 3 Faculties total
        assertEquals(3, result.size());

        // Check Engineering: 10.5
        FacultyStatsDto.CarbonResponse engineering = result.stream()
                .filter(r -> r.faculty.equals("Engineering")).findFirst().orElse(null);
        assertNotNull(engineering);
        assertEquals(10.5, engineering.totalCarbon);

        // Check Science: 20.0
        FacultyStatsDto.CarbonResponse science = result.stream()
                .filter(r -> r.faculty.equals("Science")).findFirst().orElse(null);
        assertNotNull(science);
        assertEquals(20.0, science.totalCarbon);

        // Check Arts: 0.0
        FacultyStatsDto.CarbonResponse arts = result.stream()
                .filter(r -> r.faculty.equals("Arts")).findFirst().orElse(null);
        assertNotNull(arts);
        assertEquals(0.0, arts.totalCarbon);

        // Verify Sorting (Science 20.0 > Engineering 10.5 > Arts 0.0)
        assertEquals("Science", result.get(0).faculty);
        assertEquals("Engineering", result.get(1).faculty);
    }
}

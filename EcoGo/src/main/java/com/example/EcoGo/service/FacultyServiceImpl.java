package com.example.EcoGo.service;

import com.example.EcoGo.dto.FacultyStatsDto;
import com.example.EcoGo.dto.LeaderboardEntry;
import com.example.EcoGo.model.Faculty;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.FacultyRepository;
import com.example.EcoGo.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FacultyServiceImpl {

    private final FacultyRepository facultyRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    public FacultyServiceImpl(FacultyRepository facultyRepository,
            UserRepository userRepository, MongoTemplate mongoTemplate) {
        this.facultyRepository = facultyRepository;
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public List<String> getAllFacultyNames() {
        return facultyRepository.findAll().stream()
                .map(Faculty::getName)
                .collect(Collectors.toList());
    }

    public List<FacultyStatsDto.CarbonResponse> getMonthlyFacultyCarbonStats() {
        // 1. Determine current month range
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        LocalDateTime endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1).with(LocalTime.MIN);

        // 2. Use MongoTemplate aggregation to get per-user carbon totals
        //    (same approach as LeaderboardImplementation - proven to work)
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("carbon_status").is("completed")
                        .and("start_time").gte(startOfMonth).lt(endOfMonth)),
                Aggregation.group("user_id").sum("carbon_saved").as("totalCarbonSaved"),
                Aggregation.sort(Sort.Direction.DESC, "totalCarbonSaved")
        );

        AggregationResults<LeaderboardEntry> results = mongoTemplate.aggregate(
                aggregation, "trips", LeaderboardEntry.class);
        List<LeaderboardEntry> entries = results.getMappedResults();

        // 3. Get user IDs and fetch users to map userId -> faculty
        List<String> userIds = entries.stream()
                .map(LeaderboardEntry::getUserId)
                .collect(Collectors.toList());

        Map<String, String> userFacultyMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userRepository.findByUseridIn(userIds);
            for (User user : users) {
                if (user.getFaculty() != null && !user.getFaculty().isEmpty()) {
                    userFacultyMap.put(user.getUserid(), user.getFaculty());
                }
            }
        }

        // 4. Aggregate carbon by faculty
        Map<String, Double> facultyCarbonMap = new HashMap<>();

        // Initialize with faculties from DB collection
        for (String f : getAllFacultyNames()) {
            facultyCarbonMap.put(f, 0.0);
        }

        // Also initialize faculties from user records (in case faculties collection is empty)
        for (String faculty : userFacultyMap.values()) {
            facultyCarbonMap.putIfAbsent(faculty, 0.0);
        }

        // Add carbon from aggregation results
        for (LeaderboardEntry entry : entries) {
            String faculty = userFacultyMap.get(entry.getUserId());
            if (faculty != null) {
                double current = facultyCarbonMap.getOrDefault(faculty, 0.0);
                facultyCarbonMap.put(faculty, current + entry.getTotalCarbonSaved());
            }
        }

        // 5. Convert to DTO, sorted descending
        List<FacultyStatsDto.CarbonResponse> response = new ArrayList<>();
        for (Map.Entry<String, Double> entry : facultyCarbonMap.entrySet()) {
            double roundedValue = Math.round(entry.getValue() * 100.0) / 100.0;
            response.add(new FacultyStatsDto.CarbonResponse(entry.getKey(), roundedValue));
        }

        response.sort((a, b) -> Double.compare(b.totalCarbon, a.totalCarbon));

        return response;
    }
}

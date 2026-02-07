package com.example.EcoGo.service;

import com.example.EcoGo.dto.FacultyStatsDto;
import com.example.EcoGo.model.Faculty;
import com.example.EcoGo.model.Trip;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.FacultyRepository;
import com.example.EcoGo.repository.TripRepository;
import com.example.EcoGo.repository.UserRepository;
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
    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    public FacultyServiceImpl(FacultyRepository facultyRepository, TripRepository tripRepository,
            UserRepository userRepository) {
        this.facultyRepository = facultyRepository;
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
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
        LocalDateTime endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);

        System.out.println("DEBUG: Fetching stats from " + startOfMonth + " to " + endOfMonth);

        // 2. Fetch all COMPLETED trips in this range
        List<Trip> trips = tripRepository.findByStartTimeBetweenAndCarbonStatus(startOfMonth, endOfMonth, "completed");
        System.out.println("DEBUG: Found " + trips.size() + " completed trips.");

        // 3. Collect all user IDs
        List<String> userIds = trips.stream()
                .map(Trip::getUserId)
                .distinct()
                .collect(Collectors.toList());
        System.out.println("DEBUG: Found " + userIds.size() + " distinct users: " + userIds);

        // 4. Fetch all Users to get their Faculty
        // Direct lookup as requested to ensure accuracy
        List<User> users = userRepository.findByUseridIn(userIds);
        System.out.println("DEBUG: Fetched " + users.size() + " user records.");

        Map<String, String> userFacultyMap = users.stream()
                .filter(u -> u.getFaculty() != null)
                .collect(Collectors.toMap(User::getUserid, User::getFaculty));
        System.out.println("DEBUG: User Faculty Map: " + userFacultyMap);

        // 5. Aggregate Carbon by Faculty
        Map<String, Double> facultyCarbonMap = new HashMap<>();

        // Initialize with all faculties
        List<String> allFaculties = getAllFacultyNames();
        System.out.println("DEBUG: All Faculties in DB: " + allFaculties);

        for (String f : allFaculties) {
            facultyCarbonMap.put(f, 0.0);
        }

        for (Trip trip : trips) {
            // Find faculty for this trip's user
            String faculty = userFacultyMap.get(trip.getUserId());

            if (faculty == null) {
                System.out.println("DEBUG: Warning - No faculty found for user " + trip.getUserId() + " (Trip ID: "
                        + trip.getId() + ")");
            }

            if (faculty != null) {
                if (facultyCarbonMap.containsKey(faculty)) {
                    double current = facultyCarbonMap.get(faculty);
                    double saved = trip.getCarbonSaved();
                    facultyCarbonMap.put(faculty, current + saved);
                    if (saved > 0) {
                        System.out.println(
                                "DEBUG: Adding " + saved + " to " + faculty + " (Total: " + (current + saved) + ")");
                    } else {
                        System.out.println("DEBUG: Trip " + trip.getId() + " for user " + trip.getUserId()
                                + " has 0 carbon saved. Ignoring.");
                    }
                } else {
                    System.out.println("DEBUG: Warning - Faculty '" + faculty + "' from user " + trip.getUserId()
                            + " not in faculties list.");
                }
            }
        }

        // 6. Convert to DTO
        List<FacultyStatsDto.CarbonResponse> response = new ArrayList<>();
        for (Map.Entry<String, Double> entry : facultyCarbonMap.entrySet()) {
            response.add(new FacultyStatsDto.CarbonResponse(entry.getKey(), entry.getValue()));
        }

        return response;
    }
}

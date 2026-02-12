package com.example.EcoGo.repository;

import com.example.EcoGo.model.Trip;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TripRepository extends MongoRepository<Trip, String> {

    List<Trip> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Trip> findByUserIdAndCarbonStatus(String userId, String carbonStatus);

    List<Trip> findByUserId(String userId);

    List<Trip> findByUserIdAndIsGreenTrip(String userId, boolean isGreenTrip);

    // 查询用户在指定时间范围内的绿色出行（用于Challenge进度计算）
    List<Trip> findByUserIdAndIsGreenTripAndCarbonStatusAndStartTimeBetween(
            String userId,
            boolean isGreenTrip,
            String carbonStatus,
            LocalDateTime startTime,
            LocalDateTime endTime);

    // List trips within a time range (for stats aggregation)
    List<Trip> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    // List trips within a time range AND status
    List<Trip> findByStartTimeBetweenAndCarbonStatus(LocalDateTime start, LocalDateTime end, String carbonStatus);

    // Batch query for users' trips with status
    List<Trip> findByUserIdInAndCarbonStatus(List<String> userIds, String carbonStatus);
}

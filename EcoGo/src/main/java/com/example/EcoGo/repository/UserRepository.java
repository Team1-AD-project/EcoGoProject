package com.example.EcoGo.repository;

import com.example.EcoGo.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    Optional<User> findByUserid(String userid);

    org.springframework.data.domain.Page<User> findByIsAdminFalse(org.springframework.data.domain.Pageable pageable);

    Optional<User> findByPhoneOrUserid(String phone, String userid);

    // Find active VIPs whose expiry date is before the given time
    java.util.List<User> findByVipIsActiveTrueAndVipExpiryDateBefore(java.time.LocalDateTime time);

    java.util.List<User> findByFaculty(String faculty);

    // Find users by a list of user IDs
    java.util.List<User> findByUseridIn(java.util.List<String> userids);
    // Atomic update for totalCarbon to avoid overwriting other fields (like points)
    // db.users.updateOne({ userid: ? }, { $inc: { total_carbon: ? } })
    // Note: MongoRepository doesn't support @Modifying/@Query for $inc easily with
    // simple syntax in all versions,
    // but we can use a custom repository or just find-modify-save if we are
    // careful.
    // However, given the issues, a custom method using MongoTemplate is best, or we
    // use @Query with update.
    // Spring Data MongoDB supports @Update since 3.x.
    // If not available, we can just use the Service to reload-modify-save but be
    // VERY CAREFUL.
    // actually, let's use the @Query + @Update if possible, or just rely on a safer
    // service method.
    // Since I cannot start adding CustomImpl easily without verifying structure, I
    // will stick to logical fix in Service
    // BUT I WILL ADD A METHOD TO FETCH USER AND FORCE REFRESH if needed.

    // Actually, I can use a simple trick:
    // TripService should ONLY update carbon. PointsService should ONLY update
    // points.
    // The issue is concurrency/state.

    // Let's try to add a method that only updates carbon? No, SD Repository doesn't
    // do partial updates easily without @Update.
    // Let's Stick to the "refetch" fix but this time make sure we are not holding
    // the old object instance in any way.

    // Wait, I can't easily add @Update in this interface without knowing spring
    // data version.
    // I will try to use the "Remove Carbon Logic from PointsService" + "TripService
    // Refetch" strategy first,
    // AND I will add a log to TripService to verify the points before saving.
}

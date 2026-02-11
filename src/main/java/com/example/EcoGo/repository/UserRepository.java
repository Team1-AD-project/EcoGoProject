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
}

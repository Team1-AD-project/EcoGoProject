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
}

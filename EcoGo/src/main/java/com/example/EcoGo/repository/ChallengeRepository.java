package com.example.EcoGo.repository;

import com.example.EcoGo.model.Challenge;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChallengeRepository extends MongoRepository<Challenge, String> {
    List<Challenge> findByStatus(String status);
    List<Challenge> findByType(String type);
    List<Challenge> findByStatusIn(List<String> statuses);
}

package com.example.EcoGo.repository;

import com.example.EcoGo.model.TransportMode;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface TransportModeRepository extends MongoRepository<TransportMode, String> {
    Optional<TransportMode> findByMode(String mode);
}

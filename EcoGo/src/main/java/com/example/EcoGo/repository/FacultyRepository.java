package com.example.EcoGo.repository;

import com.example.EcoGo.model.Faculty;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface FacultyRepository extends MongoRepository<Faculty, String> {
    Optional<Faculty> findByName(String name);
}

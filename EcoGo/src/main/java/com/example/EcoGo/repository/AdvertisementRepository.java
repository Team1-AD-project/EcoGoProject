package com.example.EcoGo.repository;

import com.example.EcoGo.model.Advertisement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AdvertisementRepository extends MongoRepository<Advertisement, String> {
    List<Advertisement> findByStatus(String status);
    Page<Advertisement> findByNameContainingIgnoreCase(String name, Pageable pageable);
}

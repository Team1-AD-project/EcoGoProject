package com.example.EcoGo.repository;

import com.example.EcoGo.model.Ranking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RankingRepository extends MongoRepository<Ranking, String> {

    // Find and sort by carbonSaved for dynamic ranking
    Page<Ranking> findByPeriodOrderByCarbonSavedDesc(String period, Pageable pageable);
    Page<Ranking> findByPeriodAndNicknameContainingIgnoreCaseOrderByCarbonSavedDesc(String period, String nickname, Pageable pageable);

    // For statistics
    List<Ranking> findByPeriod(String period);
    List<Ranking> findByPeriodAndNicknameContainingIgnoreCase(String period, String nickname);
}

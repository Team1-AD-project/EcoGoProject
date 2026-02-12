package com.example.EcoGo.repository;

import com.example.EcoGo.model.VipGlobalSwitch;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface VipGlobalSwitchRepository extends MongoRepository<VipGlobalSwitch, String> {
    Optional<VipGlobalSwitch> findBySwitchKey(String switchKey);
}

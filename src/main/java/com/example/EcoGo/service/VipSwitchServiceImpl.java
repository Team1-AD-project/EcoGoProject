package com.example.EcoGo.service;

import com.example.EcoGo.interfacemethods.VipSwitchService;
import com.example.EcoGo.model.VipGlobalSwitch;
import com.example.EcoGo.repository.VipGlobalSwitchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VipSwitchServiceImpl implements VipSwitchService {

    @Autowired
    private VipGlobalSwitchRepository switchRepository;

    @Override
    public boolean isSwitchEnabled(String switchKey) {
        if (switchKey == null || switchKey.isEmpty())
            return false;

        return switchRepository.findBySwitchKey(switchKey)
                .map(VipGlobalSwitch::isEnabled)
                .orElse(false); // Default to false if not configured? Or true? User didn't specify, false is
                                // safer.
    }

    @Override
    public List<VipGlobalSwitch> getAllSwitches() {
        return switchRepository.findAll();
    }

    @Override
    public VipGlobalSwitch setSwitch(String key, boolean isEnabled, String updatedBy) {
        VipGlobalSwitch vipSwitch = switchRepository.findBySwitchKey(key)
                .orElse(new VipGlobalSwitch(key, key, "Auto-created switch"));

        vipSwitch.setEnabled(isEnabled);
        vipSwitch.setUpdatedBy(updatedBy);
        vipSwitch.setUpdatedAt(LocalDateTime.now());

        return switchRepository.save(vipSwitch);
    }
}

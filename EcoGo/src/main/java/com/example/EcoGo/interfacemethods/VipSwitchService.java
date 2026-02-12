package com.example.EcoGo.interfacemethods;

import com.example.EcoGo.model.VipGlobalSwitch;
import java.util.List;

public interface VipSwitchService {
    /**
     * Check if a specific VIP feature is enabled globally.
     * 
     * @param switchKey The unique key of the switch (e.g., "exclusive_badges")
     * @return true if enabled, false otherwise (or if not found)
     */
    boolean isSwitchEnabled(String switchKey);

    /**
     * Get all switches (Admin use)
     */
    List<VipGlobalSwitch> getAllSwitches();

    /**
     * Create or Update a switch
     */
    VipGlobalSwitch setSwitch(String key, boolean isEnabled, String updatedBy);
}

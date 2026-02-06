package com.example.EcoGo.controller;

import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.interfacemethods.VipSwitchService;
import com.example.EcoGo.model.VipGlobalSwitch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/vip-switches")
@CrossOrigin(origins = "*")
public class VipSwitchController {

    @Autowired
    private VipSwitchService vipSwitchService;

    // 获取特定开关状态
    @GetMapping("/{key}")
    public ResponseMessage<Map<String, Object>> getSwitchStatus(@PathVariable String key) {
        boolean enabled = vipSwitchService.isSwitchEnabled(key);
        return ResponseMessage.success(Map.of("key", key, "isEnabled", enabled));
    }

    // 获取所有开关
    @GetMapping
    public ResponseMessage<List<VipGlobalSwitch>> getAllSwitches() {
        return ResponseMessage.success(vipSwitchService.getAllSwitches());
    }

    // 更新或创建开关
    @PostMapping
    public ResponseMessage<VipGlobalSwitch> setSwitch(@RequestBody Map<String, Object> payload) {
        String key = (String) payload.get("switchKey");
        Boolean isEnabled = (Boolean) payload.get("isEnabled");
        String updatedBy = (String) payload.get("updatedBy");

        if (key == null || isEnabled == null || updatedBy == null || updatedBy.isBlank()) {
            throw new IllegalArgumentException("switchKey, isEnabled, and updatedBy are required");
        }

        return ResponseMessage.success(vipSwitchService.setSwitch(key, isEnabled, updatedBy));
    }
}

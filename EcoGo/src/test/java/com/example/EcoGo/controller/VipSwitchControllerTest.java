package com.example.EcoGo.controller;

import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.interfacemethods.VipSwitchService;
import com.example.EcoGo.model.VipGlobalSwitch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VipSwitchControllerTest {

    @Mock
    private VipSwitchService vipSwitchService;

    @InjectMocks
    private VipSwitchController vipSwitchController;

    private VipGlobalSwitch mockSwitch;

    @BeforeEach
    void setUp() {
        mockSwitch = new VipGlobalSwitch();
        mockSwitch.setSwitchKey("Double_points");
        mockSwitch.setEnabled(true);
    }

    @Test
    void getSwitchStatus_success() {
        when(vipSwitchService.isSwitchEnabled("Double_points")).thenReturn(true);

        ResponseMessage<Map<String, Object>> response = vipSwitchController.getSwitchStatus("Double_points");

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertEquals("Double_points", response.getData().get("key"));
        assertEquals(true, response.getData().get("isEnabled"));
    }

    @Test
    void getAllSwitches_success() {
        when(vipSwitchService.getAllSwitches()).thenReturn(Collections.singletonList(mockSwitch));

        ResponseMessage<List<VipGlobalSwitch>> response = vipSwitchController.getAllSwitches();

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().size());
    }

    @Test
    void setSwitch_success() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("switchKey", "Double_points");
        payload.put("isEnabled", false);
        payload.put("updatedBy", "admin");

        mockSwitch.setEnabled(false);
        when(vipSwitchService.setSwitch(anyString(), anyBoolean(), anyString())).thenReturn(mockSwitch);

        ResponseMessage<VipGlobalSwitch> response = vipSwitchController.setSwitch(payload);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertEquals(false, response.getData().isEnabled());
        verify(vipSwitchService).setSwitch("Double_points", false, "admin");
    }

    @Test
    void setSwitch_missingParams() {
        Map<String, Object> payload = new HashMap<>();
        // Missing key
        payload.put("isEnabled", false);
        payload.put("updatedBy", "admin");

        assertThrows(IllegalArgumentException.class, () -> vipSwitchController.setSwitch(payload));
    }
}

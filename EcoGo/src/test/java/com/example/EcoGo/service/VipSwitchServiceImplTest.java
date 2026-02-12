package com.example.EcoGo.service;

import com.example.EcoGo.model.VipGlobalSwitch;
import com.example.EcoGo.repository.VipGlobalSwitchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VipSwitchServiceImplTest {

    @Mock
    private VipGlobalSwitchRepository switchRepository;

    @InjectMocks
    private VipSwitchServiceImpl vipSwitchService;

    private VipGlobalSwitch mockSwitch;

    @BeforeEach
    void setUp() {
        mockSwitch = new VipGlobalSwitch();
        mockSwitch.setId("1");
        mockSwitch.setSwitchKey("Double_points");
        // mockSwitch.setSwitchName("Double_points"); // Method doesn't exist, and
        // seemingly redundant with key or not needed for this test context
        mockSwitch.setEnabled(true);
        mockSwitch.setUpdatedBy("admin");
        mockSwitch.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void isSwitchEnabled_true() {
        when(switchRepository.findBySwitchKey("Double_points")).thenReturn(Optional.of(mockSwitch));
        assertTrue(vipSwitchService.isSwitchEnabled("Double_points"));
    }

    @Test
    void isSwitchEnabled_false() {
        mockSwitch.setEnabled(false);
        when(switchRepository.findBySwitchKey("Double_points")).thenReturn(Optional.of(mockSwitch));
        assertFalse(vipSwitchService.isSwitchEnabled("Double_points"));
    }

    @Test
    void isSwitchEnabled_notFound() {
        when(switchRepository.findBySwitchKey("Unknown")).thenReturn(Optional.empty());
        assertFalse(vipSwitchService.isSwitchEnabled("Unknown"));
    }

    @Test
    void isSwitchEnabled_nullOrEmpty() {
        assertFalse(vipSwitchService.isSwitchEnabled(null));
        assertFalse(vipSwitchService.isSwitchEnabled(""));
    }

    @Test
    void getAllSwitches_success() {
        when(switchRepository.findAll()).thenReturn(Arrays.asList(mockSwitch));
        List<VipGlobalSwitch> result = vipSwitchService.getAllSwitches();
        assertEquals(1, result.size());
        assertEquals("Double_points", result.get(0).getSwitchKey());
    }

    @Test
    void setSwitch_updateExisting() {
        when(switchRepository.findBySwitchKey("Double_points")).thenReturn(Optional.of(mockSwitch));
        when(switchRepository.save(any(VipGlobalSwitch.class))).thenReturn(mockSwitch);

        VipGlobalSwitch result = vipSwitchService.setSwitch("Double_points", false, "newAdmin");

        assertFalse(result.isEnabled());
        assertEquals("newAdmin", result.getUpdatedBy());
        verify(switchRepository).save(mockSwitch);
    }

    @Test
    void setSwitch_createNew() {
        when(switchRepository.findBySwitchKey("New_Switch")).thenReturn(Optional.empty());
        when(switchRepository.save(any(VipGlobalSwitch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VipGlobalSwitch result = vipSwitchService.setSwitch("New_Switch", true, "admin");

        assertNotNull(result);
        assertEquals("New_Switch", result.getSwitchKey());
        assertTrue(result.isEnabled());
        assertEquals("admin", result.getUpdatedBy());
        verify(switchRepository).save(any(VipGlobalSwitch.class));
    }
}

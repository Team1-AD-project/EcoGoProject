package com.example.EcoGo.controller;

import com.example.EcoGo.dto.AdvertisementRequestDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.interfacemethods.AdvertisementInterface;
import com.example.EcoGo.model.Advertisement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdvertisementControllerTest {

    private AdvertisementInterface advertisementService;
    private AdvertisementController controller;

    @BeforeEach
    void setUp() throws Exception {
        advertisementService = mock(AdvertisementInterface.class);
        controller = new AdvertisementController();

        Field f = AdvertisementController.class.getDeclaredField("advertisementService");
        f.setAccessible(true);
        f.set(controller, advertisementService);
    }

    // ---------- helper ----------
    private static Advertisement buildAd(String id, String name, String status) {
        Advertisement ad = new Advertisement(name, "Test description", status,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                "https://img.example.com/ad.png", "https://example.com", "banner");
        ad.setId(id);
        return ad;
    }

    // ---------- getAllWebAdvertisements ----------
    @Test
    void getAllWebAdvertisements_success_noFilter() {
        Advertisement ad1 = buildAd("ad1", "Summer Sale", "Active");
        Advertisement ad2 = buildAd("ad2", "Winter Sale", "Inactive");
        Page<Advertisement> page = new PageImpl<>(List.of(ad1, ad2), PageRequest.of(0, 10), 2);
        when(advertisementService.getAllAdvertisements(eq(""), any(Pageable.class))).thenReturn(page);

        ResponseMessage<Page<Advertisement>> resp = controller.getAllWebAdvertisements("", 0, 10);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(2, resp.getData().getTotalElements());
        verify(advertisementService).getAllAdvertisements(eq(""), any(Pageable.class));
    }

    @Test
    void getAllWebAdvertisements_withNameFilter() {
        Advertisement ad = buildAd("ad1", "Summer Sale", "Active");
        Page<Advertisement> page = new PageImpl<>(List.of(ad), PageRequest.of(0, 10), 1);
        when(advertisementService.getAllAdvertisements(eq("Summer"), any(Pageable.class))).thenReturn(page);

        ResponseMessage<Page<Advertisement>> resp = controller.getAllWebAdvertisements("Summer", 0, 10);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(1, resp.getData().getTotalElements());
    }

    @Test
    void getAllWebAdvertisements_emptyResult() {
        Page<Advertisement> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(advertisementService.getAllAdvertisements(eq("nothing"), any(Pageable.class))).thenReturn(emptyPage);

        ResponseMessage<Page<Advertisement>> resp = controller.getAllWebAdvertisements("nothing", 0, 10);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(0, resp.getData().getTotalElements());
    }

    // ---------- getWebAdvertisementById ----------
    @Test
    void getWebAdvertisementById_success() {
        Advertisement ad = buildAd("ad1", "Summer Sale", "Active");
        when(advertisementService.getAdvertisementById("ad1")).thenReturn(ad);

        ResponseMessage<Advertisement> resp = controller.getWebAdvertisementById("ad1");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals("ad1", resp.getData().getId());
        assertEquals("Summer Sale", resp.getData().getName());
    }

    // ---------- createWebAdvertisement ----------
    @Test
    void createWebAdvertisement_success() {
        AdvertisementRequestDto dto = new AdvertisementRequestDto();
        dto.setName("New Ad");
        dto.setDescription("Test description");
        dto.setStatus("Active");
        dto.setStartDate(LocalDate.of(2026, 1, 1));
        dto.setEndDate(LocalDate.of(2026, 12, 31));
        dto.setImageUrl("https://img.example.com/ad.png");
        dto.setLinkUrl("https://example.com");
        dto.setPosition("banner");
        Advertisement created = buildAd("ad3", "New Ad", "Active");
        when(advertisementService.createAdvertisement(any(Advertisement.class))).thenReturn(created);

        ResponseMessage<Advertisement> resp = controller.createWebAdvertisement(dto);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals("ad3", resp.getData().getId());
        verify(advertisementService).createAdvertisement(any(Advertisement.class));
    }

    // ---------- updateWebAdvertisement ----------
    @Test
    void updateWebAdvertisement_success() {
        AdvertisementRequestDto dto = new AdvertisementRequestDto();
        dto.setName("Updated Ad");
        dto.setStatus("Active");
        Advertisement updated = buildAd("ad1", "Updated Ad", "Active");
        when(advertisementService.updateAdvertisement(eq("ad1"), any(Advertisement.class))).thenReturn(updated);

        ResponseMessage<Advertisement> resp = controller.updateWebAdvertisement("ad1", dto);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals("Updated Ad", resp.getData().getName());
    }

    // ---------- deleteWebAdvertisement ----------
    @Test
    void deleteWebAdvertisement_success() {
        doNothing().when(advertisementService).deleteAdvertisement("ad1");

        ResponseMessage<Void> resp = controller.deleteWebAdvertisement("ad1");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        verify(advertisementService).deleteAdvertisement("ad1");
    }

    // ---------- updateWebAdvertisementStatus ----------
    @Test
    void updateWebAdvertisementStatus_success() {
        Advertisement updated = buildAd("ad1", "Summer Sale", "Paused");
        when(advertisementService.updateAdvertisementStatus("ad1", "Paused")).thenReturn(updated);

        ResponseMessage<Advertisement> resp = controller.updateWebAdvertisementStatus("ad1", "Paused");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals("Paused", resp.getData().getStatus());
    }

    // ---------- getActiveAdvertisements ----------
    @Test
    void getActiveAdvertisements_success() {
        Advertisement ad1 = buildAd("ad1", "Summer Sale", "Active");
        Advertisement ad2 = buildAd("ad2", "Spring Promo", "Active");
        when(advertisementService.getActiveAdvertisements()).thenReturn(List.of(ad1, ad2));

        ResponseMessage<List<Advertisement>> resp = controller.getActiveAdvertisements();

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(2, resp.getData().size());
    }

    @Test
    void getActiveAdvertisements_emptyResult() {
        when(advertisementService.getActiveAdvertisements()).thenReturn(List.of());

        ResponseMessage<List<Advertisement>> resp = controller.getActiveAdvertisements();

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertTrue(resp.getData().isEmpty());
    }
}

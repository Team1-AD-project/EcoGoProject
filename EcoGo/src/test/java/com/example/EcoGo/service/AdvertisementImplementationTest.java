package com.example.EcoGo.service;

import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.model.Advertisement;
import com.example.EcoGo.repository.AdvertisementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvertisementImplementationTest {

    @Mock private AdvertisementRepository advertisementRepository;

    @InjectMocks private AdvertisementImplementation advertisementService;

    // ---------- helper ----------
    private static Advertisement buildAd(String id, String name, String status) {
        Advertisement ad = new Advertisement(name, "Test description", status,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                "https://img.example.com/ad.png", "https://example.com", "banner");
        ad.setId(id);
        return ad;
    }

    // ---------- getAllAdvertisements ----------
    @Test
    void getAllAdvertisements_noFilter_shouldDelegateFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Advertisement> page = new PageImpl<>(List.of(buildAd("ad1", "Sale", "Active")), pageable, 1);
        when(advertisementRepository.findAll(pageable)).thenReturn(page);

        Page<Advertisement> result = advertisementService.getAllAdvertisements("", pageable);

        assertEquals(1, result.getTotalElements());
        verify(advertisementRepository).findAll(pageable);
        verify(advertisementRepository, never()).findByNameContainingIgnoreCase(anyString(), any());
    }

    @Test
    void getAllAdvertisements_nullName_shouldDelegateFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Advertisement> page = new PageImpl<>(List.of(), pageable, 0);
        when(advertisementRepository.findAll(pageable)).thenReturn(page);

        Page<Advertisement> result = advertisementService.getAllAdvertisements(null, pageable);

        assertEquals(0, result.getTotalElements());
        verify(advertisementRepository).findAll(pageable);
    }

    @Test
    void getAllAdvertisements_withFilter_shouldDelegateFindByName() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Advertisement> page = new PageImpl<>(List.of(buildAd("ad1", "Summer Sale", "Active")), pageable, 1);
        when(advertisementRepository.findByNameContainingIgnoreCase("Summer", pageable)).thenReturn(page);

        Page<Advertisement> result = advertisementService.getAllAdvertisements("Summer", pageable);

        assertEquals(1, result.getTotalElements());
        verify(advertisementRepository).findByNameContainingIgnoreCase("Summer", pageable);
    }

    // ---------- getAdvertisementById ----------
    @Test
    void getAdvertisementById_success() {
        Advertisement ad = buildAd("ad1", "Sale", "Active");
        when(advertisementRepository.findById("ad1")).thenReturn(Optional.of(ad));

        Advertisement result = advertisementService.getAdvertisementById("ad1");

        assertEquals("ad1", result.getId());
        assertEquals("Sale", result.getName());
    }

    @Test
    void getAdvertisementById_notFound() {
        when(advertisementRepository.findById("x")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> advertisementService.getAdvertisementById("x"));
        assertEquals(ErrorCode.ADVERTISEMENT_NOT_FOUND.getCode(), ex.getCode());
    }

    // ---------- createAdvertisement ----------
    @Test
    void createAdvertisement_success() {
        Advertisement input = buildAd(null, "New Ad", "Active");
        Advertisement saved = buildAd("ad3", "New Ad", "Active");
        when(advertisementRepository.save(input)).thenReturn(saved);

        Advertisement result = advertisementService.createAdvertisement(input);

        assertEquals("ad3", result.getId());
        verify(advertisementRepository).save(input);
    }

    // ---------- updateAdvertisement ----------
    @Test
    void updateAdvertisement_success() {
        Advertisement existing = buildAd("ad1", "Old Name", "Active");
        when(advertisementRepository.findById("ad1")).thenReturn(Optional.of(existing));
        when(advertisementRepository.save(any(Advertisement.class))).thenAnswer(inv -> inv.getArgument(0));

        Advertisement update = buildAd(null, "New Name", "Paused");
        update.setPosition("sidebar");

        Advertisement result = advertisementService.updateAdvertisement("ad1", update);

        assertEquals("ad1", result.getId());
        assertEquals("New Name", result.getName());
        assertEquals("Paused", result.getStatus());
        assertEquals("sidebar", result.getPosition());
    }

    @Test
    void updateAdvertisement_notFound() {
        when(advertisementRepository.findById("x")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> advertisementService.updateAdvertisement("x", new Advertisement()));
        assertEquals(ErrorCode.ADVERTISEMENT_NOT_FOUND.getCode(), ex.getCode());
    }

    // ---------- deleteAdvertisement ----------
    @Test
    void deleteAdvertisement_success() {
        when(advertisementRepository.existsById("ad1")).thenReturn(true);

        advertisementService.deleteAdvertisement("ad1");

        verify(advertisementRepository).deleteById("ad1");
    }

    @Test
    void deleteAdvertisement_notFound() {
        when(advertisementRepository.existsById("x")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> advertisementService.deleteAdvertisement("x"));
        assertEquals(ErrorCode.ADVERTISEMENT_NOT_FOUND.getCode(), ex.getCode());
    }

    // ---------- getAdvertisementsByStatus ----------
    @Test
    void getAdvertisementsByStatus_success() {
        Advertisement ad = buildAd("ad1", "Sale", "Active");
        when(advertisementRepository.findByStatus("Active")).thenReturn(List.of(ad));

        List<Advertisement> result = advertisementService.getAdvertisementsByStatus("Active");

        assertEquals(1, result.size());
        assertEquals("Active", result.get(0).getStatus());
    }

    @Test
    void getAdvertisementsByStatus_empty() {
        when(advertisementRepository.findByStatus("Paused")).thenReturn(List.of());

        List<Advertisement> result = advertisementService.getAdvertisementsByStatus("Paused");

        assertTrue(result.isEmpty());
    }

    // ---------- updateAdvertisementStatus ----------
    @Test
    void updateAdvertisementStatus_success() {
        Advertisement existing = buildAd("ad1", "Sale", "Active");
        when(advertisementRepository.findById("ad1")).thenReturn(Optional.of(existing));
        when(advertisementRepository.save(any(Advertisement.class))).thenAnswer(inv -> inv.getArgument(0));

        Advertisement result = advertisementService.updateAdvertisementStatus("ad1", "Paused");

        assertEquals("Paused", result.getStatus());
        verify(advertisementRepository).save(existing);
    }

    @Test
    void updateAdvertisementStatus_notFound() {
        when(advertisementRepository.findById("x")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> advertisementService.updateAdvertisementStatus("x", "Paused"));
        assertEquals(ErrorCode.ADVERTISEMENT_NOT_FOUND.getCode(), ex.getCode());
    }

    // ---------- getActiveAdvertisements ----------
    @Test
    void getActiveAdvertisements_success() {
        Advertisement ad1 = buildAd("ad1", "Summer", "Active");
        Advertisement ad2 = buildAd("ad2", "Spring", "Active");
        when(advertisementRepository.findByStatus("Active")).thenReturn(List.of(ad1, ad2));

        List<Advertisement> result = advertisementService.getActiveAdvertisements();

        assertEquals(2, result.size());
        verify(advertisementRepository).findByStatus("Active");
    }

    @Test
    void getActiveAdvertisements_empty() {
        when(advertisementRepository.findByStatus("Active")).thenReturn(List.of());

        List<Advertisement> result = advertisementService.getActiveAdvertisements();

        assertTrue(result.isEmpty());
    }
}

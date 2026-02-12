package com.example.EcoGo.controller;

import com.example.EcoGo.dto.RecommendationRequestDto;
import com.example.EcoGo.dto.RecommendationResponseDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.dto.chatbot.ChatResponseDto;
import com.example.EcoGo.service.chatbot.RagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecommendationControllerTest {

    private RagService ragService;
    private RecommendationController controller;

    @BeforeEach
    void setUp() throws Exception {
        ragService = mock(RagService.class);
        controller = new RecommendationController();

        Field f = RecommendationController.class.getDeclaredField("ragService");
        f.setAccessible(true);
        f.set(controller, ragService);
    }

    // ---------- RAG-based recommendation ----------
    @Test
    void recommend_ragAvailableWithCitations_shouldReturnRagBasedResponse() {
        when(ragService.isAvailable()).thenReturn(true);
        ChatResponseDto.Citation citation = new ChatResponseDto.Citation("Green Travel", "source1", "Take the MRT for low carbon travel.");
        when(ragService.retrieve(anyString(), eq(2))).thenReturn(List.of(citation));

        RecommendationRequestDto req = new RecommendationRequestDto();
        req.setDestination("Marina Bay");

        ResponseMessage<RecommendationResponseDto> resp = controller.recommend(req);

        assertEquals(HttpStatus.OK.value(), resp.getCode());
        assertNotNull(resp.getData());
        assertEquals("Eco-RAG", resp.getData().getTag());
        assertTrue(resp.getData().getText().contains("Marina Bay"));
    }

    @Test
    void recommend_ragAvailableButNoCitations_shouldFallbackToKeyword() {
        when(ragService.isAvailable()).thenReturn(true);
        when(ragService.retrieve(anyString(), eq(2))).thenReturn(Collections.emptyList());

        RecommendationRequestDto req = new RecommendationRequestDto();
        req.setDestination("library");

        ResponseMessage<RecommendationResponseDto> resp = controller.recommend(req);

        assertEquals(HttpStatus.OK.value(), resp.getCode());
        assertNotNull(resp.getData());
        assertEquals("Eco-Choice", resp.getData().getTag());
        assertTrue(resp.getData().getText().contains("library"));
    }

    @Test
    void recommend_ragThrowsException_shouldFallbackToKeyword() {
        when(ragService.isAvailable()).thenReturn(true);
        when(ragService.retrieve(anyString(), eq(2))).thenThrow(new RuntimeException("rag error"));

        RecommendationRequestDto req = new RecommendationRequestDto();
        req.setDestination("gym");

        ResponseMessage<RecommendationResponseDto> resp = controller.recommend(req);

        assertEquals(HttpStatus.OK.value(), resp.getCode());
        assertEquals("Healthy", resp.getData().getTag());
    }

    // ---------- Keyword-based fallback ----------
    @Test
    void recommend_libraryKeyword_shouldReturnEcoChoice() {
        when(ragService.isAvailable()).thenReturn(false);

        RecommendationRequestDto req = new RecommendationRequestDto();
        req.setDestination("study room");

        ResponseMessage<RecommendationResponseDto> resp = controller.recommend(req);

        assertEquals("Eco-Choice", resp.getData().getTag());
    }

    @Test
    void recommend_gymKeyword_shouldReturnHealthy() {
        when(ragService.isAvailable()).thenReturn(false);

        RecommendationRequestDto req = new RecommendationRequestDto();
        req.setDestination("sports hall");

        ResponseMessage<RecommendationResponseDto> resp = controller.recommend(req);

        assertEquals("Healthy", resp.getData().getTag());
    }

    @Test
    void recommend_mrtKeyword_shouldReturnGreenTransit() {
        when(ragService.isAvailable()).thenReturn(false);

        RecommendationRequestDto req = new RecommendationRequestDto();
        req.setDestination("Orchard Road");

        ResponseMessage<RecommendationResponseDto> resp = controller.recommend(req);

        assertEquals("Green-Transit", resp.getData().getTag());
    }

    @Test
    void recommend_emptyDestination_shouldReturnGeneral() {
        when(ragService.isAvailable()).thenReturn(false);

        RecommendationRequestDto req = new RecommendationRequestDto();
        req.setDestination("");

        ResponseMessage<RecommendationResponseDto> resp = controller.recommend(req);

        assertEquals("General", resp.getData().getTag());
    }

    @Test
    void recommend_nullDestination_shouldReturnGeneral() {
        when(ragService.isAvailable()).thenReturn(false);

        RecommendationRequestDto req = new RecommendationRequestDto();
        req.setDestination(null);

        ResponseMessage<RecommendationResponseDto> resp = controller.recommend(req);

        assertEquals("General", resp.getData().getTag());
    }

    @Test
    void recommend_unknownDestination_shouldReturnFastest() {
        when(ragService.isAvailable()).thenReturn(false);

        RecommendationRequestDto req = new RecommendationRequestDto();
        req.setDestination("Jurong East");

        ResponseMessage<RecommendationResponseDto> resp = controller.recommend(req);

        assertEquals("Fastest", resp.getData().getTag());
    }
}

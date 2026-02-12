package com.example.EcoGo.service.churn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SupportServiceImplTest {

    private FeatureExtractor featureExtractor;
    private ModelRunner modelRunner;
    private Thresholds thresholds;

    private SupportServiceImpl service;

    @BeforeEach
    void setUp() {
        featureExtractor = mock(FeatureExtractor.class);
        modelRunner = mock(ModelRunner.class);
        thresholds = mock(Thresholds.class);

        service = new SupportServiceImpl(featureExtractor, modelRunner, thresholds);
    }

    @Test
    void getChurnRiskLevel_whenFeatureVectorNull_shouldReturnInsufficientData() {
        when(featureExtractor.extract("u1")).thenReturn(null);

        ChurnRiskLevel level = service.getChurnRiskLevel("u1");

        assertEquals(ChurnRiskLevel.INSUFFICIENT_DATA, level);
        verify(modelRunner, never()).predictProbability(any());
        verify(thresholds, never()).toLevel(anyDouble());
    }

    @Test
    void getChurnRiskLevel_whenFeatureVectorInsufficient_shouldReturnInsufficientData() {
        ChurnFeatureVector fv = mock(ChurnFeatureVector.class);
        when(featureExtractor.extract("u1")).thenReturn(fv);
        when(fv.isInsufficient()).thenReturn(true);

        ChurnRiskLevel level = service.getChurnRiskLevel("u1");

        assertEquals(ChurnRiskLevel.INSUFFICIENT_DATA, level);
        verify(modelRunner, never()).predictProbability(any());
        verify(thresholds, never()).toLevel(anyDouble());
    }

    @Test
    void getChurnRiskLevel_whenSufficient_shouldCallModelRunnerAndThresholds() {
        ChurnFeatureVector fv = mock(ChurnFeatureVector.class);
        when(featureExtractor.extract("u1")).thenReturn(fv);
        when(fv.isInsufficient()).thenReturn(false);
        when(fv.toArray()).thenReturn(new float[]{1f, 2f, 3f});

        when(modelRunner.predictProbability(any(float[].class))).thenReturn(0.73);
        when(thresholds.toLevel(0.73)).thenReturn(ChurnRiskLevel.HIGH);

        ChurnRiskLevel level = service.getChurnRiskLevel("u1");

        assertEquals(ChurnRiskLevel.HIGH, level);

        verify(featureExtractor).extract("u1");
        verify(modelRunner).predictProbability(any(float[].class));
        verify(thresholds).toLevel(0.73);
    }

    @Test
    void getChurnRiskLevel_shouldPassSameArrayToModelRunner() {
        ChurnFeatureVector fv = mock(ChurnFeatureVector.class);
        float[] arr = new float[]{9f, 8f};

        when(featureExtractor.extract("u1")).thenReturn(fv);
        when(fv.isInsufficient()).thenReturn(false);
        when(fv.toArray()).thenReturn(arr);

        when(modelRunner.predictProbability(arr)).thenReturn(0.12);
        when(thresholds.toLevel(0.12)).thenReturn(ChurnRiskLevel.LOW);

        ChurnRiskLevel level = service.getChurnRiskLevel("u1");

        assertEquals(ChurnRiskLevel.LOW, level);
        verify(modelRunner).predictProbability(arr);
    }
}

package com.example.EcoGo.controller;

import com.example.EcoGo.dto.ChurnRiskDTO;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.service.churn.ChurnRiskLevel;
import com.example.EcoGo.service.churn.SupportService;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SupportChurnControllerTest {

    private SupportService supportService;
    private MongoTemplate mongoTemplate;
    private SupportChurnController controller;

    @BeforeEach
    void setUp() {
        supportService = mock(SupportService.class);
        mongoTemplate = mock(MongoTemplate.class);
        controller = new SupportChurnController(supportService, mongoTemplate);
    }

    // ---------- /churn ----------
    @Test
    void churn_userIdBlank_shouldReturnParamError() {
        ResponseMessage<ChurnRiskDTO> resp = controller.churn("  ");

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), resp.getCode());
        assertNull(resp.getData());
        // message 里面会带 userId（因为 getMessage("userId")）
        assertNotNull(resp.getMessage());
    }

    @Test
    void churn_validUserId_shouldReturnSuccessAndCallService() {
        when(supportService.getChurnRiskLevel("u1")).thenReturn(ChurnRiskLevel.HIGH);

        ResponseMessage<ChurnRiskDTO> resp = controller.churn("u1");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        assertEquals("u1", resp.getData().getUserId());
        assertEquals("HIGH", resp.getData().getRiskLevel());

        verify(supportService).getChurnRiskLevel("u1");
        verifyNoInteractions(mongoTemplate);
    }

    // ---------- /churn/me ----------
    @Test
    void myChurn_shouldBehaveSameAsChurn() {
        when(supportService.getChurnRiskLevel("u2")).thenReturn(ChurnRiskLevel.LOW);

        ResponseMessage<ChurnRiskDTO> resp = controller.myChurn("u2");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        assertEquals("u2", resp.getData().getUserId());
        assertEquals("LOW", resp.getData().getRiskLevel());

        verify(supportService).getChurnRiskLevel("u2");
        verifyNoInteractions(mongoTemplate);
    }

    // ---------- /churn/admin/all ----------
    @Test
    void allUsersChurn_shouldReadUsersFromMongo_filterNull_distinct_andCallServicePerUser() {
        // docs: u1, u2, u1(duplicate), null(userid missing)
        Document d1 = new Document("userid", "u1");
        Document d2 = new Document("userid", "u2");
        Document d3 = new Document("userid", "u1");
        Document d4 = new Document("xxx", "no-userid");

        when(mongoTemplate.findAll(Document.class, "users"))
                .thenReturn(List.of(d1, d2, d3, d4));

        when(supportService.getChurnRiskLevel("u1")).thenReturn(ChurnRiskLevel.MEDIUM);
        when(supportService.getChurnRiskLevel("u2")).thenReturn(ChurnRiskLevel.HIGH);

        ResponseMessage<List<ChurnRiskDTO>> resp = controller.allUsersChurn();

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        // distinct 后应该只有 u1,u2 两条
        assertEquals(2, resp.getData().size());

        // 内容校验（不依赖顺序就用 contains）
        boolean hasU1 = resp.getData().stream()
                .anyMatch(x -> "u1".equals(x.getUserId()) && "MEDIUM".equals(x.getRiskLevel()));
        boolean hasU2 = resp.getData().stream()
                .anyMatch(x -> "u2".equals(x.getUserId()) && "HIGH".equals(x.getRiskLevel()));
        assertTrue(hasU1);
        assertTrue(hasU2);

        verify(mongoTemplate).findAll(Document.class, "users");
        verify(supportService, times(1)).getChurnRiskLevel("u1");
        verify(supportService, times(1)).getChurnRiskLevel("u2");
    }
}

package com.example.EcoGo.controller;

import com.example.EcoGo.dto.ChurnRiskDTO;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.service.churn.ChurnRiskLevel;
import com.example.EcoGo.service.churn.SupportService;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/support")
public class SupportChurnController {

    private final SupportService supportService;
    private final MongoTemplate mongoTemplate;

    public SupportChurnController(SupportService supportService, MongoTemplate mongoTemplate) {
        this.supportService = supportService;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * ✅ 兼容旧接口（原来就是这个）：GET /support/churn?userId=xxx
     * 现在改为统一返回 code/message/data
     */
    @GetMapping("/churn")
    public ResponseMessage<ChurnRiskDTO> churn(@RequestParam String userId) {
        return doGetChurn(userId);
    }

    /**
     * ✅ 用户接口：GET /support/churn/me?userId=xxx
     */
    @GetMapping("/churn/me")
    public ResponseMessage<ChurnRiskDTO> myChurn(@RequestParam String userId) {
        return doGetChurn(userId);
    }

    /**
     * ✅ 管理员接口：GET /support/churn/admin/all
     * 返回所有用户的 riskLevel 列表
     */
    @GetMapping("/churn/admin/all")
    public ResponseMessage<List<ChurnRiskDTO>> allUsersChurn() {
        // users 集合里取 userid（你当前 extractor 也是按 userid 查）
        List<Document> docs = mongoTemplate.findAll(Document.class, "users");

        List<String> userIds = docs.stream()
                .map(d -> d.getString("userid"))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<ChurnRiskDTO> data = userIds.stream()
                .map(uid -> {
                    ChurnRiskLevel level = supportService.getChurnRiskLevel(uid);
                    return new ChurnRiskDTO(uid, level.name());
                })
                .collect(Collectors.toList());

        return ResponseMessage.success(data);
    }

    // ===== helper：复用单用户逻辑，避免重复写 =====
    private ResponseMessage<ChurnRiskDTO> doGetChurn(String userId) {
        if (userId == null || userId.isBlank()) {
            return new ResponseMessage<>(
                    ErrorCode.PARAM_ERROR.getCode(),
                    ErrorCode.PARAM_ERROR.getMessage("userId"),
                    null
            );
        }
        ChurnRiskLevel level = supportService.getChurnRiskLevel(userId);
        return ResponseMessage.success(new ChurnRiskDTO(userId, level.name()));
    }
}

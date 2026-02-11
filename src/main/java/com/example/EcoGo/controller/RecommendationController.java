package com.example.EcoGo.controller;

import com.example.EcoGo.dto.RecommendationRequestDto;
import com.example.EcoGo.dto.RecommendationResponseDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.dto.chatbot.ChatResponseDto;
import com.example.EcoGo.service.chatbot.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Recommendation controller for green travel suggestions.
 * Uses RAG service for context-aware recommendations,
 * with keyword-based fallback for common destinations.
 */
@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    @Autowired
    private RagService ragService;

    @PostMapping
    public ResponseMessage<RecommendationResponseDto> recommend(@RequestBody RecommendationRequestDto request) {
        String dest = request.getDestination() == null ? "" : request.getDestination().trim();
        String destLower = dest.toLowerCase();

        // Try RAG-based recommendation first
        if (ragService.isAvailable() && !dest.isEmpty()) {
            try {
                String query = "如何绿色出行到" + dest;
                List<ChatResponseDto.Citation> citations = ragService.retrieve(query, 2);
                if (!citations.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("🌿 前往 ").append(dest).append(" 的绿色出行建议：\n\n");
                    for (ChatResponseDto.Citation c : citations) {
                        sb.append(c.getSnippet()).append("\n");
                    }
                    return ResponseMessage.success(
                            new RecommendationResponseDto(sb.toString().trim(), "Eco-RAG"));
                }
            } catch (Exception ignored) {
                // Fall through to keyword-based
            }
        }

        // Keyword-based fallback recommendations
        RecommendationResponseDto rec;
        if (destLower.contains("library") || destLower.contains("study") ||
                destLower.contains("图书馆") || destLower.contains("学习")) {
            rec = new RecommendationResponseDto(
                    "🚶 步行前往" + dest + "大约15分钟，可获得50绿色积分。天气不错，推荐步行！",
                    "Eco-Choice");
        } else if (destLower.contains("gym") || destLower.contains("sport") ||
                destLower.contains("健身") || destLower.contains("运动")) {
            rec = new RecommendationResponseDto(
                    "🏃 慢跑前往" + dest + "，距离仅1.2公里，既环保又锻炼身体！",
                    "Healthy");
        } else if (destLower.contains("mrt") || destLower.contains("地铁") ||
                destLower.contains("orchard") || destLower.contains("乌节") ||
                destLower.contains("marina") || destLower.contains("滨海")) {
            rec = new RecommendationResponseDto(
                    "🚇 建议搭乘地铁MRT前往" + dest + "，快速且低碳排放。出站后可步行或骑共享单车到目的地。",
                    "Green-Transit");
        } else if (dest.isEmpty()) {
            rec = new RecommendationResponseDto(
                    "🌿 绿色出行小贴士：短途(<2km)步行或骑行；中途(2-10km)搭地铁或公交；长途(>10km)搭地铁或拼车。",
                    "General");
        } else {
            rec = new RecommendationResponseDto(
                    "🚌 建议搭乘公交前往" + dest + "，预计等待约3分钟。这是最快的绿色出行方式！",
                    "Fastest");
        }
        return ResponseMessage.success(rec);
    }
}

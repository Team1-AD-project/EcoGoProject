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

        RecommendationResponseDto rec = attemptRagRecommendation(dest, destLower);
        if (rec != null) {
            return ResponseMessage.success(rec);
        }

        rec = getKeywordBasedRecommendation(dest, destLower);
        return ResponseMessage.success(rec);
    }

    private RecommendationResponseDto attemptRagRecommendation(String dest, String destLower) {
        if (!ragService.isAvailable() || dest.isEmpty()) {
            return null;
        }

        try {
            String query = "如何绿色出行到" + dest;
            List<ChatResponseDto.Citation> citations = ragService.retrieve(query, 2);
            if (citations.isEmpty()) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("🌿 前往 ").append(dest).append(" 的绿色出行建议：\n\n");
            for (ChatResponseDto.Citation c : citations) {
                sb.append(c.getSnippet()).append("\n");
            }
            return new RecommendationResponseDto(sb.toString().trim(), "Eco-RAG");
        } catch (Exception ignored) {
            return null;
        }
    }

    private RecommendationResponseDto getKeywordBasedRecommendation(String dest, String destLower) {
        if (isLibraryDestination(destLower)) {
            return new RecommendationResponseDto(
                    "🚶 步行前往" + dest + "大约15分钟，可获得50绿色积分。天气不错，推荐步行！",
                    "Eco-Choice");
        }

        if (isGymDestination(destLower)) {
            return new RecommendationResponseDto(
                    "🏃 慢跑前往" + dest + "，距离仅1.2公里，既环保又锻炼身体！",
                    "Healthy");
        }

        if (isTransitDestination(destLower)) {
            return new RecommendationResponseDto(
                    "🚇 建议搭乘地铁MRT前往" + dest + "，快速且低碳排放。出站后可步行或骑共享单车到目的地。",
                    "Green-Transit");
        }

        if (dest.isEmpty()) {
            return new RecommendationResponseDto(
                    "🌿 绿色出行小贴士：短途(<2km)步行或骑行；中途(2-10km)搭地铁或公交；长途(>10km)搭地铁或拼车。",
                    "General");
        }

        return new RecommendationResponseDto(
                "🚌 建议搭乘公交前往" + dest + "，预计等待约3分钟。这是最快的绿色出行方式！",
                "Fastest");
    }

    private boolean isLibraryDestination(String destLower) {
        return destLower.contains("library") || destLower.contains("study") ||
                destLower.contains("图书馆") || destLower.contains("学习");
    }

    private boolean isGymDestination(String destLower) {
        return destLower.contains("gym") || destLower.contains("sport") ||
                destLower.contains("健身") || destLower.contains("运动");
    }

    private boolean isTransitDestination(String destLower) {
        return destLower.contains("mrt") || destLower.contains("地铁") ||
                destLower.contains("orchard") || destLower.contains("乌节") ||
                destLower.contains("marina") || destLower.contains("滨海");
    }
}

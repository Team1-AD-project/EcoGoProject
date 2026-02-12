package com.example.EcoGo.service;

import com.example.EcoGo.dto.LeaderboardEntry;
import com.example.EcoGo.dto.LeaderboardRankingDto;
import com.example.EcoGo.dto.LeaderboardStatsDto;
import com.example.EcoGo.interfacemethods.LeaderboardInterface;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.LeaderboardRewardRepository;
import com.example.EcoGo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LeaderboardImplementation implements LeaderboardInterface {

    private static final Logger logger = LoggerFactory.getLogger(LeaderboardImplementation.class);
    private static final String TOTAL_CARBON_SAVED_FIELD = "totalCarbonSaved";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaderboardRewardRepository rewardRepository;

    @Override
    public LeaderboardStatsDto getRankings(String type, String date, String name, int page, int size) {
        // 1. Compute date range from type + date
        LocalDateTime start;
        LocalDateTime end;
        String periodKey;

        if ("DAILY".equalsIgnoreCase(type)) {
            LocalDate targetDate = (date == null || date.isEmpty())
                    ? LocalDate.now()
                    : LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            start = targetDate.atStartOfDay();
            end = targetDate.plusDays(1).atStartOfDay();
            periodKey = targetDate.toString();
        } else {
            YearMonth targetMonth = (date == null || date.isEmpty())
                    ? YearMonth.now()
                    : YearMonth.parse(date, DateTimeFormatter.ofPattern("yyyy-MM"));
            start = targetMonth.atDay(1).atStartOfDay();
            end = targetMonth.atEndOfMonth().plusDays(1).atStartOfDay();
            periodKey = targetMonth.toString();
        }

        logger.info("Computing {} leaderboard for period {} ({} to {})", type, periodKey, start, end);

        // 2. Aggregate carbon_saved from completed trips
        List<LeaderboardEntry> allEntries = getTopUsers(start, end, 0); // 0 = no limit

        // 3. Batch fetch user info
        List<String> userIds = allEntries.stream()
                .map(LeaderboardEntry::getUserId)
                .collect(Collectors.toList());
        Map<String, User> userMap = getUserMap(userIds);

        // 4. Build ranking DTOs with user info
        List<LeaderboardRankingDto> allRankings = new ArrayList<>();
        for (int i = 0; i < allEntries.size(); i++) {
            LeaderboardEntry entry = allEntries.get(i);
            User user = userMap.get(entry.getUserId());
            String nickname = user != null ? user.getNickname() : entry.getUserId();
            boolean isVip = user != null && user.getVip() != null && user.getVip().isActive();

            allRankings.add(new LeaderboardRankingDto(
                    entry.getUserId(),
                    nickname,
                    i + 1,
                    entry.getTotalCarbonSaved(),
                    isVip,
                    type
            ));
        }

        // 5. Calculate stats from full list
        long totalCarbonSaved = Math.round(allRankings.stream()
                .mapToDouble(LeaderboardRankingDto::getCarbonSaved).sum());
        long totalVipUsers = allRankings.stream()
                .filter(LeaderboardRankingDto::isVip).count();

        // 6. Filter by nickname if search provided
        List<LeaderboardRankingDto> filtered = allRankings;
        if (name != null && !name.isEmpty()) {
            String lowerName = name.toLowerCase();
            filtered = allRankings.stream()
                    .filter(r -> r.getNickname() != null
                            && r.getNickname().toLowerCase().contains(lowerName))
                    .collect(Collectors.toList());
        }

        // 7. Paginate
        int fromIndex = Math.min(page * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        List<LeaderboardRankingDto> pageContent = filtered.subList(fromIndex, toIndex);
        Page<LeaderboardRankingDto> rankingsPage = new PageImpl<>(
                pageContent, PageRequest.of(page, size), filtered.size());

        // 8. Rewards distributed count for this period
        long rewardsDistributed = rewardRepository.findByTypeAndPeriodKey(type.toUpperCase(), periodKey).size();

        return new LeaderboardStatsDto(rankingsPage, totalCarbonSaved, totalVipUsers, rewardsDistributed);
    }

    @Override
    public List<LeaderboardEntry> getTopUsers(LocalDateTime start, LocalDateTime end, int limit) {
        Aggregation aggregation;
        if (limit > 0) {
            aggregation = Aggregation.newAggregation(
                    Aggregation.match(Criteria.where("carbon_status").is("completed")
                            .and("start_time").gte(start).lt(end)),
                    Aggregation.group("user_id").sum("carbon_saved").as(TOTAL_CARBON_SAVED_FIELD),
                    Aggregation.sort(Sort.Direction.DESC, TOTAL_CARBON_SAVED_FIELD),
                    Aggregation.limit(limit)
            );
        } else {
            aggregation = Aggregation.newAggregation(
                    Aggregation.match(Criteria.where("carbon_status").is("completed")
                            .and("start_time").gte(start).lt(end)),
                    Aggregation.group("user_id").sum("carbon_saved").as(TOTAL_CARBON_SAVED_FIELD),
                    Aggregation.sort(Sort.Direction.DESC, TOTAL_CARBON_SAVED_FIELD)
            );
        }

        AggregationResults<LeaderboardEntry> results = mongoTemplate.aggregate(
                aggregation, "trips", LeaderboardEntry.class);

        return results.getMappedResults();
    }

    private Map<String, User> getUserMap(List<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<User> users = userRepository.findByUseridIn(userIds);
        return users.stream().collect(Collectors.toMap(User::getUserid, u -> u, (a, b) -> a));
    }
}

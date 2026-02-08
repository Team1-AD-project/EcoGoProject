package com.example.EcoGo.scheduler;

import com.example.EcoGo.dto.LeaderboardEntry;
import com.example.EcoGo.interfacemethods.LeaderboardInterface;
import com.example.EcoGo.model.LeaderboardReward;
import com.example.EcoGo.model.User;
import com.example.EcoGo.model.UserPointsLog;
import com.example.EcoGo.repository.UserPointsLogRepository;
import com.example.EcoGo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Component
public class LeaderboardRewardScheduler {

    private static final Logger logger = LoggerFactory.getLogger(LeaderboardRewardScheduler.class);

    @Autowired
    private LeaderboardInterface leaderboardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPointsLogRepository pointsLogRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Daily reward: runs at 00:05 every day, rewards top 10 for YESTERDAY.
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void distributeDailyRewards() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String periodKey = yesterday.toString();

        // Use MongoTemplate to check duplicates (avoids @Field annotation issue on period_key)
        if (rewardAlreadyDistributed("DAILY", periodKey)) {
            logger.info("Daily rewards for {} already distributed. Skipping.", periodKey);
            return;
        }

        logger.info("Distributing daily leaderboard rewards for {}", periodKey);
        LocalDateTime start = yesterday.atStartOfDay();
        LocalDateTime end = yesterday.plusDays(1).atStartOfDay();

        List<LeaderboardEntry> topUsers = leaderboardService.getTopUsers(start, end, 10);
        distributeRewards(topUsers, "DAILY", periodKey);
    }

    /**
     * Monthly reward: runs at 00:10 on the 1st of each month, rewards top 10 for LAST MONTH.
     */
    @Scheduled(cron = "0 10 0 1 * ?")
    public void distributeMonthlyRewards() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        String periodKey = lastMonth.toString();

        if (rewardAlreadyDistributed("MONTHLY", periodKey)) {
            logger.info("Monthly rewards for {} already distributed. Skipping.", periodKey);
            return;
        }

        logger.info("Distributing monthly leaderboard rewards for {}", periodKey);
        LocalDateTime start = lastMonth.atDay(1).atStartOfDay();
        LocalDateTime end = lastMonth.atEndOfMonth().plusDays(1).atStartOfDay();

        List<LeaderboardEntry> topUsers = leaderboardService.getTopUsers(start, end, 10);
        distributeRewards(topUsers, "MONTHLY", periodKey);
    }

    /**
     * Check if rewards have already been distributed using MongoTemplate
     * (avoids @Field("period_key") derived query issue).
     */
    private boolean rewardAlreadyDistributed(String type, String periodKey) {
        Query query = new Query(Criteria.where("type").is(type).and("period_key").is(periodKey));
        return mongoTemplate.exists(query, LeaderboardReward.class);
    }

    private void distributeRewards(List<LeaderboardEntry> topUsers, String type, String periodKey) {
        for (int i = 0; i < topUsers.size(); i++) {
            int rank = i + 1;
            long multiplier = "DAILY".equals(type) ? 10L : 100L;
            long points = (11 - rank) * multiplier;
            LeaderboardEntry entry = topUsers.get(i);

            try {
                // 1. Fetch user (same as challenge's adjustPoints logic)
                Optional<User> userOpt = userRepository.findByUserid(entry.getUserId());
                if (userOpt.isEmpty()) {
                    logger.warn("User {} not found, skipping reward", entry.getUserId());
                    continue;
                }
                User user = userOpt.get();

                // 2. Update user points directly (same as PointsServiceImpl.adjustPoints)
                long newBalance = user.getCurrentPoints() + points;
                user.setCurrentPoints(newBalance);
                user.setTotalPoints(user.getTotalPoints() + points);
                userRepository.save(user);

                // 3. Create points log (same as PointsServiceImpl.adjustPoints)
                String description = String.format("Leaderboard %s Rank #%d reward (%s)", type, rank, periodKey);
                UserPointsLog log = new UserPointsLog();
                log.setId(java.util.UUID.randomUUID().toString());
                log.setUserId(user.getUserid());
                log.setChangeType("gain");
                log.setPoints(points);
                log.setSource("leaderboard");
                log.setDescription(description);
                log.setBalanceAfter(newBalance);
                pointsLogRepository.save(log);

                // 4. Save leaderboard reward record
                LeaderboardReward reward = new LeaderboardReward();
                reward.setType(type);
                reward.setPeriodKey(periodKey);
                reward.setUserId(entry.getUserId());
                reward.setRank(rank);
                reward.setPointsAwarded(points);
                reward.setCarbonSaved(entry.getTotalCarbonSaved());
                reward.setDistributedAt(LocalDateTime.now());
                mongoTemplate.save(reward);

                logger.info("Awarded {} points to user {} (Rank #{}) for {} {}, balanceAfter={}",
                        points, entry.getUserId(), rank, type, periodKey, newBalance);
            } catch (Exception e) {
                logger.error("Failed to reward user {} (Rank #{}) for {} {}: {}",
                        entry.getUserId(), rank, type, periodKey, e.getMessage());
            }
        }
        logger.info("Finished {} rewards for {}: {} users rewarded", type, periodKey, topUsers.size());
    }
}

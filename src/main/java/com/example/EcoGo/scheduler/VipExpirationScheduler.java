package com.example.EcoGo.scheduler;

import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class VipExpirationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(VipExpirationScheduler.class);

    @Autowired
    private UserRepository userRepository;

    /**
     * Check for expired VIPs every hour.
     * Cron: 0 0 * * * ? (Every hour at minute 0)
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void checkVipExpiration() {
        // logger.info("Running VIP Expiration Check...");

        List<User> expiredVips = userRepository.findByVipIsActiveTrueAndVipExpiryDateBefore(LocalDateTime.now());

        if (expiredVips.isEmpty()) {
            return;
        }

        logger.info("Found {} expired VIPs. Deactivating...", expiredVips.size());

        for (User user : expiredVips) {
            try {
                if (user.getVip() != null) {
                    user.getVip().setActive(false);
                    // Optionally: Set pointsMultiplier back to 1?
                    // user.getVip().setPointsMultiplier(1);
                    userRepository.save(user);
                    logger.info("Deactivated VIP for user: {}", user.getUserid());
                }
            } catch (Exception e) {
                logger.error("Failed to deactivate VIP for user {}: {}", user.getUserid(), e.getMessage());
            }
        }
    }
}

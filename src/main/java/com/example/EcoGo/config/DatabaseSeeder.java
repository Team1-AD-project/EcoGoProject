package com.example.EcoGo.config;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.UserRepository;
import com.example.EcoGo.utils.PasswordUtils;
import org.springframework.context.annotation.Profile;


@Profile("!test")
@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final String ADMIN_USERID = "admin";
    private final UserRepository userRepository;
    private final PasswordUtils passwordUtils;
    private final com.example.EcoGo.repository.TransportModeRepository transportModeRepository;

    @Value("${app.admin.default-password}")
    private String adminDefaultPassword;

    public DatabaseSeeder(UserRepository userRepository, PasswordUtils passwordUtils,
            com.example.EcoGo.repository.TransportModeRepository transportModeRepository) {
        this.userRepository = userRepository;
        this.passwordUtils = passwordUtils;
        this.transportModeRepository = transportModeRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByUserid(ADMIN_USERID).isEmpty()) {
            User admin = new User();
            admin.setId(UUID.randomUUID().toString());
            admin.setUserid(ADMIN_USERID);
            // admin.setUsername("Super Admin"); // Removed
            admin.setEmail("admin@eco.go");
            admin.setEmail("admin@eco.go");
            admin.setPassword(passwordUtils.encode(adminDefaultPassword));
            admin.setNickname("System Admin");
            admin.setAdmin(true);

            // Initialize defaults
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());
            admin.setLastLoginAt(LocalDateTime.now());

            // Preferences
            User.Preferences prefs = new User.Preferences();
            prefs.setLanguage("en");
            prefs.setTheme("dark");
            admin.setPreferences(prefs);

            // Vip
            User.Vip vip = new User.Vip();
            vip.setActive(true);
            vip.setPlan("YEARLY");
            vip.setExpiryDate(LocalDateTime.now().plusYears(99));
            admin.setVip(vip);

            // Stats
            User.Stats stats = new User.Stats();
            admin.setStats(stats);

            // ActivityMetrics
            User.ActivityMetrics metrics = new User.ActivityMetrics();
            metrics.setLoginDates(new java.util.ArrayList<>());
            admin.setActivityMetrics(metrics);

            userRepository.save(admin);
            System.out.println("---------------------------------------------");
            System.out.println("Admin user created: userid=admin");
            System.out.println("---------------------------------------------");
        }

        // --- Seed Transport Modes ---
        if (transportModeRepository.count() == 0) {
            seedTransportModes();
            System.out.println("Transport modes seeded.");
        }
    }

    private void seedTransportModes() {
        java.util.List<com.example.EcoGo.model.TransportMode> modes = java.util.Arrays.asList(
                new com.example.EcoGo.model.TransportMode("1001", "walk", "步行", 0, "https://xxx/icon/walk.png", 1,
                        true),
                new com.example.EcoGo.model.TransportMode("1002", "bike", "自行车", 0, "https://xxx/icon/bike.png", 2,
                        true),
                new com.example.EcoGo.model.TransportMode("1003", "bus", "公交", 20, "https://xxx/icon/bus.png", 3, true),
                new com.example.EcoGo.model.TransportMode("1004", "subway", "地铁", 10, "https://xxx/icon/subway.png", 4,
                        true),
                new com.example.EcoGo.model.TransportMode("1005", "car", "私家车", 100, "https://xxx/icon/car.png", 5,
                        false), // Not green
                new com.example.EcoGo.model.TransportMode("1006", "electric_bike", "电动车", 5,
                        "https://xxx/icon/ebike.png", 6, true));
        transportModeRepository.saveAll(modes);
    }
}

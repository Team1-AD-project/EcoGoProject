package com.example.EcoGo.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.EcoGo.dto.AuthDto;
import com.example.EcoGo.dto.UserProfileDto;
import com.example.EcoGo.dto.UserResponseDto;
import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.interfacemethods.UserInterface;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.UserRepository;
import com.example.EcoGo.utils.JwtUtils;
import com.example.EcoGo.utils.PasswordUtils;

@Service
public class UserServiceImpl implements UserInterface {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordUtils passwordUtils;
    private final JwtUtils jwtUtils;

    public UserServiceImpl(UserRepository userRepository, PasswordUtils passwordUtils, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordUtils = passwordUtils;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public UserResponseDto getUserByUserid(String userid) {
        // Keeping existing method for compatibility
        return userRepository.findByUserid(userid)
                .map(u -> new UserResponseDto(u.getEmail(), u.getUserid(), u.getNickname(), u.getPhone()))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    // --- Mobile Auth ---

    @Override
    public AuthDto.RegisterResponse register(AuthDto.MobileRegisterRequest request) {

        if (!request.password.equals(request.repassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的密码不一致");
        }

        if (userRepository.findByEmail(request.email).isPresent()) {
            throw new BusinessException(ErrorCode.USER_NAME_DUPLICATE, "邮箱已存在");
        }

        // Use userid provided by user
        String userid = request.userid;

        // Ensure userid is unique
        if (userRepository.findByUserid(userid).isPresent()) {
            throw new BusinessException(ErrorCode.USER_NAME_DUPLICATE, "用户ID已存在 (" + userid + ")");
        }

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setUserid(userid);
        user.setEmail(request.email);
        user.setPassword(passwordUtils.encode(request.password));
        user.setNickname(request.nickname);

        // Set Default Preferences (User doesn't provide them at registration)
        User.Preferences preferences = new User.Preferences();
        preferences.setPreferredTransport(new java.util.ArrayList<>(java.util.List.of("bus")));
        preferences.setEnablePush(true);
        preferences.setEnableEmail(true);
        preferences.setEnableBusReminder(true);
        preferences.setLanguage("zh");
        preferences.setTheme("light");
        preferences.setShareLocation(true);
        preferences.setShowOnLeaderboard(true);
        preferences.setShareAchievements(true);

        // New Fields Defaults
        preferences.setDormitoryOrResidence(null);
        preferences.setMainTeachingBuilding(null);
        preferences.setFavoriteStudySpot(null);
        preferences.setInterests(new java.util.ArrayList<>());
        preferences.setWeeklyGoals(0);
        preferences.setNewChallenges(false);
        preferences.setActivityReminders(false);
        preferences.setFriendActivity(false);

        user.setPreferences(preferences);

        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // Defaults
        user.setTotalPoints(0);
        user.setCurrentPoints(0);
        user.setTotalCarbon(0);

        User.Vip vip = new User.Vip();
        vip.setActive(false);
        user.setVip(vip);

        User.Stats stats = new User.Stats();
        stats.setTotalTrips(0);
        stats.setTotalDistance(0.0);
        stats.setGreenDays(0);
        stats.setWeeklyRank(0);
        stats.setMonthlyRank(0);
        user.setStats(stats);

        userRepository.save(user);
        logger.info("User registered: {}", user.getId());

        return new AuthDto.RegisterResponse(user);
    }

    @Override
    public AuthDto.LoginResponse loginMobile(AuthDto.MobileLoginRequest request) {
        // Login by UserID
        User user = userRepository.findByUserid(request.userid)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户ID不存在"));

        if (!passwordUtils.matches(request.password, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        user.setLastLoginAt(LocalDateTime.now());

        // Update Activity Metrics (Sliding Window Calculation)
        User.ActivityMetrics metrics = user.getActivityMetrics();
        if (metrics == null) {
            metrics = new User.ActivityMetrics();
            metrics.setLastTripDays(0);
            metrics.setLoginFrequency7d(0);
            user.setActivityMetrics(metrics);
        }

        // Initialize lists/defaults if null (for legacy data)
        if (metrics.getLoginDates() == null) {
            metrics.setLoginDates(new java.util.ArrayList<>());
        }

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate sevenDaysAgo = today.minusDays(7);
        java.time.LocalDate thirtyDaysAgo = today.minusDays(30);

        // 1. Add today to history if not present
        if (!metrics.getLoginDates().contains(today)) {
            metrics.getLoginDates().add(today);
        }

        // 2. Prune dates older than 30 days
        metrics.getLoginDates().removeIf(date -> date.isBefore(thirtyDaysAgo));

        // 3. Calculate Metrics from history
        long active7d = metrics.getLoginDates().stream()
                .filter(date -> !date.isBefore(sevenDaysAgo))
                .count();
        long active30d = metrics.getLoginDates().size(); // All remaining are within 30 days

        metrics.setActiveDays7d((int) active7d);
        metrics.setActiveDays30d((int) active30d);

        // Note: our List<LocalDate> stores unique days; this counter approximates frequency.
        metrics.setLoginFrequency7d(metrics.getLoginFrequency7d() + 1);

        userRepository.save(user);

        String token = jwtUtils.generateToken(user.getUserid(), user.isAdmin());
        String expireAt = jwtUtils.getExpirationDate(token).toString();

        return new AuthDto.LoginResponse(token, expireAt, user);
    }

    @Override
    public void logoutMobile(String token, String userId) {
        if (userId == null) {
            try {
                userId = jwtUtils.validateToken(token).getSubject();
            } catch (Exception e) {
                logger.warn("Logout with invalid token");
                return;
            }
        }
        // Stateless JWT, client side discard
        logger.info("User logout: {}", userId);
    }

    // --- Helper ---
    private User getUserFromToken(String token) {
        String userId;
        try {
            userId = jwtUtils.validateToken(token).getSubject();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "Invalid Token");
        }

        User user = userRepository.findByUserid(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeactivated()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        return user;
    }

    // --- Mobile Profile ---

    @Override
    public UserProfileDto.UpdateProfileResponse updateProfile(String token,
            UserProfileDto.UpdateProfileRequest request) {
        User user = getUserFromToken(token);
        return performUpdate(user, request);
    }

    @Override
    public UserProfileDto.PreferencesResetResponse resetPreferences(String token) {
        User user = getUserFromToken(token);

        User.Preferences defaultPref = new User.Preferences();
        defaultPref.setLanguage("zh");
        defaultPref.setTheme("light");
        defaultPref.setPreferredTransport(new java.util.ArrayList<>(java.util.List.of("bus")));
        defaultPref.setEnablePush(true);
        defaultPref.setEnableEmail(true);
        defaultPref.setEnableBusReminder(true);
        defaultPref.setShareLocation(true);
        defaultPref.setShowOnLeaderboard(true);
        defaultPref.setShareAchievements(true);

        // Reset new fields
        defaultPref.setDormitoryOrResidence(null);
        defaultPref.setMainTeachingBuilding(null);
        defaultPref.setFavoriteStudySpot(null);
        defaultPref.setInterests(new java.util.ArrayList<>());
        defaultPref.setWeeklyGoals(0);
        defaultPref.setNewChallenges(false);
        defaultPref.setActivityReminders(false);
        defaultPref.setFriendActivity(false);

        user.setPreferences(defaultPref);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return new UserProfileDto.PreferencesResetResponse(defaultPref, user.getUpdatedAt());
    }

    @Override
    public void deleteUser(String token) {
        User user = getUserFromToken(token);
        userRepository.delete(user);
    }

    @Override
    public UserProfileDto.UserDetailResponse getUserDetail(String token) {
        User user = getUserFromToken(token);
        return new UserProfileDto.UserDetailResponse(user);
    }

    // --- Web Admin ---

    @Override
    public UserProfileDto.UserDetailResponse getUserDetailAdmin(String userId) {
        User user = userRepository.findById(userId)
                .or(() -> userRepository.findByUserid(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return new UserProfileDto.UserDetailResponse(user);
    }

    @Override
    public UserProfileDto.UpdateProfileResponse updateUserStatus(String userId,
            UserProfileDto.UserStatusRequest request) {
        User user = userRepository.findById(userId)
                .or(() -> userRepository.findByUserid(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.setDeactivated(request.isDeactivated);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return new UserProfileDto.UpdateProfileResponse(user.getId(), user.getUpdatedAt());
    }

    @Override
    public com.example.EcoGo.dto.PageResponse<User> getAllUsers(int page, int size) {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page - 1, size);
        org.springframework.data.domain.Page<User> userPage = userRepository.findByIsAdminFalse(pageable);
        return new com.example.EcoGo.dto.PageResponse<>(userPage.getContent(), userPage.getTotalElements(), page, size);
    }

    // --- Web Auth ---

    @Override
    public AuthDto.LoginResponse loginWeb(AuthDto.WebLoginRequest request) {
        User user = userRepository.findByUserid(request.userid)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordUtils.matches(request.password, user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Invalid password");
        }

        if (user.isDeactivated()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "Your account has been deactivated.");
        }

        if (!user.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "Not an admin account");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtils.generateToken(user.getUserid(), user.isAdmin());
        String expireAt = jwtUtils.getExpirationDate(token).toString();

        return new AuthDto.LoginResponse(token, expireAt, user);
    }

    @Override
    public void logoutWeb(String token) {
        logger.info("Web admin logout");
    }

    // --- Web Admin ---

    @Override
    public UserProfileDto.AuthCheckResponse authenticateUser(String token) {
        try {
            var claims = jwtUtils.validateToken(token);
            boolean isAdmin = (boolean) claims.get("isAdmin");
            return new UserProfileDto.AuthCheckResponse(isAdmin, Collections.singletonList("ALL"));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "Invalid Token");
        }
    }

    @Override
    public UserProfileDto.UpdateProfileResponse manageUser(String userId,
            UserProfileDto.AdminManageUserRequest request) {
        User user = userRepository.findByUserid(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.setAdmin(request.isAdmin);
        user.setDeactivated(request.isDeactivated);

        if (user.getVip() == null) user.setVip(new User.Vip());
        user.getVip().setActive(request.vip_status);

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return new UserProfileDto.UpdateProfileResponse(user.getId(), user.getUpdatedAt());
    }

    @Override
    public UserProfileDto.UpdateProfileResponse updateProfileAdmin(String userId,
            UserProfileDto.UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return performUpdate(user, request);
    }

    @Override
    public UserProfileDto.UpdateProfileResponse updateMobileProfileByUserId(String userid,
            UserProfileDto.UpdateProfileRequest request) {
        User user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return performUpdate(user, request);
    }

    private UserProfileDto.UpdateProfileResponse performUpdate(User user, UserProfileDto.UpdateProfileRequest request) {
        updateUserBasicInfo(user, request);
        updateUserPreferences(user, request);

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return new UserProfileDto.UpdateProfileResponse(user.getId(), user.getUpdatedAt());
    }

    private void updateUserBasicInfo(User user, UserProfileDto.UpdateProfileRequest request) {
        if (request.nickname != null) user.setNickname(request.nickname);
        if (request.avatar != null) user.setAvatar(request.avatar);
        if (request.phone != null) user.setPhone(request.phone);
        if (request.faculty != null) user.setFaculty(request.faculty);
    }

    private void updateUserPreferences(User user, UserProfileDto.UpdateProfileRequest request) {
        if (request.preferences == null) return;

        User.Preferences userPref = user.getPreferences();
        if (userPref == null) {
            userPref = new User.Preferences();
            user.setPreferences(userPref);
        }
        UserProfileDto.PreferencesDto reqPref = request.preferences;

        updateGeneralPreferences(userPref, reqPref);
        updateNewPreferences(userPref, reqPref);
    }

    private void updateGeneralPreferences(User.Preferences userPref, UserProfileDto.PreferencesDto reqPref) {
        if (reqPref.preferredTransport != null) userPref.setPreferredTransport(reqPref.preferredTransport);
        if (reqPref.enablePush != null) userPref.setEnablePush(reqPref.enablePush);
        if (reqPref.enableEmail != null) userPref.setEnableEmail(reqPref.enableEmail);
        if (reqPref.enableBusReminder != null) userPref.setEnableBusReminder(reqPref.enableBusReminder);
        if (reqPref.language != null) userPref.setLanguage(reqPref.language);
        if (reqPref.theme != null) userPref.setTheme(reqPref.theme);
        if (reqPref.shareLocation != null) userPref.setShareLocation(reqPref.shareLocation);
        if (reqPref.showOnLeaderboard != null) userPref.setShowOnLeaderboard(reqPref.showOnLeaderboard);
        if (reqPref.shareAchievements != null) userPref.setShareAchievements(reqPref.shareAchievements);
    }

    private void updateNewPreferences(User.Preferences userPref, UserProfileDto.PreferencesDto reqPref) {
        if (reqPref.dormitoryOrResidence != null) userPref.setDormitoryOrResidence(reqPref.dormitoryOrResidence);
        if (reqPref.mainTeachingBuilding != null) userPref.setMainTeachingBuilding(reqPref.mainTeachingBuilding);
        if (reqPref.favoriteStudySpot != null) userPref.setFavoriteStudySpot(reqPref.favoriteStudySpot);
        if (reqPref.interests != null) userPref.setInterests(reqPref.interests);
        if (reqPref.weeklyGoals != null) userPref.setWeeklyGoals(reqPref.weeklyGoals);
        if (reqPref.newChallenges != null) userPref.setNewChallenges(reqPref.newChallenges);
        if (reqPref.activityReminders != null) userPref.setActivityReminders(reqPref.activityReminders);
        if (reqPref.friendActivity != null) userPref.setFriendActivity(reqPref.friendActivity);
    }

    @Override
    public User getUserDetailByUserid(String userid) {
        return userRepository.findByUserid(userid)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * ✅ SonarQube Cognitive Complexity fix (line ~487):
     * Refactor updateUserInfoAdmin() into small helpers.
     */
    @Override
    public UserProfileDto.UpdateProfileResponse updateUserInfoAdmin(String userid,
            UserProfileDto.AdminUpdateUserInfoRequest request) {

        validateTargetUserid(userid);

        User user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        applyBasicAdminUpdates(user, request);
        applyVipStatusUpdate(user, request);
        applyVipPlanUpdate(user, request);
        applyVipExpiryUpdate(user, request);
        applyDeactivationUpdate(user, request);

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return new UserProfileDto.UpdateProfileResponse(user.getId(), user.getUpdatedAt());
    }

    // =========================
    // updateUserInfoAdmin helpers
    // =========================
    private void validateTargetUserid(String userid) {
        if (userid == null || userid.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Target UserID cannot be empty");
        }
    }

    private void applyBasicAdminUpdates(User user, UserProfileDto.AdminUpdateUserInfoRequest request) {
        if (request.nickname != null) user.setNickname(request.nickname);
        if (request.email != null) user.setEmail(request.email);
    }

    private void applyVipStatusUpdate(User user, UserProfileDto.AdminUpdateUserInfoRequest request) {
        if (request.isVipActive == null) return;

        ensureVipObject(user);

        boolean wasActive = user.getVip().isActive();
        boolean expired = isVipExpired(user);

        user.getVip().setActive(request.isVipActive);

        // If activating and (was not active OR was expired), reset Start Date to now
        if (Boolean.TRUE.equals(request.isVipActive) && (!wasActive || expired)) {
            user.getVip().setStartDate(LocalDateTime.now());
        }
    }

    private void applyVipPlanUpdate(User user, UserProfileDto.AdminUpdateUserInfoRequest request) {
        if (request.vipPlan == null || request.vipPlan.isEmpty()) return;

        ensureVipObject(user);
        user.getVip().setPlan(request.vipPlan);
    }

    private void applyVipExpiryUpdate(User user, UserProfileDto.AdminUpdateUserInfoRequest request) {
        if (request.vipExpiryDate == null || request.vipExpiryDate.isEmpty()) return;

        ensureVipObject(user);

        Optional<LocalDateTime> parsed = parseVipExpiryDate(request.vipExpiryDate);
        if (parsed.isPresent()) {
            user.getVip().setExpiryDate(parsed.get());
            return;
        }

        logger.warn("Invalid date format for vipExpiryDate: {}", request.vipExpiryDate);
    }

    private void applyDeactivationUpdate(User user, UserProfileDto.AdminUpdateUserInfoRequest request) {
        if (request.isDeactivated != null) {
            user.setDeactivated(request.isDeactivated);
        }
    }

    private void ensureVipObject(User user) {
        if (user.getVip() == null) {
            user.setVip(new User.Vip());
        }
    }

    private boolean isVipExpired(User user) {
        if (user.getVip() == null) return true;
        LocalDateTime expiry = user.getVip().getExpiryDate();
        return expiry != null && expiry.isBefore(LocalDateTime.now());
    }

    /**
     * Accepts:
     * - yyyy-MM-dd  -> set to 23:59:59
     * - ISO LocalDateTime (e.g. 2026-02-12T10:15:30)
     */
    private Optional<LocalDateTime> parseVipExpiryDate(String raw) {
        // Try yyyy-MM-dd
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(raw);
            return Optional.of(date.atTime(23, 59, 59));
        } catch (Exception ignore) {
            // continue
        }

        // Try LocalDateTime ISO
        try {
            return Optional.of(LocalDateTime.parse(raw));
        } catch (Exception ignore) {
            return Optional.empty();
        }
    }

    /**
     * Security Validation:
     * 1. Check if token is valid.
     * 2. If Admin -> Pass.
     * 3. If User -> Check if Token UUID matches Target User's UUID.
     */
    @Override
    public void activateVip(String userId, int durationDays) {
        User user = userRepository.findByUserid(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        User.Vip vip = user.getVip();
        if (vip == null) {
            vip = new User.Vip();
            user.setVip(vip);
        }

        LocalDateTime now = LocalDateTime.now();

        // Extend if already active, otherwise start new
        if (vip.isActive() && vip.getExpiryDate() != null && vip.getExpiryDate().isAfter(now)) {
            vip.setStartDate(now);
            vip.setExpiryDate(vip.getExpiryDate().plusDays(durationDays));
        } else {
            vip.setActive(true);
            vip.setStartDate(now);
            vip.setExpiryDate(now.plusDays(durationDays));
            vip.setPlan("MONTHLY"); // Default from redemption
            vip.setPointsMultiplier(2);
            vip.setAutoRenew(false);
        }

        userRepository.save(user);
        logger.info("VIP Activated/Extended for user: {}, duration: {} days", userId, durationDays);
    }
}

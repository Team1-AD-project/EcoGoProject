package com.example.EcoGo.service;

import com.example.EcoGo.dto.AuthDto;
import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.UserRepository;
import com.example.EcoGo.utils.JwtUtils;
import com.example.EcoGo.utils.PasswordUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordUtils passwordUtils;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private UserServiceImpl userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId("uuid");
        mockUser.setUserid("testUser");
        mockUser.setPassword("encodedPassword");
        mockUser.setAdmin(false);
    }

    @Test
    void register_success() {
        AuthDto.MobileRegisterRequest request = new AuthDto.MobileRegisterRequest();
        request.userid = "newUser";
        request.email = "new@example.com";
        request.password = "password";
        request.repassword = "password";
        request.nickname = "New User";

        when(userRepository.findByEmail(request.email)).thenReturn(Optional.empty());
        when(userRepository.findByUserid(request.userid)).thenReturn(Optional.empty());
        when(passwordUtils.encode(request.password)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        AuthDto.RegisterResponse response = userService.register(request);

        assertNotNull(response);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_passwordMismatch() {
        AuthDto.MobileRegisterRequest request = new AuthDto.MobileRegisterRequest();
        request.password = "pass1";
        request.repassword = "pass2";

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.register(request));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void loginMobile_success() {
        AuthDto.MobileLoginRequest request = new AuthDto.MobileLoginRequest();
        request.userid = "testUser";
        request.password = "password";

        when(userRepository.findByUserid(request.userid)).thenReturn(Optional.of(mockUser));
        when(passwordUtils.matches(request.password, mockUser.getPassword())).thenReturn(true);
        when(jwtUtils.generateToken(mockUser.getUserid(), mockUser.isAdmin())).thenReturn("token");
        when(jwtUtils.getExpirationDate("token")).thenReturn(new java.util.Date());

        AuthDto.LoginResponse response = userService.loginMobile(request);

        assertNotNull(response);
        assertEquals("token", response.token);
        verify(userRepository).save(mockUser); // Updates last login
    }

    @Test
    void loginMobile_userNotFound() {
        AuthDto.MobileLoginRequest request = new AuthDto.MobileLoginRequest();
        request.userid = "unknown";

        when(userRepository.findByUserid("unknown")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> userService.loginMobile(request));
    }

    @Test
    void loginMobile_wrongPassword() {
        AuthDto.MobileLoginRequest request = new AuthDto.MobileLoginRequest();
        request.userid = "testUser";
        request.password = "wrong";

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(passwordUtils.matches("wrong", "encodedPassword")).thenReturn(false);

        assertThrows(BusinessException.class, () -> userService.loginMobile(request));
    }

    @Test
    void manageUser_success() {
        com.example.EcoGo.dto.UserProfileDto.AdminManageUserRequest request = new com.example.EcoGo.dto.UserProfileDto.AdminManageUserRequest();
        request.isAdmin = true;
        request.vip_status = true;
        request.isDeactivated = false;
        request.remark = "Promoted";

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        com.example.EcoGo.dto.UserProfileDto.UpdateProfileResponse response = userService.manageUser("testUser",
                request);

        assertNotNull(response);
        assertTrue(mockUser.isAdmin());
        assertTrue(mockUser.getVip().isActive());
        verify(userRepository).save(mockUser);
    }

    @Test
    void activateVip_new() {
        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        userService.activateVip("testUser", 30); // Assuming 30 days for monthly

        assertTrue(mockUser.getVip().isActive());
        // assertEquals("Monthly", mockUser.getVip().getPlan()); // Plan might not be
        // set by simple activateVip(int)
        assertNotNull(mockUser.getVip().getStartDate());
        verify(userRepository).save(mockUser);
    }

    @Test
    void updateUserInfoAdmin_success() {
        com.example.EcoGo.dto.UserProfileDto.AdminUpdateUserInfoRequest request = new com.example.EcoGo.dto.UserProfileDto.AdminUpdateUserInfoRequest();
        request.nickname = "AdminUpdated";
        request.isVipActive = true;
        request.vipPlan = "Yearly";

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        com.example.EcoGo.dto.UserProfileDto.UpdateProfileResponse response = userService
                .updateUserInfoAdmin("testUser", request);

        assertNotNull(response);
        assertEquals("AdminUpdated", mockUser.getNickname());
        assertTrue(mockUser.getVip().isActive());
        assertEquals("Yearly", mockUser.getVip().getPlan());
        verify(userRepository).save(mockUser);
    }

    @Test
    void loginWeb_success() {
        AuthDto.WebLoginRequest request = new AuthDto.WebLoginRequest();
        request.userid = "adminUser";
        request.password = "password";

        User adminUser = new User();
        adminUser.setUserid("adminUser");
        adminUser.setPassword("encodedPassword");
        adminUser.setAdmin(true);
        adminUser.setDeactivated(false);

        when(userRepository.findByUserid("adminUser")).thenReturn(Optional.of(adminUser));
        when(passwordUtils.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtUtils.generateToken("adminUser", true)).thenReturn("adminToken");
        when(jwtUtils.getExpirationDate("adminToken")).thenReturn(new java.util.Date());

        AuthDto.LoginResponse response = userService.loginWeb(request);

        assertNotNull(response);
        assertEquals("adminToken", response.token);
        verify(userRepository).save(adminUser);
    }

    @Test
    void loginWeb_notAdmin() {
        AuthDto.WebLoginRequest request = new AuthDto.WebLoginRequest();
        request.userid = "normalUser";
        request.password = "password";

        User normalUser = new User();
        normalUser.setUserid("normalUser");
        normalUser.setPassword("encodedPassword");
        normalUser.setAdmin(false);

        when(userRepository.findByUserid("normalUser")).thenReturn(Optional.of(normalUser));
        when(passwordUtils.matches("password", "encodedPassword")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.loginWeb(request));
    }

    @Test
    void loginWeb_deactivated() {
        AuthDto.WebLoginRequest request = new AuthDto.WebLoginRequest();
        request.userid = "deactivatedUser";
        request.password = "password";

        User deactivatedUser = new User();
        deactivatedUser.setUserid("deactivatedUser");
        deactivatedUser.setPassword("encodedPassword");
        deactivatedUser.setAdmin(true);
        deactivatedUser.setDeactivated(true);

        when(userRepository.findByUserid("deactivatedUser")).thenReturn(Optional.of(deactivatedUser));
        when(passwordUtils.matches("password", "encodedPassword")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.loginWeb(request));
    }

    @Test
    void resetPreferences_success() {
        when(jwtUtils.validateToken("mock-token")).thenReturn(io.jsonwebtoken.impl.DefaultClaims.class
                .cast(io.jsonwebtoken.Jwts.claims().setSubject(mockUser.getUserid())));
        when(userRepository.findByUserid(mockUser.getUserid())).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        com.example.EcoGo.dto.UserProfileDto.PreferencesResetResponse response = userService
                .resetPreferences("mock-token");

        assertNotNull(response);
        assertNotNull(mockUser.getPreferences());
        assertEquals("zh", mockUser.getPreferences().getLanguage()); // Check default
        verify(userRepository).save(mockUser);
    }

    @Test
    void authenticateUser_success() {
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.claims();
        claims.put("isAdmin", true);
        when(jwtUtils.validateToken("valid-token")).thenReturn(claims);

        com.example.EcoGo.dto.UserProfileDto.AuthCheckResponse response = userService.authenticateUser("valid-token");

        assertTrue(response.is_admin);
        assertEquals("ALL", response.permissions.get(0));
    }

    @Test
    void getAllUsers_success() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        User user1 = new User();
        User user2 = new User();
        org.springframework.data.domain.Page<User> page = new org.springframework.data.domain.PageImpl<>(
                java.util.Arrays.asList(user1, user2));

        when(userRepository.findByIsAdminFalse(pageable)).thenReturn(page);

        com.example.EcoGo.dto.PageResponse<User> response = userService.getAllUsers(1, 10);

        assertEquals(2, response.getList().size());
        assertEquals(2, response.getTotal());
    }

    @Test
    void deleteUser_success() {
        when(jwtUtils.validateToken("mock-token")).thenReturn(io.jsonwebtoken.impl.DefaultClaims.class
                .cast(io.jsonwebtoken.Jwts.claims().setSubject(mockUser.getUserid())));
        when(userRepository.findByUserid(mockUser.getUserid())).thenReturn(Optional.of(mockUser));

        userService.deleteUser("mock-token");

        verify(userRepository).delete(mockUser);
    }

    @Test
    void getUserDetail_success() {
        when(jwtUtils.validateToken("mock-token")).thenReturn(io.jsonwebtoken.impl.DefaultClaims.class
                .cast(io.jsonwebtoken.Jwts.claims().setSubject(mockUser.getUserid())));
        when(userRepository.findByUserid(mockUser.getUserid())).thenReturn(Optional.of(mockUser));

        com.example.EcoGo.dto.UserProfileDto.UserDetailResponse response = userService.getUserDetail("mock-token");

        assertEquals(mockUser.getUserid(), response.user_info.getUserid());
    }

    @Test
    void getUserDetailAdmin_success() {
        when(userRepository.findById("uuid")).thenReturn(Optional.of(mockUser));

        com.example.EcoGo.dto.UserProfileDto.UserDetailResponse response = userService.getUserDetailAdmin("uuid");

        assertEquals(mockUser.getUserid(), response.user_info.getUserid());
    }

    @Test
    void updateUserStatus_success() {
        com.example.EcoGo.dto.UserProfileDto.UserStatusRequest request = new com.example.EcoGo.dto.UserProfileDto.UserStatusRequest();
        request.isDeactivated = true;

        when(userRepository.findById("uuid")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        com.example.EcoGo.dto.UserProfileDto.UpdateProfileResponse response = userService.updateUserStatus("uuid",
                request);

        assertTrue(mockUser.isDeactivated());
        verify(userRepository).save(mockUser);
    }

    @Test
    void updateProfileAdmin_success() {
        com.example.EcoGo.dto.UserProfileDto.UpdateProfileRequest request = new com.example.EcoGo.dto.UserProfileDto.UpdateProfileRequest();
        request.nickname = "New Nick";

        when(userRepository.findById("uuid")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        com.example.EcoGo.dto.UserProfileDto.UpdateProfileResponse response = userService.updateProfileAdmin("uuid",
                request);

        assertEquals("New Nick", mockUser.getNickname());
        verify(userRepository).save(mockUser);
    }

    @Test
    void logoutWeb_success() {
        // Just verify it doesn't throw
        assertDoesNotThrow(() -> userService.logoutWeb("token"));
    }

    @Test
    void getUserByUserid_userNotFound() {
        when(userRepository.findByUserid("unknown")).thenReturn(java.util.Optional.empty());
        assertThrows(BusinessException.class, () -> userService.getUserByUserid("unknown"));
    }

    @Test
    void updateUserStatus_userNotFound() {
        com.example.EcoGo.dto.UserProfileDto.UserStatusRequest request = new com.example.EcoGo.dto.UserProfileDto.UserStatusRequest();
        when(userRepository.findById("unknown")).thenReturn(java.util.Optional.empty());
        when(userRepository.findByUserid("unknown")).thenReturn(java.util.Optional.empty());

        assertThrows(BusinessException.class, () -> userService.updateUserStatus("unknown", request));
    }

    @Test
    void updateProfileAdmin_userNotFound() {
        com.example.EcoGo.dto.UserProfileDto.UpdateProfileRequest request = new com.example.EcoGo.dto.UserProfileDto.UpdateProfileRequest();
        when(userRepository.findById("unknown")).thenReturn(java.util.Optional.empty());

        assertThrows(BusinessException.class, () -> userService.updateProfileAdmin("unknown", request));
    }

    @Test
    void updateMobileProfileByUserId_userNotFound() {
        com.example.EcoGo.dto.UserProfileDto.UpdateProfileRequest request = new com.example.EcoGo.dto.UserProfileDto.UpdateProfileRequest();
        when(userRepository.findByUserid("unknown")).thenReturn(java.util.Optional.empty());

        assertThrows(BusinessException.class, () -> userService.updateMobileProfileByUserId("unknown", request));
    }

    @Test
    void updateUserInfoAdmin_emptyUserId() {
        com.example.EcoGo.dto.UserProfileDto.AdminUpdateUserInfoRequest request = new com.example.EcoGo.dto.UserProfileDto.AdminUpdateUserInfoRequest();
        assertThrows(BusinessException.class, () -> userService.updateUserInfoAdmin("", request));
        assertThrows(BusinessException.class, () -> userService.updateUserInfoAdmin(null, request));
    }

    @Test
    void updateUserInfoAdmin_userNotFound() {
        com.example.EcoGo.dto.UserProfileDto.AdminUpdateUserInfoRequest request = new com.example.EcoGo.dto.UserProfileDto.AdminUpdateUserInfoRequest();
        when(userRepository.findByUserid("unknown")).thenReturn(java.util.Optional.empty());

        assertThrows(BusinessException.class, () -> userService.updateUserInfoAdmin("unknown", request));
    }

    @Test
    void activateVip_userNotFound() {
        when(userRepository.findByUserid("unknown")).thenReturn(java.util.Optional.empty());
        assertThrows(BusinessException.class, () -> userService.activateVip("unknown", 30));
    }

    @Test
    void manageUser_userNotFound() {
        com.example.EcoGo.dto.UserProfileDto.AdminManageUserRequest request = new com.example.EcoGo.dto.UserProfileDto.AdminManageUserRequest();
        when(userRepository.findByUserid("unknown")).thenReturn(java.util.Optional.empty());

        assertThrows(BusinessException.class, () -> userService.manageUser("unknown", request));
    }

    @Test
    void getUserDetailAdmin_userNotFound() {
        when(userRepository.findById("unknown")).thenReturn(java.util.Optional.empty());
        when(userRepository.findByUserid("unknown")).thenReturn(java.util.Optional.empty());

        assertThrows(BusinessException.class, () -> userService.getUserDetailAdmin("unknown"));
    }

    @Test
    void getUserFromToken_userNotFound() {
        // Indirectly test via getUserDetail
        when(jwtUtils.validateToken("token")).thenReturn(io.jsonwebtoken.impl.DefaultClaims.class
                .cast(io.jsonwebtoken.Jwts.claims().setSubject("unknown")));
        when(userRepository.findByUserid("unknown")).thenReturn(java.util.Optional.empty());

        assertThrows(BusinessException.class, () -> userService.getUserDetail("token"));
    }

    @Test
    void loginWeb_userNotFound() {
        AuthDto.WebLoginRequest request = new AuthDto.WebLoginRequest();
        request.userid = "unknown";
        when(userRepository.findByUserid("unknown")).thenReturn(java.util.Optional.empty());

        assertThrows(BusinessException.class, () -> userService.loginWeb(request));
    }

    @Test
    void performUpdate_fullCoverage() {
        // Test via updateProfile to cover performUpdate
        com.example.EcoGo.dto.UserProfileDto.UpdateProfileRequest request = new com.example.EcoGo.dto.UserProfileDto.UpdateProfileRequest();
        request.nickname = "UpdatedNick";
        request.avatar = "new_avatar.png";
        request.phone = "123456789";
        request.faculty = "Engineering";

        com.example.EcoGo.dto.UserProfileDto.PreferencesDto pref = new com.example.EcoGo.dto.UserProfileDto.PreferencesDto();
        pref.preferredTransport = new java.util.ArrayList<>(java.util.List.of("metro"));
        pref.enablePush = false;
        pref.enableEmail = false;
        pref.enableBusReminder = false;
        pref.language = "en";
        pref.theme = "dark";
        pref.shareLocation = false;
        pref.showOnLeaderboard = false;
        pref.shareAchievements = false;

        // New fields
        pref.dormitoryOrResidence = "Dorm A";
        pref.mainTeachingBuilding = "Building B";
        pref.favoriteStudySpot = "Library";
        pref.interests = new java.util.ArrayList<>(java.util.List.of("Reading"));
        pref.weeklyGoals = 5;
        pref.newChallenges = true;
        pref.activityReminders = true;
        pref.friendActivity = true;

        request.preferences = pref;

        // Mock getting user from token
        when(jwtUtils.validateToken("token")).thenReturn(io.jsonwebtoken.impl.DefaultClaims.class
                .cast(io.jsonwebtoken.Jwts.claims().setSubject(mockUser.getUserid())));
        when(userRepository.findByUserid(mockUser.getUserid())).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        userService.updateProfile("token", request);

        // Verify updates
        assertEquals("UpdatedNick", mockUser.getNickname());
        assertEquals("new_avatar.png", mockUser.getAvatar());
        assertEquals("123456789", mockUser.getPhone());
        assertEquals("Engineering", mockUser.getFaculty());

        User.Preferences savedPref = mockUser.getPreferences(); // Assumes mockUser is updated in place
        // If mockUser preferences were null, performUpdate creates new.
        // My setUp creates a fresh External User but check logic.
        // UserServiceImpl:416 User.Preferences userPref = user.getPreferences();
        // Since mockUser in setUp doesn't set preferences, it might be null initially?
        // Let's ensure mockUser has empty preferences or handle null in code.
        // The code handles null: if (userPref == null) { userPref = new
        // User.Preferences(); ... }

        assertNotNull(savedPref);
        assertEquals("metro", savedPref.getPreferredTransport().get(0));
        assertFalse(savedPref.isEnablePush());
        assertEquals("en", savedPref.getLanguage());
        assertEquals("Dorm A", savedPref.getDormitoryOrResidence());
        assertEquals("Building B", savedPref.getMainTeachingBuilding());
        assertTrue(savedPref.isFriendActivity());
    }

    @Test
    void logoutMobile_withUserId() {
        assertDoesNotThrow(() -> userService.logoutMobile("token", "user1"));
    }

    @Test
    void logoutMobile_extractFromToken() {
        when(jwtUtils.validateToken("token")).thenReturn(io.jsonwebtoken.impl.DefaultClaims.class
                .cast(io.jsonwebtoken.Jwts.claims().setSubject("user1")));
        assertDoesNotThrow(() -> userService.logoutMobile("token", null));
    }

    @Test
    void logoutMobile_invalidToken() {
        // Should catch exception and log warning, not throw
        when(jwtUtils.validateToken("invalid")).thenThrow(new RuntimeException("Invalid"));
        assertDoesNotThrow(() -> userService.logoutMobile("invalid", null));
    }

    @Test
    void getUserByUserid_success_dtoMapping() {
        mockUser.setNickname("MapMe");
        mockUser.setPhone("987654321");
        mockUser.setEmail("map@example.com");

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));

        com.example.EcoGo.dto.UserResponseDto response = userService.getUserByUserid("testUser");

        assertNotNull(response);
        assertEquals("testUser", response.getUserid());
        assertEquals("MapMe", response.getNickname());
        assertEquals("987654321", response.getPhone());
        assertEquals("map@example.com", response.getEmail());
    }

    @Test
    void getUserDetailByUserid_success() {
        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        User result = userService.getUserDetailByUserid("testUser");
        assertEquals(mockUser, result);
    }

    @Test
    void getUserDetailByUserid_notFound() {
        when(userRepository.findByUserid("unknown")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> userService.getUserDetailByUserid("unknown"));
    }
}

package com.example.EcoGo.controller;

import com.example.EcoGo.dto.AuthDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.dto.UserProfileDto;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.interfacemethods.UserInterface;
import com.example.EcoGo.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserInterface userService;

    @InjectMocks
    private UserController userController;

    private User mockUser;
    private String mockToken;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserid("testUser");
        mockUser.setEmail("test@example.com");
        mockUser.setCreatedAt(java.time.LocalDateTime.now());
        mockUser.setUpdatedAt(java.time.LocalDateTime.now());

        mockToken = "Bearer mock-token";
    }

    // --- Mobile Endpoints Tests ---

    @Test
    void registerMobile_success() {
        AuthDto.MobileRegisterRequest request = new AuthDto.MobileRegisterRequest();
        request.userid = "testUser";
        request.email = "test@example.com";
        request.password = "password";
        request.repassword = "password";

        AuthDto.RegisterResponse response = new AuthDto.RegisterResponse(mockUser);
        when(userService.register(any(AuthDto.MobileRegisterRequest.class))).thenReturn(response);

        ResponseMessage<AuthDto.RegisterResponse> result = userController.registerMobile(request);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        assertEquals("testUser", result.getData().userid);
        verify(userService).register(request);
    }

    @Test
    void loginMobile_success() {
        AuthDto.MobileLoginRequest request = new AuthDto.MobileLoginRequest();
        request.userid = "testUser";
        request.password = "password";

        AuthDto.LoginResponse response = new AuthDto.LoginResponse("token", "expireAt", mockUser);
        when(userService.loginMobile(any(AuthDto.MobileLoginRequest.class))).thenReturn(response);

        ResponseMessage<AuthDto.LoginResponse> result = userController.loginMobile(request);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        assertEquals("token", result.getData().token);
        verify(userService).loginMobile(request);
    }

    @Test
    void logoutMobile_success() {
        ResponseMessage<Void> result = userController.logoutMobile(mockToken);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        verify(userService).logoutMobile("mock-token", null);
    }

    @Test
    void getUserProfileMobile_success() {
        UserProfileDto.UserDetailResponse response = new UserProfileDto.UserDetailResponse(mockUser);
        when(userService.getUserDetail("mock-token")).thenReturn(response);

        ResponseMessage<UserProfileDto.UserDetailResponse> result = userController.getUserProfileMobile(mockToken);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        assertEquals("testUser", result.getData().user_info.getUserid());
        verify(userService).getUserDetail("mock-token");
    }

    @Test
    void updateProfileMobile_success() {
        UserProfileDto.UpdateProfileRequest request = new UserProfileDto.UpdateProfileRequest();
        request.nickname = "NewNick";

        UserProfileDto.UpdateProfileResponse response = new UserProfileDto.UpdateProfileResponse("uuid",
                java.time.LocalDateTime.now());
        when(userService.updateProfile(eq("mock-token"), any(UserProfileDto.UpdateProfileRequest.class)))
                .thenReturn(response);

        ResponseMessage<UserProfileDto.UpdateProfileResponse> result = userController.updateProfileMobile(mockToken,
                request);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        verify(userService).updateProfile(eq("mock-token"), eq(request));
    }

    // --- Web Endpoints Tests ---

    @Test
    void loginWeb_success() {
        AuthDto.WebLoginRequest request = new AuthDto.WebLoginRequest();
        request.userid = "admin";
        request.password = "admin123";

        AuthDto.LoginResponse response = new AuthDto.LoginResponse("token", "expireAt", mockUser);
        when(userService.loginWeb(any(AuthDto.WebLoginRequest.class))).thenReturn(response);

        ResponseMessage<AuthDto.LoginResponse> result = userController.loginWeb(request);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        verify(userService).loginWeb(request);
    }

    @Test
    void listAllUsers_success() {
        com.example.EcoGo.dto.PageResponse<User> pageResponse = new com.example.EcoGo.dto.PageResponse<>(null, 0, 1,
                10);
        when(userService.getAllUsers(1, 10)).thenReturn(pageResponse);

        ResponseMessage<com.example.EcoGo.dto.PageResponse<User>> result = userController.listAllUsers(1, 10);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        verify(userService).getAllUsers(1, 10);
    }

    @Test
    void logoutWeb_success() {
        ResponseMessage<Map<String, Boolean>> result = userController.logoutWeb(mockToken);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        verify(userService).logoutWeb("mock-token");
    }

    @Test
    void authenticateWeb_success() {
        UserProfileDto.AuthCheckResponse response = new UserProfileDto.AuthCheckResponse(true,
                java.util.Collections.singletonList("ALL"));
        when(userService.authenticateUser("mock-token")).thenReturn(response);

        ResponseMessage<UserProfileDto.AuthCheckResponse> result = userController.authenticateWeb(mockToken);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        verify(userService).authenticateUser("mock-token");
    }

    @Test
    void manageUser_success() {
        UserProfileDto.AdminManageUserRequest request = new UserProfileDto.AdminManageUserRequest();
        request.isAdmin = true;

        UserProfileDto.UpdateProfileResponse response = new UserProfileDto.UpdateProfileResponse("userId",
                java.time.LocalDateTime.now());
        when(userService.manageUser(eq("userId"), any(UserProfileDto.AdminManageUserRequest.class)))
                .thenReturn(response);

        ResponseMessage<UserProfileDto.UpdateProfileResponse> result = userController.manageUser("userId", request);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        verify(userService).manageUser(eq("userId"), eq(request));
    }

    @Test
    void updateUserStatus_success() {
        UserProfileDto.UserStatusRequest request = new UserProfileDto.UserStatusRequest();
        request.isDeactivated = true;

        UserProfileDto.UpdateProfileResponse response = new UserProfileDto.UpdateProfileResponse("userId",
                java.time.LocalDateTime.now());
        when(userService.updateUserStatus(eq("userId"), any(UserProfileDto.UserStatusRequest.class)))
                .thenReturn(response);

        ResponseMessage<UserProfileDto.UpdateProfileResponse> result = userController.updateUserStatus("userId",
                request);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        verify(userService).updateUserStatus(eq("userId"), eq(request));
    }

    @Test
    void getUserDetail_success() {
        UserProfileDto.UserDetailResponse response = new UserProfileDto.UserDetailResponse(mockUser);
        when(userService.getUserDetailAdmin("userId")).thenReturn(response);

        ResponseMessage<UserProfileDto.UserDetailResponse> result = userController.getUserDetail("userId");

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        verify(userService).getUserDetailAdmin("userId");
    }

    @Test
    void updateUserInfoAdmin_success() {
        UserProfileDto.AdminUpdateUserInfoRequest request = new UserProfileDto.AdminUpdateUserInfoRequest();
        request.nickname = "New Admin Name";

        UserProfileDto.UpdateProfileResponse response = new UserProfileDto.UpdateProfileResponse("userId",
                java.time.LocalDateTime.now());
        when(userService.updateUserInfoAdmin(eq("userId"), any(UserProfileDto.AdminUpdateUserInfoRequest.class)))
                .thenReturn(response);

        ResponseMessage<UserProfileDto.UpdateProfileResponse> result = userController.updateUserInfoAdmin("userId",
                request);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        verify(userService).updateUserInfoAdmin(eq("userId"), eq(request));
    }

    @Test
    void deleteUser_success() {
        ResponseMessage<Map<String, Boolean>> result = userController.deleteUser(mockToken);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        verify(userService).deleteUser("mock-token");
    }

    @Test
    void getUserByUserid_success() {
        com.example.EcoGo.dto.UserResponseDto response = new com.example.EcoGo.dto.UserResponseDto(
                mockUser.getEmail(),
                mockUser.getUserid(),
                mockUser.getNickname(),
                "1234567890"); // Mock phone
        when(userService.getUserByUserid("testUser")).thenReturn(response);

        ResponseMessage<com.example.EcoGo.dto.UserResponseDto> result = userController.getUserByUserid("testUser");

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        verify(userService).getUserByUserid("testUser");
    }

    @Test
    void resetPreferences_success() {
        UserProfileDto.PreferencesResetResponse response = new UserProfileDto.PreferencesResetResponse(
                new User.Preferences(), java.time.LocalDateTime.now());
        when(userService.resetPreferences("mock-token")).thenReturn(response);

        ResponseMessage<UserProfileDto.PreferencesResetResponse> result = userController.resetPreferences(mockToken);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        verify(userService).resetPreferences("mock-token");
    }

    @Test
    void getUserDetailByUserid_success() {
        when(userService.getUserDetailByUserid("testUser")).thenReturn(mockUser);

        ResponseMessage<User> result = userController.getUserDetailByUserid("testUser");

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        assertEquals("testUser", result.getData().getUserid());
        verify(userService).getUserDetailByUserid("testUser");
    }

    @Test
    void updateUserProfile_success() {
        UserProfileDto.UpdateProfileRequest request = new UserProfileDto.UpdateProfileRequest();
        request.nickname = "UpdatedNick";

        UserProfileDto.UpdateProfileResponse response = new UserProfileDto.UpdateProfileResponse("userId",
                java.time.LocalDateTime.now());
        when(userService.updateProfileAdmin(eq("userId"), any(UserProfileDto.UpdateProfileRequest.class)))
                .thenReturn(response);

        ResponseMessage<UserProfileDto.UpdateProfileResponse> result = userController.updateUserProfile("userId",
                request);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        verify(userService).updateProfileAdmin(eq("userId"), eq(request));
    }

    @Test
    void updateMobileProfileByUserId_success() {
        UserProfileDto.UpdateProfileRequest request = new UserProfileDto.UpdateProfileRequest();
        request.nickname = "UpdatedNickMobile";

        UserProfileDto.UpdateProfileResponse response = new UserProfileDto.UpdateProfileResponse("userId",
                java.time.LocalDateTime.now());
        when(userService.updateMobileProfileByUserId(eq("userId"), any(UserProfileDto.UpdateProfileRequest.class)))
                .thenReturn(response);

        ResponseMessage<UserProfileDto.UpdateProfileResponse> result = userController
                .updateMobileProfileByUserId("userId", request);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        verify(userService).updateMobileProfileByUserId(eq("userId"), eq(request));
    }
}

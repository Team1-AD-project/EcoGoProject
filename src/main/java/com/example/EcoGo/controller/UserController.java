package com.example.EcoGo.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.EcoGo.dto.AuthDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.dto.UserProfileDto;
import com.example.EcoGo.dto.UserResponseDto;
import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.interfacemethods.UserInterface;

/**
 * 用户接口控制器
 * 路径规范：/api/v1/user/xxx（v1表示版本，便于后续迭代）
 */
@RestController
// @RequestMapping("/api/v1") // We will use full paths for clarity as per
// design
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserInterface userService;

    public UserController(UserInterface userService) {
        this.userService = userService;
    }

    // --- Mobile Endpoints ---

    @PostMapping("/api/v1/mobile/users/register")
    public ResponseMessage<AuthDto.RegisterResponse> registerMobile(
            @RequestBody @Validated AuthDto.MobileRegisterRequest request) {
        return ResponseMessage.success(userService.register(request));
    }

    @PostMapping("/api/v1/mobile/users/login")
    public ResponseMessage<AuthDto.LoginResponse> loginMobile(
            @RequestBody @Validated AuthDto.MobileLoginRequest request) {
        return ResponseMessage.success(userService.loginMobile(request));
    }

    @PostMapping("/api/v1/mobile/users/logout")
    public ResponseMessage<Void> logoutMobile(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        // Ideally we extract UserID from token here or in service
        // For keeping the service interface clean, let's pass token.
        // Service implementation will validate and extract if needed.
        userService.logoutMobile(token, null); // userId is optional or extracted in service
        return ResponseMessage.success(null);
    }

    @GetMapping("/api/v1/mobile/users/profile")
    public ResponseMessage<UserProfileDto.UserDetailResponse> getUserProfileMobile(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return ResponseMessage.success(userService.getUserDetail(token));
    }

    @PutMapping("/api/v1/mobile/users/profile")
    public ResponseMessage<UserProfileDto.UpdateProfileResponse> updateProfileMobile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UserProfileDto.UpdateProfileRequest request) {
        String token = authHeader.replace("Bearer ", "");
        return ResponseMessage.success(userService.updateProfile(token, request));
    }

    @PutMapping("/api/v1/mobile/users/preferences/reset")
    public ResponseMessage<UserProfileDto.PreferencesResetResponse> resetPreferences(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return ResponseMessage.success(userService.resetPreferences(token));
    }

    @DeleteMapping("/api/v1/mobile/users")
    public ResponseMessage<Map<String, Boolean>> deleteUser(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        userService.deleteUser(token);
        return ResponseMessage.success(Map.of("success", true));
    }

    // --- Web Endpoints ---

    @PostMapping("/api/v1/web/users/login")
    public ResponseMessage<AuthDto.LoginResponse> loginWeb(@RequestBody @Validated AuthDto.WebLoginRequest request) {
        return ResponseMessage.success(userService.loginWeb(request));
    }

    @PostMapping("/api/v1/web/users/logout")
    public ResponseMessage<Map<String, Boolean>> logoutWeb(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        userService.logoutWeb(token);
        return ResponseMessage.success(Map.of("success", true));
    }

    @GetMapping("/api/v1/web/users/auth")
    public ResponseMessage<UserProfileDto.AuthCheckResponse> authenticateWeb(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return ResponseMessage.success(userService.authenticateUser(token));
    }

    @GetMapping("/api/v1/web/users/list")
    public ResponseMessage<java.util.List<UserResponseDto>> listAllUsers() {
        return ResponseMessage.success(userService.getAllUsers());
    }

    @PutMapping("/api/v1/web/users/manage/{userId}")
    public ResponseMessage<UserProfileDto.UpdateProfileResponse> manageUser(@PathVariable String userId,
            @RequestBody UserProfileDto.AdminManageUserRequest request) {
        return ResponseMessage.success(userService.manageUser(userId, request));
    }

    @PutMapping("/api/v1/web/users/status/{userId}")
    public ResponseMessage<UserProfileDto.UpdateProfileResponse> updateUserStatus(@PathVariable String userId,
            @RequestBody UserProfileDto.UserStatusRequest request) {
        return ResponseMessage.success(userService.updateUserStatus(userId, request));
    }

    @GetMapping("/api/v1/web/users/profile/{userId}")
    public ResponseMessage<UserProfileDto.UserDetailResponse> getUserDetail(
            @PathVariable String userId) {
        // Token validation is handled by JwtAuthenticationFilter
        return ResponseMessage.success(userService.getUserDetailAdmin(userId));
    }

    @PutMapping("/api/v1/web/users/profile/{userId}")
    public ResponseMessage<UserProfileDto.UpdateProfileResponse> updateUserProfile(@PathVariable String userId,
            @RequestBody UserProfileDto.UpdateProfileRequest request) {
        // userId from path is used directly in service
        return ResponseMessage.success(userService.updateProfileAdmin(userId, request));
    }

    // --- Legacy / Compatibility ---

    /**
     * 根据用户名查询用户信息
     */

    @GetMapping("/api/v1/user/{userid}")
    public ResponseMessage<UserResponseDto> getUserByUserid(@PathVariable("userid") String userid) {
        if (userid == null || userid.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "User ID cannot be empty");
        }
        UserResponseDto userDTO = userService.getUserByUserid(userid);
        return ResponseMessage.success(userDTO);
    }
}
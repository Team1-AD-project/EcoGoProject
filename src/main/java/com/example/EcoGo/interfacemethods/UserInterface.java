package com.example.EcoGo.interfacemethods;

import com.example.EcoGo.dto.AuthDto;
import com.example.EcoGo.dto.UserProfileDto;
import com.example.EcoGo.dto.UserResponseDto;

public interface UserInterface {

    // Existing method
    // Existing method
    UserResponseDto getUserByUserid(String userid);

    // Mobile Auth
    AuthDto.RegisterResponse register(AuthDto.MobileRegisterRequest request);

    AuthDto.LoginResponse loginMobile(AuthDto.MobileLoginRequest request);

    void logoutMobile(String token, String userId); // Legacy or optional userId

    // Mobile Profile - Refactored to Token-based
    UserProfileDto.UpdateProfileResponse updateProfile(String token, UserProfileDto.UpdateProfileRequest request);

    UserProfileDto.PreferencesResetResponse resetPreferences(String token);

    void deleteUser(String token);

    UserProfileDto.UserDetailResponse getUserDetail(String token); // Mobile profile via token checking

    // Web Auth
    AuthDto.LoginResponse loginWeb(AuthDto.WebLoginRequest request);

    void logoutWeb(String token);

    // Web Admin
    UserProfileDto.AuthCheckResponse authenticateUser(String token);

    java.util.List<UserResponseDto> getAllUsers();

    UserProfileDto.UpdateProfileResponse manageUser(String userId, UserProfileDto.AdminManageUserRequest request);

    // New: Dedicated Status Toggle
    UserProfileDto.UpdateProfileResponse updateUserStatus(String userId, UserProfileDto.UserStatusRequest request);

    UserProfileDto.UpdateProfileResponse updateProfileAdmin(String userId, UserProfileDto.UpdateProfileRequest request);

    UserProfileDto.UserDetailResponse getUserDetailAdmin(String userId); // For Admin viewing users
}

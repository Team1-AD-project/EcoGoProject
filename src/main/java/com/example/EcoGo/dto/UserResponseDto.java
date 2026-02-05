package com.example.EcoGo.dto;

public class UserResponseDto {
    private String email;
    private String userid;
    private String nickname;
    private String phone;

    // No-argument constructor
    public UserResponseDto() {
    }

    // Full constructor (optional, but good practice)
    public UserResponseDto(String email, String userid, String nickname, String phone) {
        this.email = email;
        this.userid = userid;
        this.nickname = nickname;
        this.phone = phone;
    }

    // Getters and Setters
    public UserResponseDto(String userid, String nickname, String phone) {
        this.userid = userid;
        this.nickname = nickname;
        this.phone = phone;
    }

    // 保留队友的 Email 方法
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}

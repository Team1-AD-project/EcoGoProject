package com.example.EcoGo.dto;

public class UserResponseDto {
    private String userid;
    private String nickname;
    private String phone;

    public UserResponseDto(String userid, String nickname, String phone) {
        this.userid = userid;
        this.nickname = nickname;
        this.phone = phone;
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

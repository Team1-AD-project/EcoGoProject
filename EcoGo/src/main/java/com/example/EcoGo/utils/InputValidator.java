package com.example.EcoGo.utils;

import org.springframework.stereotype.Component;

@Component
public class InputValidator {

    /**
     * 验证邮箱格式
     */
    public static boolean validateEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return email.contains("@") && email.contains(".");
    }

    /**
     * 验证用户年龄
     */
    public static boolean validateAge(int age) {
        return age >= 0 && age <= 150;
    }
}

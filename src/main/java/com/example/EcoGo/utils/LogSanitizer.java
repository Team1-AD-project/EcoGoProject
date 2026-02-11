package com.example.EcoGo.utils;

/**
 * Utility to sanitize user-controlled input before logging,
 * preventing log injection attacks (SonarQube S5145).
 */
public final class LogSanitizer {

    private LogSanitizer() {
    }

    /**
     * Strips newline, carriage-return and tab characters from the input
     * so that a malicious user cannot forge extra log lines.
     */
    public static String sanitize(String value) {
        if (value == null) {
            return "null";
        }
        return value.replaceAll("[\\n\\r\\t]", "_");
    }
}

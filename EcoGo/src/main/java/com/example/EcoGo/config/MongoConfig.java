package com.example.EcoGo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new StringToLocalDateTimeConverter());
        return new MongoCustomConversions(converters);
    }

    static class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public LocalDateTime convert(@NonNull String source) {
            // Try standard ISO format first (just in case)
            try {
                return LocalDateTime.parse(source);
            } catch (Exception e) {
                // If failed, try "yyyy-MM-dd HH:mm:ss"
                try {
                    return LocalDateTime.parse(source, FORMATTER);
                } catch (Exception ex) {
                    // Start of day fallback
                    try {
                        java.time.LocalDate date = java.time.LocalDate.parse(source);
                        return date.atStartOfDay();
                    } catch (Exception ignore) {
                        // Give up
                        throw new IllegalArgumentException("Cannot parse date: " + source);
                    }
                }
            }
        }
    }
}

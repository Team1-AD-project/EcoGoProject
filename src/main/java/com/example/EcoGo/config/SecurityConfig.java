package com.example.EcoGo.config;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        http
                // Disable CSRF (since we use JWT and don't need session-based CSRF protection)
                .csrf(AbstractHttpConfigurer::disable)

                // Disable default login forms (we use our own API endpoints)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // Authorize Requests
                .authorizeHttpRequests(auth -> auth
                        // Public Endpoints
                        .requestMatchers("/api/v1/mobile/users/login", "/api/v1/mobile/users/register").permitAll()
                        .requestMatchers("/api/v1/web/users/login").permitAll()

                        // Secured Endpoints
                        .requestMatchers("/api/v1/mobile/**").authenticated()
                        .requestMatchers("/api/v1/web/**").authenticated() // Suggest adding .hasRole("ADMIN") later if
                                                                           // needed

                        // Default
                        .anyRequest().permitAll() // Keep other paths open or strict as needed
                )

                // Add JWT Filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

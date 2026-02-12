package com.example.EcoGo.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {

    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    // Token validity period: 7 days
    private static final long EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000L;

    /**
     * Generate Token
     * 
     * @param userId  User ID
     * @param isAdmin Whether it is an administrator
     * @return Token string
     */
    public String generateToken(String userId, boolean isAdmin) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("isAdmin", isAdmin);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * 验证 Token 并获取 Claims
     * 
     * @param token token字符串
     * @return Claims 对象 (如果验证失败则抛出异常)
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("Invalid JWT token");
        }
    }

    /**
     * Parse Token to get User ID (Subject)
     */
    public String getUserIdFromToken(String token) {
        return validateToken(token).getSubject();
    }

    /**
     * 获取过期时间
     */
    public Date getExpirationDate(String token) {
        return validateToken(token).getExpiration();
    }
}

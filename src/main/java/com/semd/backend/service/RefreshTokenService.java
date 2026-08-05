package com.semd.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {
    private final StringRedisTemplate redisTemplate;

    public RefreshTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String sha256(String raw) {
        try{
            MessageDigest mDigest = MessageDigest.getInstance("SHA-256");
            byte[] out = mDigest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String key(String username, String refresToken) {
        return "refresh:" + username + ":" + sha256(refresToken);
    }

    public void storeRefreshToken(String username, String refreshToken, long ttlSeconds){
        redisTemplate.opsForValue().set(key(username, refreshToken), "1", Duration.ofSeconds(ttlSeconds));
    }

    public boolean exists(String username, String refreshToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(username, refreshToken)));
    }

    public void revoke(String username, String refreshToken) {
        redisTemplate.delete(key(username, refreshToken));
    }
}

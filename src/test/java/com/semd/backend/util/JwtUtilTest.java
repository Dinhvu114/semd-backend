package com.semd.backend.util;

import com.semd.backend.entity.User;
import com.semd.backend.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private User testUser;

    private static final String SECRET = "your-256-bit-secret-key-here-change-in-production-THAT-IS-EXTREMELY-L0NG-AND-EASY-TO-REMEMBER-WHATSOVEVER";
    private static final long ACCESS_EXPIRATION = 900000; // 15 mins in millis
    private static final long REFRESH_EXPIRATION = 604800000; // 7 days in millis

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION);

        testUser = new User();
        testUser.setId(123);
        testUser.setUsername("testuser");
        testUser.setRoles(Set.of(new Role("ROLE_USER")));
    }

    @Test
    void testGetTtlSeconds() {
        assertEquals(ACCESS_EXPIRATION / 1000, jwtUtil.getAccessTtlSeconds());
        assertEquals(REFRESH_EXPIRATION / 1000, jwtUtil.getRefreshTtlSeconds());
    }

    @Test
    void testGenerateAndExtractAccessToken() {
        String token = jwtUtil.generateAccessToken(testUser);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        assertEquals("testuser", jwtUtil.extractUsername(token));
        assertEquals(123, jwtUtil.extractUserId(token));
        assertTrue(jwtUtil.extractRoles(token).contains("ROLE_USER"));

        assertTrue(jwtUtil.isTokenValid(token));
        assertTrue(jwtUtil.isTokenValid(token, "testuser"));
        assertFalse(jwtUtil.isTokenValid(token, "otheruser"));
        assertFalse(jwtUtil.isTokenExpired(token));
        assertFalse(jwtUtil.isRefreshToken(token));
    }

    @Test
    void testGenerateAndExtractRefreshToken() {
        String token = jwtUtil.generateRefreshToken(testUser);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        assertEquals("testuser", jwtUtil.extractUsername(token));
        assertNull(jwtUtil.extractUserId(token));
        assertNull(jwtUtil.extractRoles(token));

        assertTrue(jwtUtil.isTokenValid(token));
        assertTrue(jwtUtil.isTokenValid(token, "testuser"));
        assertFalse(jwtUtil.isTokenExpired(token));
        assertTrue(jwtUtil.isRefreshToken(token));
    }

    @Test
    void testExpiredOrInvalidToken() {
        String invalidToken = "invalid.token.here";
        assertFalse(jwtUtil.isTokenValid(invalidToken));
        assertFalse(jwtUtil.isTokenValid(invalidToken, "testuser"));
        assertTrue(jwtUtil.isTokenExpired(invalidToken));
        assertFalse(jwtUtil.isRefreshToken(invalidToken));
    }
}

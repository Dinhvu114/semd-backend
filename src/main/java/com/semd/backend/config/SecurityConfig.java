package com.semd.backend.config;

import com.semd.backend.security.JwtAuthFiller;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFiller jwtAuthFiller;

    public SecurityConfig(JwtAuthFiller jwtAuthFiller) {
        this.jwtAuthFiller = jwtAuthFiller;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return sha256(rawPassword.toString());
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                if (rawPassword == null || encodedPassword == null) {
                    return false;
                }
                return encode(rawPassword).equalsIgnoreCase(encodedPassword);
            }

            private String sha256(String raw) {
                try {
                    java.security.MessageDigest mDigest = java.security.MessageDigest.getInstance("SHA-256");
                    byte[] out = mDigest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    return java.util.HexFormat.of().formatHex(out);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:8081", "http://127.0.0.1:8081"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"code\":401,\"success\":false,\"message\":\"Chưa đăng nhập hoặc token không hợp lệ\",\"data\":null}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // Cho phép truy cập tự do vào các tài liệu API, endpoint Auth và callback AI
                        .requestMatchers("/api/v1/hello", "/api/v1/auth/**", "/api/v1/calls/callback", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/ws", "/ws/**").permitAll()
                        // Mọi API khác cần phải đăng nhập mới sử dụng được
                        .anyRequest().authenticated()

                )
                // Đăng ký JwtAuthFiller vào chuỗi lọc Spring Security
                .addFilterBefore(jwtAuthFiller, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
package com.semd.backend.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.semd.backend.util.JwtUtil;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFiller extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    public JwtAuthFiller(JwtUtil jwtUtil){
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                if (jwtUtil.isTokenValid(token) && !jwtUtil.isRefreshToken(token)) {
                    String username = jwtUtil.extractUsername(token);
                    
                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        Integer userId = jwtUtil.extractUserId(token);
                        java.util.Collection<String> roles = jwtUtil.extractRoles(token);
                        String fullName = jwtUtil.extractFullName(token);
                        String phoneNumber = jwtUtil.extractPhoneNumber(token);

                        UserPrincipal principal = new UserPrincipal(userId, username, roles, fullName, phoneNumber);


                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                        
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (JwtException e) {
                // Có thể log warning hoặc ignore để filterChain tiếp tục xử lý
            }
        }
        filterChain.doFilter(request, response);
    }
}

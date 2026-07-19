package com.semd.backend.security;

import java.util.List;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails {
    private final Integer id;
    private final String username;
    private final Collection<String> roles;
    private final String fullName;
    private final String phoneNumber;

    public UserPrincipal(Integer id, String username, Collection<String> roles, String fullName, String phoneNumber) {
        this.id = id;
        this.username = username;
        this.roles = roles != null ? roles : java.util.Collections.emptyList();
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
    }

    public Integer getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Collection<String> getRoles() {
        return roles;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> {
                    String normalized = role.toUpperCase();
                    if (!normalized.startsWith("ROLE_")) {
                        normalized = "ROLE_" + normalized;
                    }
                    return new SimpleGrantedAuthority(normalized);
                })
                .toList();
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

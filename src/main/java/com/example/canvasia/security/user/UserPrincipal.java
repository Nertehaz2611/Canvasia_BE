package com.example.canvasia.security.user;

import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.canvasia.entity.User;
import com.example.canvasia.enums.UserStatus;

public class UserPrincipal implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final String email;
    private final String passwordHash;
    private final UserStatus status;
    private final String role;


    public UserPrincipal(User user) {
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.status = user.getStatus();
        this.role = user.getRole().name();
    }

    @Override
    @NullMarked
    public String getUsername() {
        return email;
    }

    @Override
    @NullMarked
    public String getPassword() {
        return passwordHash;
    }

    @Override
    @NullMarked
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}

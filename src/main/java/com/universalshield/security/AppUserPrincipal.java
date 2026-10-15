package com.universalshield.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

/** Authenticated user stored in the HTTP session's security context. */
public class AppUserPrincipal implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String email;
    private final String displayName;
    private final transient String passwordHash;
    private final String role;
    private final boolean enabled;

    public AppUserPrincipal(Long id, String email, String displayName, String passwordHash, String role, boolean enabled) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = enabled;
    }

    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return enabled; }

    @Override
    public String toString() {
        return "AppUserPrincipal{id=" + id + "}";
    }
}

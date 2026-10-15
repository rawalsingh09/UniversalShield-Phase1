package com.universalshield.security;

import com.universalshield.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** Loads users by email for Spring Security's password authentication. */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public AppUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return users.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
                .map(u -> new AppUserPrincipal(u.getId(), u.getEmail(), u.getDisplayName(), u.getPasswordHash(),
                        u.getRole(), u.isEnabled()))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }
}

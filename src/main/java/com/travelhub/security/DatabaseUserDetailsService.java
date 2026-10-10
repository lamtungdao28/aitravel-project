package com.travelhub.security;

import com.travelhub.user.entity.User;
import com.travelhub.user.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    private final RolePermissionRepository permissions;

    @Override
    public TravelHubPrincipal loadUserByUsername(String email) {
        return principal(
                users.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                        .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials")));
    }

    public TravelHubPrincipal loadById(Long id) {
        return principal(
                users.findById(id)
                        .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials")));
    }

    private TravelHubPrincipal principal(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()));
            permissions
                    .findPermissionNames(user.getRole().getId())
                    .forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        }
        return new TravelHubPrincipal(
                user.getId(), user.getEmail(), user.getPassword(), List.copyOf(authorities));
    }
}

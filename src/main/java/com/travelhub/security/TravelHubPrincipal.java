package com.travelhub.security;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

public record TravelHubPrincipal(
        Long id, String email, @JsonIgnore String password, List<GrantedAuthority> authorities)
        implements UserDetails {
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String toString() {
        return "TravelHubPrincipal[id=" + id + "]";
    }
}

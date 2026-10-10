package com.travelhub.security;

import com.travelhub.common.exception.ForbiddenOperationException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("access")
public class Access {
    public boolean isAdmin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null
                && a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }

    public Long id() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof TravelHubPrincipal p) return p.id();
        throw new ForbiddenOperationException("An authenticated account is required");
    }

    public boolean owns(Long userId) {
        return isAdmin() || id().equals(userId);
    }

    public Long resolveUser(Long administrativeUserId) {
        return isAdmin() && administrativeUserId != null ? administrativeUserId : id();
    }
}

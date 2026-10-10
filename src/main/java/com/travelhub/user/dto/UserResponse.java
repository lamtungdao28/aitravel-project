package com.travelhub.user.dto;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String avatarUrl,
        String role,
        Instant createdAt,
        Instant updatedAt) {}

package com.travelhub.admin.dto;

import jakarta.validation.constraints.*;

import java.util.Set;

public record RolePermissionsRequest(
        @NotNull @Size(max = 100) Set<@NotNull @Positive Long> permissionIds) {}

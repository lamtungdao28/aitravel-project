package com.travelhub.admin.dto;
import jakarta.validation.constraints.*;
public record UserRoleRequest(@NotNull @Positive Long roleId) {}

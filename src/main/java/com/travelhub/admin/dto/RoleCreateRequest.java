package com.travelhub.admin.dto;

import jakarta.validation.constraints.*;

public record RoleCreateRequest(
        @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]*") @Size(max = 50) String name,
        @Size(max = 500) String description) {}

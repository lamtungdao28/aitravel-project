package com.travelhub.admin.dto;

import jakarta.validation.constraints.*;

public record PermissionCreateRequest(
        @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]*") @Size(max = 100) String name,
        @Size(max = 500) String description) {}

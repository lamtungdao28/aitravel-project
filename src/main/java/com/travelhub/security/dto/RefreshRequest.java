package com.travelhub.security.dto;

import jakarta.validation.constraints.*;

public record RefreshRequest(@NotBlank @Size(max = 200) String refreshToken) {}

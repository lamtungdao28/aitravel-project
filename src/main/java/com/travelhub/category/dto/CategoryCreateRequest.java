package com.travelhub.category.dto;

import jakarta.validation.constraints.*;

import org.hibernate.validator.constraints.URL;

public record CategoryCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) @Pattern(regexp = "[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*") String slug,
        @Size(max = 500) String description,
        @URL @Size(max = 2048) String imageUrl) {}

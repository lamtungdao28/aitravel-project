package com.travelhub.review.dto;
import jakarta.validation.constraints.*;
public record ReviewUpdateRequest(@NotNull @Min(1) @Max(5) Integer rating,
        @Size(max=200) String title, @NotBlank @Size(max=10000) String content) {}

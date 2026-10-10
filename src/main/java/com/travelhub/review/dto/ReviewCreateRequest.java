package com.travelhub.review.dto;
import jakarta.validation.constraints.*;
import java.util.List;
import org.hibernate.validator.constraints.URL;
public record ReviewCreateRequest(@Positive Long userId,
        @NotNull @Min(1) @Max(5) Integer rating, @Size(max=200) String title,
        @NotBlank @Size(max=10000) String content,
        @Size(max=10) List<@NotBlank @URL @Size(max=2048) String> imageUrls) {}

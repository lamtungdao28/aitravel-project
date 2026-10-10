package com.travelhub.review.dto;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;
public record ReviewImageRequest(@NotBlank @URL @Size(max=2048) String imageUrl) {}

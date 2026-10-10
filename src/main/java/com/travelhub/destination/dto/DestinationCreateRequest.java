package com.travelhub.destination.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import org.hibernate.validator.constraints.URL;

public record DestinationCreateRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 120) String country,
        @Size(max = 10000) String description,
        @URL @Size(max = 2048) String imageUrl,
        @DecimalMin("-90") @DecimalMax("90") @Digits(integer = 3, fraction = 7) BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") @Digits(integer = 3, fraction = 7) BigDecimal longitude,
        @Size(max = 80) String timezone) {}

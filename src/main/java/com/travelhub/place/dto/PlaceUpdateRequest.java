package com.travelhub.place.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Set;

public record PlaceUpdateRequest(
        @NotNull @Positive Long destinationId,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 10000) String description,
        @Size(max = 500) String address,
        @DecimalMin("-90") @DecimalMax("90") @Digits(integer = 3, fraction = 7) BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") @Digits(integer = 3, fraction = 7)
                BigDecimal longitude,
        @NotNull @Min(0) @Max(4) Integer priceLevel,
        boolean bookingEnabled,
        @NotNull @Size(max = 20) Set<@NotNull @Positive Long> categoryIds) {}

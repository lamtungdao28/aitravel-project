package com.travelhub.trip.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record TripCreateRequest(@Positive Long userId, @Positive Long destinationId,
        @NotBlank @Size(max=200) String name, LocalDate startDate, LocalDate endDate,
        @PositiveOrZero @Digits(integer=10,fraction=2) BigDecimal budget, @Size(max=1000) String description) {}

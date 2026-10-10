package com.travelhub.trip.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record TripPlaceRequest(@NotNull @Positive Long placeId, LocalDate visitDate,
        @Size(max=1000) String note, @NotNull @PositiveOrZero Integer sortOrder) {}

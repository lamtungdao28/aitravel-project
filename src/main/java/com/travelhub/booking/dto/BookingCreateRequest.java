package com.travelhub.booking.dto;

import jakarta.validation.constraints.*;

import java.time.*;

public record BookingCreateRequest(
        @Positive Long userId,
        @NotNull @Positive Long placeId,
        @NotNull LocalDate bookingDate,
        @NotNull LocalTime bookingTime,
        @NotNull @Positive @Max(1000) Integer numberOfPeople,
        @Size(max = 5000) String note) {}

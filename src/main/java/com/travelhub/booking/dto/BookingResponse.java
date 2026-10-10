package com.travelhub.booking.dto;

import com.travelhub.booking.entity.BookingStatus;

import java.time.*;

public record BookingResponse(
        Long id,
        Long userId,
        Long placeId,
        String placeName,
        LocalDate bookingDate,
        LocalTime bookingTime,
        int numberOfPeople,
        BookingStatus status,
        String note,
        Instant createdAt,
        Instant updatedAt) {}

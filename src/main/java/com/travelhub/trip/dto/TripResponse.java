package com.travelhub.trip.dto;
import java.math.BigDecimal;
import java.time.*;
public record TripResponse(Long id, Long userId, Long destinationId, String name, LocalDate startDate,
        LocalDate endDate, BigDecimal budget, String description, Instant createdAt, Instant updatedAt) {}

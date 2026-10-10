package com.travelhub.place.dto;
import java.math.BigDecimal;
public record PlaceSummaryResponse(Long id, Long destinationId, String name, int priceLevel,
        BigDecimal averageRating, int reviewCount, boolean bookingEnabled) {}

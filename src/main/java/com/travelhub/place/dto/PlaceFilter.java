package com.travelhub.place.dto;

import java.math.BigDecimal;

public record PlaceFilter(
        String keyword,
        Long destinationId,
        Long categoryId,
        BigDecimal minRating,
        Integer maxPriceLevel,
        Boolean bookingEnabled) {}

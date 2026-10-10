package com.travelhub.admin.dto;

import java.math.BigDecimal;

public record PlaceStatisticsResponse(
        long totalPlaces, long bookablePlaces, BigDecimal averageRating) {}

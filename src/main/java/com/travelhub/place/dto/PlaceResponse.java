package com.travelhub.place.dto;
import com.travelhub.category.dto.CategoryResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PlaceResponse(Long id, Long destinationId, String name, String description, String address,
        BigDecimal latitude, BigDecimal longitude, int priceLevel, BigDecimal averageRating,
        int reviewCount, boolean bookingEnabled, List<CategoryResponse> categories, Instant createdAt, Instant updatedAt) {}

package com.travelhub.destination.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record DestinationResponse(Long id, String name, String country, String description,
                                  String imageUrl, BigDecimal latitude, BigDecimal longitude,
                                  String timezone, Instant createdAt, Instant updatedAt) {}

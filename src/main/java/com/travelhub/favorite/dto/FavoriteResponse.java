package com.travelhub.favorite.dto;
import java.time.Instant;
public record FavoriteResponse(Long userId, Long placeId, Instant createdAt) {}

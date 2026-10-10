package com.travelhub.review.dto;
import java.time.Instant;
import java.util.List;
public record ReviewResponse(Long id, Long userId, String userFullName, Long placeId, int rating,
        String title, String content, List<ReviewImageResponse> images, Instant createdAt, Instant updatedAt) {}

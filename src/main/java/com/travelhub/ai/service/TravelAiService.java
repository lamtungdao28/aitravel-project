package com.travelhub.ai.service;

import com.travelhub.ai.dto.*;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TravelAiService {
    private final AiDataService data;
    private final AiService ai;

    @Cacheable(cacheNames = "reviewSummaries", key = "#placeId")
    @PreAuthorize("isAuthenticated()")
    public ReviewSummaryResponse summary(Long placeId) {
        var input = data.reviewInput(placeId);
        ReviewSummary summary =
                input.reviews().isEmpty()
                        ? new ReviewSummary("No reviews yet.", List.of(), List.of())
                        : ai.summarizeReviews(input);
        return new ReviewSummaryResponse(
                placeId,
                input.averageRating(),
                input.reviewCount(),
                summary.summary(),
                summary.positivePoints(),
                summary.negativePoints());
    }

    @PreAuthorize("@access.owns(#userId)")
    public List<PlaceRecommendation> recommendations(Long userId, Long destinationId, int limit) {
        return ai.recommendPlaces(data.recommendationInput(userId, destinationId), limit);
    }
}

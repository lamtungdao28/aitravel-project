package com.travelhub.ai.dto;
import java.math.BigDecimal;
import java.util.List;
public final class AiInputs {
    private AiInputs() {}
    public record ReviewText(int rating,String title,String content) {}
    public record ReviewInput(Long placeId,String name,BigDecimal averageRating,int reviewCount,List<ReviewText> reviews) {}
    public record Candidate(Long placeId,String name,String destination,List<String> categories,int priceLevel,BigDecimal averageRating,int reviewCount) {}
    public record HistoryReview(String placeName,int rating) {}
    public record RecommendationInput(List<String> favoritePlaces,List<HistoryReview> reviewHistory,List<String> tripDestinations,List<Candidate> candidates) {}
}

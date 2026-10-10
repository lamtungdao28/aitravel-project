package com.travelhub.ai.service;
import com.travelhub.ai.dto.*;
import com.travelhub.ai.dto.AiInputs.*;
import java.util.List;
public interface AiService {
    ReviewSummary summarizeReviews(ReviewInput input);
    List<PlaceRecommendation> recommendPlaces(RecommendationInput input,int limit);
}

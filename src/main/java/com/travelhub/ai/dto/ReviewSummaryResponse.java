package com.travelhub.ai.dto;
import java.math.BigDecimal;
import java.util.List;
public record ReviewSummaryResponse(Long placeId,BigDecimal averageRating,int reviewCount,String summary,List<String> positivePoints,List<String> negativePoints) {}

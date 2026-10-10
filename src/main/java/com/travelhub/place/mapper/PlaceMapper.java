package com.travelhub.place.mapper;

import com.travelhub.category.dto.CategoryResponse;
import com.travelhub.category.entity.Category;
import com.travelhub.category.mapper.CategoryMapper;
import com.travelhub.destination.mapper.DestinationMapper;
import com.travelhub.place.dto.PlaceDetailResponse;
import com.travelhub.place.dto.PlaceResponse;
import com.travelhub.place.dto.PlaceSummaryResponse;
import com.travelhub.place.entity.Place;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PlaceMapper {
    private final CategoryMapper categoryMapper;
    private final DestinationMapper destinationMapper;

    public PlaceResponse toResponse(Place place, List<Category> categories) {
        return new PlaceResponse(
                place.getId(),
                place.getDestination().getId(),
                place.getName(),
                place.getDescription(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getPriceLevel(),
                place.getAverageRating(),
                place.getReviewCount(),
                place.isBookingEnabled(),
                categoryResponses(categories),
                place.getCreatedAt(),
                place.getUpdatedAt());
    }

    public PlaceDetailResponse toDetail(Place place, List<Category> categories) {
        return new PlaceDetailResponse(
                place.getId(),
                destinationMapper.toSummary(place.getDestination()),
                place.getName(),
                place.getDescription(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getPriceLevel(),
                place.getAverageRating(),
                place.getReviewCount(),
                place.isBookingEnabled(),
                categoryResponses(categories),
                place.getCreatedAt(),
                place.getUpdatedAt());
    }

    public PlaceSummaryResponse toSummary(Place place) {
        return new PlaceSummaryResponse(
                place.getId(),
                place.getDestination().getId(),
                place.getName(),
                place.getPriceLevel(),
                place.getAverageRating(),
                place.getReviewCount(),
                place.isBookingEnabled());
    }

    private List<CategoryResponse> categoryResponses(List<Category> categories) {
        return categories.stream()
                .sorted(Comparator.comparing(Category::getName))
                .map(categoryMapper::toResponse)
                .toList();
    }
}

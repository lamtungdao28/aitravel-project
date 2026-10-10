package com.travelhub.ai.service;

import com.travelhub.ai.dto.AiInputs.*;
import com.travelhub.ai.repository.RecommendationRepository;
import com.travelhub.config.AiProperties;
import com.travelhub.favorite.repository.FavoriteRepository;
import com.travelhub.place.entity.PlaceCategory;
import com.travelhub.place.repository.PlaceCategoryRepository;
import com.travelhub.place.service.PlaceService;
import com.travelhub.review.repository.ReviewRepository;
import com.travelhub.trip.repository.TripRepository;
import com.travelhub.user.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiDataService {
    private final PlaceService places;
    private final UserService users;
    private final ReviewRepository reviews;
    private final FavoriteRepository favorites;
    private final TripRepository trips;
    private final PlaceCategoryRepository categories;
    private final RecommendationRepository recommendations;
    private final AiProperties properties;

    public ReviewInput reviewInput(Long placeId) {
        var p = places.require(placeId);
        var sample =
                reviews
                        .findByPlaceId(
                                placeId,
                                PageRequest.of(
                                        0,
                                        properties.getMaxReviews(),
                                        Sort.by(
                                                Sort.Order.desc("createdAt"),
                                                Sort.Order.desc("id"))))
                        .stream()
                        .map(
                                r ->
                                        new ReviewText(
                                                r.getRating(),
                                                r.getTitle(),
                                                r.getContent()
                                                        .substring(
                                                                0,
                                                                Math.min(
                                                                        r.getContent().length(),
                                                                        properties
                                                                                .getMaxReviewChars()))))
                        .toList();
        return new ReviewInput(
                placeId, p.getName(), p.getAverageRating(), p.getReviewCount(), sample);
    }

    public RecommendationInput recommendationInput(Long userId, Long destinationId) {
        users.require(userId);
        var candidates =
                recommendations.candidates(userId, destinationId, properties.getMaxCandidates());
        Map<Long, List<PlaceCategory>> categoryMap =
                candidates.isEmpty()
                        ? Map.of()
                        : categories
                                .findCategoriesByPlaceIds(
                                        candidates.stream().map(p -> p.getId()).toList())
                                .stream()
                                .collect(Collectors.groupingBy(c -> c.getPlace().getId()));
        var historyPage = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt")));
        var favoriteNames =
                favorites.findByUserId(userId, historyPage).stream()
                        .map(f -> f.getPlace().getName())
                        .toList();
        var reviewHistory =
                reviews.findByUserId(userId, historyPage).stream()
                        .map(r -> new HistoryReview(r.getPlace().getName(), r.getRating()))
                        .toList();
        var destinations =
                trips.findByUserId(userId, historyPage).stream()
                        .filter(t -> t.getDestination() != null)
                        .map(t -> t.getDestination().getName())
                        .distinct()
                        .toList();
        var input =
                candidates.stream()
                        .map(
                                p ->
                                        new Candidate(
                                                p.getId(),
                                                p.getName(),
                                                p.getDestination().getName(),
                                                categoryMap
                                                        .getOrDefault(p.getId(), List.of())
                                                        .stream()
                                                        .map(c -> c.getCategory().getName())
                                                        .toList(),
                                                p.getPriceLevel(),
                                                p.getAverageRating(),
                                                p.getReviewCount()))
                        .toList();
        return new RecommendationInput(favoriteNames, reviewHistory, destinations, input);
    }
}

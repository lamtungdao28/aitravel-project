package com.travelhub.review.service;

import com.travelhub.common.exception.BusinessException;
import com.travelhub.common.exception.ResourceNotFoundException;
import com.travelhub.common.response.PageResponse;
import com.travelhub.common.util.Pages;
import com.travelhub.place.entity.Place;
import com.travelhub.place.service.PlaceService;
import com.travelhub.review.dto.*;
import com.travelhub.review.entity.Review;
import com.travelhub.review.entity.ReviewImage;
import com.travelhub.review.mapper.ReviewMapper;
import com.travelhub.review.repository.ReviewImageRepository;
import com.travelhub.review.repository.ReviewRepository;
import com.travelhub.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReviewService {
    private static final int MAX_IMAGES = 10;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository imageRepository;
    private final ReviewMapper reviewMapper;
    private final PlaceService placeService;
    private final UserService userService;

    @Transactional
    @CacheEvict(
            cacheNames = {"placeDetails", "popularPlaces", "topRatedPlaces", "reviewSummaries"},
            allEntries = true)
    @PreAuthorize("hasAuthority('REVIEW_CREATE') and @access.owns(#request.userId())")
    public ReviewResponse create(Long placeId, ReviewCreateRequest request) {
        Place place = placeService.lock(placeId);
        List<String> imageUrls = request.imageUrls() == null ? List.of() : request.imageUrls();
        checkImageCount(imageUrls.size());
        Review review = new Review();
        review.setUser(userService.require(request.userId()));
        review.setPlace(place);
        review.setRating(request.rating());
        review.setTitle(request.title());
        review.setContent(request.content());
        reviewRepository.saveAndFlush(review);
        List<ReviewImage> images = imageUrls.stream().map(url -> newImage(review, url)).toList();
        imageRepository.saveAllAndFlush(images);
        recalculateRating(place);
        log.info("Created review {} for place {}", review.getId(), placeId);
        return reviewMapper.toResponse(review, images);
    }

    public ReviewResponse get(Long id) {
        Review review = require(id);
        return reviewMapper.toResponse(review, imageRepository.findByReviewIdOrderByIdAsc(id));
    }

    public PageResponse<ReviewResponse> list(Long placeId, Pageable pageable) {
        placeService.require(placeId);
        Page<Review> page =
                reviewRepository.findByPlaceId(
                        placeId, Pages.checked(pageable, "id", "createdAt", "updatedAt", "rating"));
        return mapPage(page);
    }

    public PageResponse<ReviewResponse> mapPage(Page<Review> page) {
        if (page.isEmpty()) {
            return PageResponse.from(
                    page.map(review -> reviewMapper.toResponse(review, List.of())));
        }
        List<Long> reviewIds = page.getContent().stream().map(Review::getId).toList();
        Map<Long, List<ReviewImage>> images =
                imageRepository.findByReviewIdInOrderByIdAsc(reviewIds).stream()
                        .collect(Collectors.groupingBy(image -> image.getReview().getId()));
        return PageResponse.from(
                page.map(
                        review ->
                                reviewMapper.toResponse(
                                        review, images.getOrDefault(review.getId(), List.of()))));
    }

    @Transactional
    @CacheEvict(
            cacheNames = {"placeDetails", "popularPlaces", "topRatedPlaces", "reviewSummaries"},
            allEntries = true)
    @PreAuthorize("hasAuthority('REVIEW_UPDATE') and @access.owns(@reviewService.ownerId(#id))")
    public ReviewResponse update(Long id, ReviewUpdateRequest request) {
        Place place = lockPlaceForReview(id);
        Review review = require(id);
        review.setRating(request.rating());
        review.setTitle(request.title());
        review.setContent(request.content());
        recalculateRating(place);
        log.info("Updated review {} for place {}", id, place.getId());
        return reviewMapper.toResponse(review, imageRepository.findByReviewIdOrderByIdAsc(id));
    }

    @Transactional
    @CacheEvict(
            cacheNames = {"placeDetails", "popularPlaces", "topRatedPlaces", "reviewSummaries"},
            allEntries = true)
    @PreAuthorize("hasAuthority('REVIEW_DELETE') and @access.owns(@reviewService.ownerId(#id))")
    public void delete(Long id) {
        Place place = lockPlaceForReview(id);
        Review review = require(id);
        imageRepository.deleteAll(imageRepository.findByReviewIdOrderByIdAsc(id));
        imageRepository.flush();
        reviewRepository.delete(review);
        recalculateRating(place);
        log.info("Deleted review {} for place {}", id, place.getId());
    }

    @Transactional
    @PreAuthorize(
            "hasAuthority('REVIEW_UPDATE') and @access.owns(@reviewService.ownerId(#reviewId))")
    public ReviewImageResponse addImage(Long reviewId, ReviewImageRequest request) {
        lockPlaceForReview(reviewId);
        Review review = require(reviewId);
        checkImageCount(imageRepository.countByReviewId(reviewId) + 1);
        ReviewImage image = imageRepository.saveAndFlush(newImage(review, request.imageUrl()));
        log.info("Added image {} to review {}", image.getId(), reviewId);
        return reviewMapper.toImageResponse(image);
    }

    @Transactional
    @PreAuthorize(
            "hasAuthority('REVIEW_UPDATE') and @access.owns(@reviewService.imageOwnerId(#imageId))")
    public void deleteImage(Long imageId) {
        Long reviewId =
                imageRepository
                        .findReviewId(imageId)
                        .orElseThrow(() -> new ResourceNotFoundException("Review image", imageId));
        lockPlaceForReview(reviewId);
        ReviewImage image =
                imageRepository
                        .findById(imageId)
                        .orElseThrow(() -> new ResourceNotFoundException("Review image", imageId));
        imageRepository.delete(image);
        imageRepository.flush();
        log.info("Deleted image {} from review {}", imageId, reviewId);
    }

    public Long ownerId(Long reviewId) {
        return reviewRepository
                .findUserId(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
    }

    public Long imageOwnerId(Long imageId) {
        return imageRepository
                .findUserId(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Review image", imageId));
    }

    private Review require(Long id) {
        return reviewRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", id));
    }

    private Place lockPlaceForReview(Long reviewId) {
        Long placeId =
                reviewRepository
                        .findPlaceId(reviewId)
                        .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
        return placeService.lock(placeId);
    }

    private void recalculateRating(Place place) {
        // Publish pending review changes to the aggregate query while holding the place lock.
        reviewRepository.flush();
        ReviewRepository.RatingStatistics statistics = reviewRepository.statistics(place.getId());
        place.setReviewCount(Math.toIntExact(statistics.getReviewCount()));
        place.setAverageRating(
                statistics.getAverageRating() == null
                        ? BigDecimal.ZERO.setScale(2)
                        : BigDecimal.valueOf(statistics.getAverageRating())
                                .setScale(2, RoundingMode.HALF_UP));
        reviewRepository.flush();
    }

    private ReviewImage newImage(Review review, String url) {
        ReviewImage image = new ReviewImage();
        image.setReview(review);
        image.setImageUrl(url);
        return image;
    }

    private void checkImageCount(long count) {
        if (count > MAX_IMAGES) {
            throw new BusinessException("A review can contain at most " + MAX_IMAGES + " images");
        }
    }
}

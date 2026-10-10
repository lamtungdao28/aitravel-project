package com.travelhub.review.controller;

import com.travelhub.common.response.PageResponse;
import com.travelhub.review.dto.*;
import com.travelhub.review.service.ReviewService;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "Reviews")
public class ReviewController {
    private final ReviewService service;
    private final com.travelhub.security.Access access;

    @PostMapping("/places/{placeId}/reviews")
    public ResponseEntity<ReviewResponse> create(
            @PathVariable @Positive Long placeId, @Valid @RequestBody ReviewCreateRequest request) {
        var response =
                service.create(
                        placeId,
                        new ReviewCreateRequest(
                                access.resolveUser(request.userId()),
                                request.rating(),
                                request.title(),
                                request.content(),
                                request.imageUrls()));
        return ResponseEntity.created(URI.create("/api/v1/reviews/" + response.id()))
                .body(response);
    }

    @GetMapping("/places/{placeId}/reviews")
    public PageResponse<ReviewResponse> list(
            @PathVariable @Positive Long placeId,
            @ParameterObject
                    @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return service.list(placeId, pageable);
    }

    @GetMapping("/reviews/{id}")
    public ReviewResponse get(@PathVariable @Positive Long id) {
        return service.get(id);
    }

    @PutMapping("/reviews/{id}")
    public ReviewResponse update(
            @PathVariable @Positive Long id, @Valid @RequestBody ReviewUpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reviews/{reviewId}/images")
    public ResponseEntity<ReviewImageResponse> addImage(
            @PathVariable @Positive Long reviewId, @Valid @RequestBody ReviewImageRequest request) {
        var response = service.addImage(reviewId, request);
        return ResponseEntity.created(URI.create("/api/v1/review-images/" + response.id()))
                .body(response);
    }

    @DeleteMapping("/review-images/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable @Positive Long id) {
        service.deleteImage(id);
        return ResponseEntity.noContent().build();
    }
}

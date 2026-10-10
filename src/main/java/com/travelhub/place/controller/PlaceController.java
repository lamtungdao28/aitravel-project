package com.travelhub.place.controller;

import com.travelhub.common.response.PageResponse;
import com.travelhub.place.dto.*;
import com.travelhub.place.service.PlaceService;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.RequiredArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
@Validated
@Tag(name = "Places")
public class PlaceController {
    private final PlaceService service;

    @PostMapping
    public ResponseEntity<PlaceResponse> create(@Valid @RequestBody PlaceCreateRequest request) {
        var response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/places/" + response.id())).body(response);
    }

    @GetMapping
    public PageResponse<PlaceSummaryResponse> search(
            @RequestParam(required = false) @Size(max = 200) String keyword,
            @RequestParam(required = false) @Positive Long destinationId,
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(required = false) @DecimalMin("0") @DecimalMax("5") BigDecimal minRating,
            @RequestParam(required = false) @Min(0) @Max(4) Integer maxPriceLevel,
            @RequestParam(required = false) Boolean bookingEnabled,
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return service.search(
                new PlaceFilter(
                        keyword,
                        destinationId,
                        categoryId,
                        minRating,
                        maxPriceLevel,
                        bookingEnabled),
                pageable);
    }

    @GetMapping("/popular")
    public PageResponse<PlaceSummaryResponse> popular(@ParameterObject Pageable pageable) {
        return service.popular(pageable);
    }

    @GetMapping("/top-rated")
    public PageResponse<PlaceSummaryResponse> topRated(@ParameterObject Pageable pageable) {
        return service.topRated(pageable);
    }

    @GetMapping("/{id}")
    public PlaceDetailResponse get(@PathVariable @Positive Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public PlaceResponse update(
            @PathVariable @Positive Long id, @Valid @RequestBody PlaceUpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

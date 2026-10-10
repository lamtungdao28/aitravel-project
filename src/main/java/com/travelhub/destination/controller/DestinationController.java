package com.travelhub.destination.controller;

import com.travelhub.common.response.PageResponse;
import com.travelhub.destination.dto.*;
import com.travelhub.destination.service.DestinationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

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
@RequestMapping("/api/v1/destinations")
@RequiredArgsConstructor
@Validated
@Tag(name = "Destinations")
public class DestinationController {
    private final DestinationService destinationService;

    @PostMapping
    @Operation(summary = "Create a travel destination")
    public ResponseEntity<DestinationResponse> create(
            @Valid @RequestBody DestinationCreateRequest request) {
        DestinationResponse response = destinationService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/destinations/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get destination details")
    public DestinationResponse get(@PathVariable @Positive Long id) {
        return destinationService.get(id);
    }

    @GetMapping
    @Operation(summary = "Search destinations with optional keyword and country filters")
    public PageResponse<DestinationResponse> search(
            @RequestParam(required = false) @Size(max = 200) String keyword,
            @RequestParam(required = false) @Size(max = 120) String country,
            @ParameterObject
                    @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return destinationService.search(keyword, country, pageable);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace destination details")
    public DestinationResponse update(
            @PathVariable @Positive Long id, @Valid @RequestBody DestinationUpdateRequest request) {
        return destinationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a destination without places or trips")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        destinationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.travelhub.trip.controller;

import com.travelhub.common.response.PageResponse;
import com.travelhub.trip.dto.*;
import com.travelhub.trip.service.TripService;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "Trips")
public class TripController {
    private final TripService service;
    private final com.travelhub.security.Access access;

    @PostMapping("/trips")
    public ResponseEntity<TripResponse> create(@Valid @RequestBody TripCreateRequest r) {
        var t =
                service.create(
                        new TripCreateRequest(
                                access.resolveUser(r.userId()),
                                r.destinationId(),
                                r.name(),
                                r.startDate(),
                                r.endDate(),
                                r.budget(),
                                r.description()));
        return ResponseEntity.created(URI.create("/api/v1/trips/" + t.id())).body(t);
    }

    @GetMapping("/trips/{id}")
    public TripResponse get(@PathVariable @Positive Long id) {
        return service.get(id);
    }

    @PutMapping("/trips/{id}")
    public TripResponse update(
            @PathVariable @Positive Long id, @Valid @RequestBody TripUpdateRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/trips/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/trips")
    public PageResponse<TripResponse> list(
            @PathVariable @Positive Long userId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable p) {
        return service.list(userId, p);
    }

    @PostMapping("/trips/{tripId}/places")
    public ResponseEntity<TripPlaceResponse> addPlace(
            @PathVariable @Positive Long tripId, @Valid @RequestBody TripPlaceRequest r) {
        var p = service.addPlace(tripId, r);
        return ResponseEntity.created(
                        URI.create("/api/v1/trips/" + tripId + "/places/" + p.placeId()))
                .body(p);
    }

    @PutMapping("/trips/{tripId}/places/{placeId}")
    public TripPlaceResponse updatePlace(
            @PathVariable @Positive Long tripId,
            @PathVariable @Positive Long placeId,
            @Valid @RequestBody TripPlaceRequest r) {
        return service.updatePlace(tripId, placeId, r);
    }

    @DeleteMapping("/trips/{tripId}/places/{placeId}")
    public ResponseEntity<Void> removePlace(
            @PathVariable @Positive Long tripId, @PathVariable @Positive Long placeId) {
        service.removePlace(tripId, placeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trips/{tripId}/places")
    public PageResponse<TripPlaceResponse> listPlaces(
            @PathVariable @Positive Long tripId, @ParameterObject Pageable p) {
        return service.listPlaces(tripId, p);
    }
}

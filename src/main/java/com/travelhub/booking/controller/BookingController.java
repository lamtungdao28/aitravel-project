package com.travelhub.booking.controller;

import com.travelhub.booking.dto.*;
import com.travelhub.booking.service.BookingService;
import com.travelhub.common.response.PageResponse;

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
@Tag(name = "Bookings")
public class BookingController {
    private final BookingService service;
    private final com.travelhub.security.Access access;

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingCreateRequest r) {
        var b =
                service.create(
                        new BookingCreateRequest(
                                access.resolveUser(r.userId()),
                                r.placeId(),
                                r.bookingDate(),
                                r.bookingTime(),
                                r.numberOfPeople(),
                                r.note()));
        return ResponseEntity.created(URI.create("/api/v1/bookings/" + b.id())).body(b);
    }

    @GetMapping("/bookings/{id}")
    public BookingResponse get(@PathVariable @Positive Long id) {
        return service.get(id);
    }

    @PutMapping("/bookings/{id}")
    public BookingResponse update(
            @PathVariable @Positive Long id, @Valid @RequestBody BookingUpdateRequest r) {
        return service.update(id, r);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/bookings")
    public PageResponse<BookingResponse> list(
            @PathVariable @Positive Long userId,
            @ParameterObject @PageableDefault(size = 20, sort = "bookingDate") Pageable p) {
        return service.list(userId, p);
    }

    @PutMapping("/bookings/{id}/confirm")
    public BookingResponse confirm(@PathVariable @Positive Long id) {
        return service.confirm(id);
    }

    @PutMapping("/bookings/{id}/cancel")
    public BookingResponse cancel(@PathVariable @Positive Long id) {
        return service.cancel(id);
    }

    @PutMapping("/bookings/{id}/complete")
    public BookingResponse complete(@PathVariable @Positive Long id) {
        return service.complete(id);
    }
}

package com.travelhub.trip.service;

import com.travelhub.common.exception.*;
import com.travelhub.common.response.PageResponse;
import com.travelhub.common.util.Pages;
import com.travelhub.destination.repository.DestinationRepository;
import com.travelhub.place.service.PlaceService;
import com.travelhub.trip.dto.*;
import com.travelhub.trip.entity.*;
import com.travelhub.trip.mapper.TripMapper;
import com.travelhub.trip.repository.*;
import com.travelhub.user.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripService {
    private final TripRepository repository;
    private final TripPlaceRepository itinerary;
    private final TripMapper mapper;
    private final UserService users;
    private final PlaceService places;
    private final DestinationRepository destinations;

    @Transactional
    @PreAuthorize("@access.owns(#r.userId())")
    public TripResponse create(TripCreateRequest r) {
        Trip t = new Trip();
        t.setUser(users.require(r.userId()));
        apply(
                t,
                r.destinationId(),
                r.name(),
                r.startDate(),
                r.endDate(),
                r.budget(),
                r.description());
        return mapper.toResponse(repository.saveAndFlush(t));
    }

    @PreAuthorize("@access.owns(@tripService.ownerId(#id))")
    public TripResponse get(Long id) {
        return mapper.toResponse(require(id));
    }

    @PreAuthorize("@access.owns(#userId)")
    public PageResponse<TripResponse> list(Long userId, Pageable pageable) {
        users.require(userId);
        return PageResponse.from(
                repository
                        .findByUserId(
                                userId,
                                Pages.checked(pageable, "id", "name", "startDate", "createdAt"))
                        .map(mapper::toResponse));
    }

    @Transactional
    @PreAuthorize("@access.owns(@tripService.ownerId(#id))")
    public TripResponse update(Long id, TripUpdateRequest r) {
        Trip t = lock(id);
        validateDates(r.startDate(), r.endDate());
        itinerary
                .findByTripId(id)
                .forEach(p -> validateVisit(p.getVisitDate(), r.startDate(), r.endDate()));
        apply(
                t,
                r.destinationId(),
                r.name(),
                r.startDate(),
                r.endDate(),
                r.budget(),
                r.description());
        repository.flush();
        return mapper.toResponse(t);
    }

    @Transactional
    @PreAuthorize("@access.owns(@tripService.ownerId(#id))")
    public void delete(Long id) {
        Trip t = lock(id);
        itinerary.deleteAll(itinerary.findByTripId(id));
        itinerary.flush();
        repository.delete(t);
        repository.flush();
    }

    @Transactional
    @PreAuthorize("@access.owns(@tripService.ownerId(#id))")
    public TripPlaceResponse addPlace(Long id, TripPlaceRequest r) {
        Trip t = lock(id);
        TripPlaceId key = new TripPlaceId(id, r.placeId());
        if (itinerary.existsById(key))
            throw new DuplicateResourceException("Place is already in this trip");
        TripPlace p = new TripPlace();
        p.setId(key);
        p.setTrip(t);
        p.setPlace(places.require(r.placeId()));
        applyVisit(p, r, t);
        return mapper.toResponse(itinerary.saveAndFlush(p));
    }

    @Transactional
    @PreAuthorize("@access.owns(@tripService.ownerId(#id))")
    public TripPlaceResponse updatePlace(Long id, Long placeId, TripPlaceRequest r) {
        Trip t = lock(id);
        if (!placeId.equals(r.placeId()))
            throw new BusinessException("Place ID must match the path");
        TripPlace p =
                itinerary
                        .findById(new TripPlaceId(id, placeId))
                        .orElseThrow(() -> new ResourceNotFoundException("Trip place", placeId));
        applyVisit(p, r, t);
        itinerary.flush();
        return mapper.toResponse(p);
    }

    @Transactional
    @PreAuthorize("@access.owns(@tripService.ownerId(#id))")
    public void removePlace(Long id, Long placeId) {
        lock(id);
        TripPlace p =
                itinerary
                        .findById(new TripPlaceId(id, placeId))
                        .orElseThrow(() -> new ResourceNotFoundException("Trip place", placeId));
        itinerary.delete(p);
        itinerary.flush();
    }

    @PreAuthorize("@access.owns(@tripService.ownerId(#id))")
    public PageResponse<TripPlaceResponse> listPlaces(Long id, Pageable pageable) {
        require(id);
        Pageable checked = Pages.checked(pageable, "visitDate", "sortOrder", "id.placeId");
        Sort sort = Sort.by("visitDate", "sortOrder", "id.placeId");
        return PageResponse.from(
                itinerary
                        .findByTripId(
                                id,
                                PageRequest.of(
                                        checked.getPageNumber(), checked.getPageSize(), sort))
                        .map(mapper::toResponse));
    }

    public Long ownerId(Long id) {
        return repository
                .findUserId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", id));
    }

    private Trip require(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Trip", id));
    }

    private Trip lock(Long id) {
        return repository
                .findLockedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", id));
    }

    private void apply(
            Trip t,
            Long destinationId,
            String name,
            LocalDate start,
            LocalDate end,
            BigDecimal budget,
            String description) {
        validateDates(start, end);
        if (budget != null && budget.signum() < 0)
            throw new BusinessException("Budget must not be negative");
        t.setDestination(
                destinationId == null
                        ? null
                        : destinations
                                .findById(destinationId)
                                .orElseThrow(
                                        () ->
                                                new ResourceNotFoundException(
                                                        "Destination", destinationId)));
        t.setName(name.trim());
        t.setStartDate(start);
        t.setEndDate(end);
        t.setBudget(budget);
        t.setDescription(description);
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start))
            throw new BusinessException("End date must not precede start date");
    }

    private void validateVisit(LocalDate date, LocalDate start, LocalDate end) {
        if (date != null
                && ((start != null && date.isBefore(start)) || (end != null && date.isAfter(end))))
            throw new BusinessException("Visit date must be within the trip dates");
    }

    private void applyVisit(TripPlace p, TripPlaceRequest r, Trip t) {
        validateVisit(r.visitDate(), t.getStartDate(), t.getEndDate());
        p.setVisitDate(r.visitDate());
        p.setNote(r.note());
        p.setSortOrder(r.sortOrder());
    }
}

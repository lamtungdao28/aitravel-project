package com.travelhub.place.service;

import com.travelhub.category.entity.Category;
import com.travelhub.category.repository.CategoryRepository;
import com.travelhub.common.exception.BusinessException;
import com.travelhub.common.exception.ResourceNotFoundException;
import com.travelhub.common.response.PageResponse;
import com.travelhub.common.util.Pages;
import com.travelhub.destination.entity.Destination;
import com.travelhub.destination.repository.DestinationRepository;
import com.travelhub.place.dto.*;
import com.travelhub.place.entity.Place;
import com.travelhub.place.entity.PlaceCategory;
import com.travelhub.place.entity.PlaceCategoryId;
import com.travelhub.place.mapper.PlaceMapper;
import com.travelhub.place.repository.PlaceCategoryRepository;
import com.travelhub.place.repository.PlaceRepository;
import com.travelhub.place.repository.PlaceSpecification;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PlaceService {
    private static final String[] SORT_FIELDS = {"id", "name", "priceLevel", "averageRating", "reviewCount", "createdAt", "updatedAt"};

    private final PlaceRepository placeRepository;
    private final PlaceCategoryRepository placeCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final DestinationRepository destinationRepository;
    private final PlaceMapper placeMapper;

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames={"placeDetails","popularPlaces","topRatedPlaces","reviewSummaries"},allEntries=true)
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('PLACE_CREATE')")
    public PlaceResponse create(PlaceCreateRequest request) {
        Destination destination = requireDestination(request.destinationId());
        List<Category> categories = requireCategories(request.categoryIds());
        Place place = new Place();
        apply(place, destination, request.name(), request.description(), request.address(), request.latitude(),
                request.longitude(), request.priceLevel(), request.bookingEnabled());
        placeRepository.saveAndFlush(place);
        assignCategories(place, categories);
        log.info("Created place {} in destination {}", place.getId(), destination.getId());
        return placeMapper.toResponse(place, categories);
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames={"placeDetails","popularPlaces","topRatedPlaces","reviewSummaries"},allEntries=true)
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('PLACE_UPDATE')")
    public PlaceResponse update(Long id, PlaceUpdateRequest request) {
        Place place = lock(id);
        Destination destination = requireDestination(request.destinationId());
        List<Category> categories = requireCategories(request.categoryIds());
        apply(place, destination, request.name(), request.description(), request.address(), request.latitude(),
                request.longitude(), request.priceLevel(), request.bookingEnabled());
        replaceCategories(place, categories);
        placeRepository.flush();
        log.info("Updated place {}", id);
        return placeMapper.toResponse(place, categories);
    }

    @org.springframework.cache.annotation.Cacheable(cacheNames="placeDetails",key="#id")
    public PlaceDetailResponse get(Long id) {
        Place place = require(id);
        List<Category> categories = placeCategoryRepository.findCategoriesByPlaceId(id).stream()
                .map(PlaceCategory::getCategory).toList();
        return placeMapper.toDetail(place, categories);
    }

    public PageResponse<PlaceSummaryResponse> search(PlaceFilter filter, Pageable pageable) {
        return PageResponse.from(placeRepository.findAll(PlaceSpecification.search(filter), Pages.checked(pageable, SORT_FIELDS))
                .map(placeMapper::toSummary));
    }

    @org.springframework.cache.annotation.Cacheable(cacheNames="popularPlaces",key="@cacheKeys.key(#pageable)")
    public PageResponse<PlaceSummaryResponse> popular(Pageable pageable) {
        return ranked(pageable, Sort.by(Sort.Order.desc("reviewCount"), Sort.Order.desc("averageRating"), Sort.Order.asc("id")));
    }

    @org.springframework.cache.annotation.Cacheable(cacheNames="topRatedPlaces",key="@cacheKeys.key(#pageable)")
    public PageResponse<PlaceSummaryResponse> topRated(Pageable pageable) {
        return ranked(pageable, Sort.by(Sort.Order.desc("averageRating"), Sort.Order.desc("reviewCount"), Sort.Order.asc("id")));
    }

    public PageResponse<PlaceSummaryResponse> destinationPlaces(Long destinationId, Pageable pageable) {
        requireDestination(destinationId);
        return search(new PlaceFilter(null, destinationId, null, null, null, null), pageable);
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames={"placeDetails","popularPlaces","topRatedPlaces","reviewSummaries"},allEntries=true)
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('PLACE_DELETE')")
    public void delete(Long id) {
        Place place = lock(id);
        if (placeRepository.countReviews(id) > 0 || placeRepository.countBookings(id) > 0
                || placeRepository.countTripPlaces(id) > 0 || placeRepository.countFavorites(id) > 0) {
            throw new BusinessException("Cannot delete a place referenced by reviews, bookings, trips, or favorites");
        }
        placeCategoryRepository.deleteAll(placeCategoryRepository.findCategoriesByPlaceId(id));
        placeCategoryRepository.flush();
        placeRepository.delete(place);
        placeRepository.flush();
        log.info("Deleted place {}", id);
    }

    public Place require(Long id) {
        return placeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Place", id));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Place lock(Long id) {
        return placeRepository.findLockedById(id).orElseThrow(() -> new ResourceNotFoundException("Place", id));
    }

    private PageResponse<PlaceSummaryResponse> ranked(Pageable pageable, Sort sort) {
        Pageable checked = Pages.checked(pageable, SORT_FIELDS);
        return PageResponse.from(placeRepository.findAll(PageRequest.of(checked.getPageNumber(), checked.getPageSize(), sort))
                .map(placeMapper::toSummary));
    }

    private Destination requireDestination(Long id) {
        return destinationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Destination", id));
    }

    private List<Category> requireCategories(Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }
        List<Category> categories = categoryRepository.findAllById(categoryIds);
        Set<Long> foundIds = categories.stream().map(Category::getId).collect(Collectors.toSet());
        categoryIds.stream().filter(id -> !foundIds.contains(id)).findFirst()
                .ifPresent(id -> { throw new ResourceNotFoundException("Category", id); });
        return categories;
    }

    private void assignCategories(Place place, List<Category> categories) {
        List<PlaceCategory> assignments = categories.stream().map(category -> {
            PlaceCategory assignment = new PlaceCategory();
            assignment.setId(new PlaceCategoryId(place.getId(), category.getId()));
            assignment.setPlace(place);
            assignment.setCategory(category);
            return assignment;
        }).toList();
        placeCategoryRepository.saveAllAndFlush(assignments);
    }

    private void replaceCategories(Place place, List<Category> categories) {
        List<PlaceCategory> existing = placeCategoryRepository.findCategoriesByPlaceId(place.getId());
        Set<Long> wanted = categories.stream().map(Category::getId).collect(Collectors.toSet());
        Set<Long> retained = existing.stream().map(link -> link.getCategory().getId()).collect(Collectors.toSet());
        placeCategoryRepository.deleteAll(existing.stream()
                .filter(link -> !wanted.contains(link.getCategory().getId())).toList());
        placeCategoryRepository.flush();
        assignCategories(place, categories.stream().filter(category -> !retained.contains(category.getId())).toList());
    }

    private void apply(Place place, Destination destination, String name, String description, String address,
                       BigDecimal latitude, BigDecimal longitude, Integer priceLevel, boolean bookingEnabled) {
        place.setDestination(destination);
        place.setName(name.trim());
        place.setDescription(description);
        place.setAddress(address);
        place.setLatitude(latitude);
        place.setLongitude(longitude);
        place.setPriceLevel(priceLevel);
        place.setBookingEnabled(bookingEnabled);
    }
}

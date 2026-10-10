package com.travelhub.destination.service;

import com.travelhub.common.exception.BusinessException;
import com.travelhub.common.exception.ResourceNotFoundException;
import com.travelhub.common.response.PageResponse;
import com.travelhub.common.util.Pages;
import com.travelhub.destination.dto.*;
import com.travelhub.destination.entity.Destination;
import com.travelhub.destination.mapper.DestinationMapper;
import com.travelhub.destination.repository.DestinationRepository;
import com.travelhub.destination.repository.DestinationSpecification;
import java.time.DateTimeException;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DestinationService {
    private final DestinationRepository destinationRepository;
    private final DestinationMapper destinationMapper;

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames={"destinationDetails","destinationLists","placeDetails"},allEntries=true)
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('DESTINATION_CREATE')")
    public DestinationResponse create(DestinationCreateRequest request) {
        validateTimezone(request.timezone());
        Destination destination = destinationRepository.saveAndFlush(destinationMapper.toEntity(request));
        log.info("Created destination {}", destination.getId());
        return destinationMapper.toResponse(destination);
    }

    @org.springframework.cache.annotation.Cacheable(cacheNames="destinationDetails",key="#id")
    public DestinationResponse get(Long id) {
        return destinationMapper.toResponse(requireDestination(id));
    }

    @org.springframework.cache.annotation.Cacheable(cacheNames="destinationLists",key="@cacheKeys.key(#keyword,#country,#pageable)")
    public PageResponse<DestinationResponse> search(String keyword, String country, Pageable pageable) {
        Pageable checked = Pages.checked(pageable, "id", "name", "country", "createdAt", "updatedAt");
        return PageResponse.from(destinationRepository.findAll(DestinationSpecification.search(keyword, country), checked)
                .map(destinationMapper::toResponse));
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames={"destinationDetails","destinationLists","placeDetails"},allEntries=true)
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('DESTINATION_UPDATE')")
    public DestinationResponse update(Long id, DestinationUpdateRequest request) {
        Destination destination = requireDestination(id);
        validateTimezone(request.timezone());
        destinationMapper.update(destination, request);
        destinationRepository.flush();
        log.info("Updated destination {}", id);
        return destinationMapper.toResponse(destination);
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames={"destinationDetails","destinationLists","placeDetails"},allEntries=true)
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('DESTINATION_DELETE')")
    public void delete(Long id) {
        Destination destination = requireDestination(id);
        if (destinationRepository.countPlaces(id) > 0) {
            throw new BusinessException("Cannot delete a destination that has places");
        }
        if (destinationRepository.countTrips(id) > 0) {
            throw new BusinessException("Cannot delete a destination that has trips");
        }
        destinationRepository.delete(destination);
        destinationRepository.flush();
        log.info("Deleted destination {}", id);
    }

    private Destination requireDestination(Long id) {
        return destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination", id));
    }

    private void validateTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return;
        }
        try {
            ZoneId.of(timezone.trim());
        } catch (DateTimeException exception) {
            throw new BusinessException("Timezone must be a valid IANA timezone, such as Asia/Ho_Chi_Minh");
        }
    }
}

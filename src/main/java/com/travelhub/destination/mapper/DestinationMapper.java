package com.travelhub.destination.mapper;

import com.travelhub.destination.dto.*;
import com.travelhub.destination.entity.Destination;
import org.springframework.stereotype.Component;

@Component
public class DestinationMapper {
    public Destination toEntity(DestinationCreateRequest request) {
        Destination destination = new Destination();
        destination.setName(request.name().trim());
        destination.setCountry(request.country().trim());
        destination.setDescription(request.description());
        destination.setImageUrl(request.imageUrl());
        destination.setLatitude(request.latitude());
        destination.setLongitude(request.longitude());
        destination.setTimezone(normalizeTimezone(request.timezone()));
        return destination;
    }

    public void update(Destination destination, DestinationUpdateRequest request) {
        destination.setName(request.name().trim());
        destination.setCountry(request.country().trim());
        destination.setDescription(request.description());
        destination.setImageUrl(request.imageUrl());
        destination.setLatitude(request.latitude());
        destination.setLongitude(request.longitude());
        destination.setTimezone(normalizeTimezone(request.timezone()));
    }

    public DestinationResponse toResponse(Destination destination) {
        return new DestinationResponse(destination.getId(), destination.getName(), destination.getCountry(),
                destination.getDescription(), destination.getImageUrl(), destination.getLatitude(),
                destination.getLongitude(), destination.getTimezone(), destination.getCreatedAt(),
                destination.getUpdatedAt());
    }

    public DestinationSummaryResponse toSummary(Destination destination) {
        return new DestinationSummaryResponse(destination.getId(), destination.getName(),
                destination.getCountry(), destination.getImageUrl());
    }

    private String normalizeTimezone(String timezone) {
        return timezone == null || timezone.isBlank() ? null : timezone.trim();
    }
}

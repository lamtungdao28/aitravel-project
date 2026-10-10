package com.travelhub.trip.mapper;

import com.travelhub.trip.dto.*;
import com.travelhub.trip.entity.*;

import org.springframework.stereotype.Component;

@Component
public class TripMapper {
    public TripResponse toResponse(Trip t) {
        return new TripResponse(
                t.getId(),
                t.getUser().getId(),
                t.getDestination() == null ? null : t.getDestination().getId(),
                t.getName(),
                t.getStartDate(),
                t.getEndDate(),
                t.getBudget(),
                t.getDescription(),
                t.getCreatedAt(),
                t.getUpdatedAt());
    }

    public TripPlaceResponse toResponse(TripPlace p) {
        return new TripPlaceResponse(
                p.getTrip().getId(),
                p.getPlace().getId(),
                p.getPlace().getName(),
                p.getVisitDate(),
                p.getNote(),
                p.getSortOrder());
    }
}

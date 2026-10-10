package com.travelhub.trip.dto;
import java.time.LocalDate;
public record TripPlaceResponse(Long tripId, Long placeId, String placeName, LocalDate visitDate, String note, int sortOrder) {}

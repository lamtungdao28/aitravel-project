package com.travelhub.trip.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TripPlaceId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "place_id")
    private Long placeId;
}

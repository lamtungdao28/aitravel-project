package com.travelhub.trip.entity;

import com.travelhub.place.entity.Place;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "trip_places")
@Check(constraints = "sort_order >= 0")
@Getter
@Setter
@NoArgsConstructor
public class TripPlace {
    @EmbeddedId
    private TripPlaceId id = new TripPlaceId();

    @MapsId("tripId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @MapsId("placeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "visit_date")
    private LocalDate visitDate;

    @Column(length = 1000)
    private String note;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}

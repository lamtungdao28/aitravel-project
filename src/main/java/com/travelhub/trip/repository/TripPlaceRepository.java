package com.travelhub.trip.repository;
import com.travelhub.trip.entity.*;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface TripPlaceRepository extends JpaRepository<TripPlace, TripPlaceId> {
    @EntityGraph(attributePaths="place")
    Page<TripPlace> findByTripId(Long tripId, Pageable pageable);
    List<TripPlace> findByTripId(Long tripId);
}

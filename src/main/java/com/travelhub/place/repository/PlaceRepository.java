package com.travelhub.place.repository;

import com.travelhub.place.entity.Place;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlaceRepository
        extends JpaRepository<Place, Long>, JpaSpecificationExecutor<Place> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Place p where p.id = :id")
    Optional<Place> findLockedById(@Param("id") Long id);

    @Query("select count(r) from Review r where r.place.id = :id")
    long countReviews(@Param("id") Long id);

    @Query("select count(b) from Booking b where b.place.id = :id")
    long countBookings(@Param("id") Long id);

    @Query("select count(t) from TripPlace t where t.place.id = :id")
    long countTripPlaces(@Param("id") Long id);

    @Query("select count(f) from Favorite f where f.place.id = :id")
    long countFavorites(@Param("id") Long id);
}

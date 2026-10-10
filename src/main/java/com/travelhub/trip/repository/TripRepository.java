package com.travelhub.trip.repository;
import com.travelhub.trip.entity.Trip;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface TripRepository extends JpaRepository<Trip, Long> {
    Page<Trip> findByUserId(Long userId, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trip t where t.id=:id")
    Optional<Trip> findLockedById(Long id);
    @Query("select t.user.id from Trip t where t.id=:id")
    Optional<Long> findUserId(Long id);
}

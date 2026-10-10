package com.travelhub.booking.repository;

import com.travelhub.booking.entity.*;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

import java.util.Optional;

public interface BookingRepository
        extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {
    @Override
    @EntityGraph(attributePaths = "place")
    Page<Booking> findAll(
            org.springframework.data.jpa.domain.Specification<Booking> specification,
            Pageable pageable);

    @EntityGraph(attributePaths = "place")
    Page<Booking> findByUserId(Long userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id=:id")
    Optional<Booking> findLockedById(Long id);

    @Query("select b.user.id from Booking b where b.id=:id")
    Optional<Long> findUserId(Long id);

    long countByStatus(BookingStatus status);
}

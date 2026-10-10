package com.travelhub.user.repository;

import com.travelhub.user.entity.User;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "role")
    org.springframework.data.domain.Page<User> findAll(
            org.springframework.data.jpa.domain.Specification<User> specification,
            org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "role")
    Optional<User> findByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findLockedById(@Param("id") Long id);

    @Query("select count(r) from Review r where r.user.id = :id")
    long countReviews(@Param("id") Long id);

    @Query("select count(t) from Trip t where t.user.id = :id")
    long countTrips(@Param("id") Long id);

    @Query("select count(b) from Booking b where b.user.id = :id")
    long countBookings(@Param("id") Long id);

    @Query("select count(a) from Address a where a.user.id = :id")
    long countAddresses(@Param("id") Long id);

    @Query("select count(f) from Favorite f where f.user.id = :id")
    long countFavorites(@Param("id") Long id);
}

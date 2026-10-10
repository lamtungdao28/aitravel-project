package com.travelhub.address.repository;

import com.travelhub.address.entity.Address;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    Page<Address> findByUserId(Long userId, Pageable pageable);

    List<Address> findByUserIdAndDefaultAddressTrue(Long userId);

    boolean existsByUserId(Long userId);

    Optional<Address> findFirstByUserIdOrderByCreatedAtAscIdAsc(Long userId);

    @Query("select a.user.id from Address a where a.id = :id")
    Optional<Long> findUserId(Long id);
}

package com.travelhub.destination.repository;

import com.travelhub.destination.entity.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DestinationRepository extends JpaRepository<Destination, Long>, JpaSpecificationExecutor<Destination> {
    @Query("select count(p) from Place p where p.destination.id = :destinationId")
    long countPlaces(@Param("destinationId") Long destinationId);

    @Query("select count(t) from Trip t where t.destination.id = :destinationId")
    long countTrips(@Param("destinationId") Long destinationId);
}

package com.travelhub.place.repository;

import com.travelhub.place.entity.PlaceCategory;
import com.travelhub.place.entity.PlaceCategoryId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PlaceCategoryRepository extends JpaRepository<PlaceCategory, PlaceCategoryId> {
    @Query(
            "select pc from PlaceCategory pc join fetch pc.category where pc.place.id = :placeId"
                + " order by pc.category.name")
    List<PlaceCategory> findCategoriesByPlaceId(@Param("placeId") Long placeId);

    @Query(
            "select pc from PlaceCategory pc join fetch pc.category where pc.place.id in :placeIds"
                + " order by pc.category.name")
    List<PlaceCategory> findCategoriesByPlaceIds(@Param("placeIds") Collection<Long> placeIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from PlaceCategory pc where pc.place.id = :placeId")
    void deleteByPlaceId(@Param("placeId") Long placeId);
}

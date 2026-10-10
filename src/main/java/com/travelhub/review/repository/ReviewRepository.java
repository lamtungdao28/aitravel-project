package com.travelhub.review.repository;

import com.travelhub.review.entity.Review;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {
    @EntityGraph(attributePaths="place")
    Page<Review> findByUserId(Long userId, Pageable pageable);
    @Override
    @EntityGraph(attributePaths = "user")
    Optional<Review> findById(Long id);

    @EntityGraph(attributePaths = "user")
    Page<Review> findByPlaceId(Long placeId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "user")
    Page<Review> findAll(Specification<Review> specification, Pageable pageable);

    @Query("select r.place.id from Review r where r.id = :id")
    Optional<Long> findPlaceId(@Param("id") Long id);

    @Query("select r.user.id from Review r where r.id = :id")
    Optional<Long> findUserId(@Param("id") Long id);

    @Query("select avg(r.rating) as averageRating, count(r) as reviewCount from Review r where r.place.id = :placeId")
    RatingStatistics statistics(@Param("placeId") Long placeId);

    interface RatingStatistics {
        Double getAverageRating();
        long getReviewCount();
    }
}

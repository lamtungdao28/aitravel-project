package com.travelhub.review.repository;

import com.travelhub.review.entity.ReviewImage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {
    List<ReviewImage> findByReviewIdOrderByIdAsc(Long reviewId);

    List<ReviewImage> findByReviewIdInOrderByIdAsc(Collection<Long> reviewIds);

    long countByReviewId(Long reviewId);

    @Query("select i.review.id from ReviewImage i where i.id = :id")
    Optional<Long> findReviewId(@Param("id") Long id);

    @Query("select i.review.user.id from ReviewImage i where i.id = :id")
    Optional<Long> findUserId(@Param("id") Long id);
}

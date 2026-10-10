package com.travelhub.review.mapper;

import com.travelhub.review.dto.ReviewImageResponse;
import com.travelhub.review.dto.ReviewResponse;
import com.travelhub.review.entity.Review;
import com.travelhub.review.entity.ReviewImage;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {
    public ReviewResponse toResponse(Review review, List<ReviewImage> images) {
        return new ReviewResponse(review.getId(), review.getUser().getId(), review.getUser().getFullName(),
                review.getPlace().getId(), review.getRating(), review.getTitle(), review.getContent(),
                images.stream().map(this::toImageResponse).toList(), review.getCreatedAt(), review.getUpdatedAt());
    }

    public ReviewImageResponse toImageResponse(ReviewImage image) {
        return new ReviewImageResponse(image.getId(), image.getImageUrl());
    }
}

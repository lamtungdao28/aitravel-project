package com.travelhub.place.repository;

import com.travelhub.common.util.Pages;
import com.travelhub.place.dto.PlaceFilter;
import com.travelhub.place.entity.Place;
import com.travelhub.place.entity.PlaceCategory;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class PlaceSpecification {
    private PlaceSpecification() {}

    public static Specification<Place> search(PlaceFilter filter) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.keyword() != null && !filter.keyword().isBlank()) {
                String pattern = Pages.contains(filter.keyword());
                predicates.add(
                        builder.or(
                                builder.like(builder.lower(root.get("name")), pattern, '\\'),
                                builder.like(builder.lower(root.get("description")), pattern, '\\'),
                                builder.like(builder.lower(root.get("address")), pattern, '\\')));
            }
            if (filter.destinationId() != null) {
                predicates.add(
                        builder.equal(root.get("destination").get("id"), filter.destinationId()));
            }
            if (filter.categoryId() != null) {
                var categoryMatch = query.subquery(Long.class);
                var assignment = categoryMatch.from(PlaceCategory.class);
                categoryMatch
                        .select(assignment.get("place").get("id"))
                        .where(
                                builder.equal(assignment.get("place").get("id"), root.get("id")),
                                builder.equal(
                                        assignment.get("category").get("id"), filter.categoryId()));
                predicates.add(builder.exists(categoryMatch));
            }
            if (filter.minRating() != null) {
                predicates.add(
                        builder.greaterThanOrEqualTo(
                                root.get("averageRating"), filter.minRating()));
            }
            if (filter.maxPriceLevel() != null) {
                predicates.add(
                        builder.lessThanOrEqualTo(root.get("priceLevel"), filter.maxPriceLevel()));
            }
            if (filter.bookingEnabled() != null) {
                predicates.add(builder.equal(root.get("bookingEnabled"), filter.bookingEnabled()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}

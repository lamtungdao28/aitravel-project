package com.travelhub.destination.repository;

import com.travelhub.common.util.Pages;
import com.travelhub.destination.entity.Destination;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DestinationSpecification {
    private DestinationSpecification() {}

    public static Specification<Destination> search(String keyword, String country) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String pattern = Pages.contains(keyword);
                predicates.add(
                        builder.or(
                                builder.like(builder.lower(root.get("name")), pattern, '\\'),
                                builder.like(builder.lower(root.get("description")), pattern, '\\'),
                                builder.like(builder.lower(root.get("country")), pattern, '\\')));
            }
            if (country != null && !country.isBlank()) {
                predicates.add(
                        builder.equal(
                                builder.lower(root.get("country")),
                                country.trim().toLowerCase(Locale.ROOT)));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}

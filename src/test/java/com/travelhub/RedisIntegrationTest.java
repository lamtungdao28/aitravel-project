package com.travelhub;

import static org.assertj.core.api.Assertions.*;

import com.travelhub.destination.dto.*;
import com.travelhub.destination.service.DestinationService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "travelhub.cache.enabled=true")
class RedisIntegrationTest extends PostgresIntegrationTest {
    @Autowired DestinationService destinations;
    @Autowired CacheManager caches;
    @Autowired com.travelhub.place.service.PlaceService places;
    @Autowired com.travelhub.category.service.CategoryService categories;
    @Autowired com.travelhub.review.service.ReviewService reviews;
    @Autowired com.travelhub.user.service.UserService users;
    @Autowired com.travelhub.ai.service.TravelAiService ai;

    @Test
    void placeAndRatingMutationsEvictRelatedTypedCaches() {
        var d =
                destinations.create(
                        new DestinationCreateRequest(
                                "Ratings", "Vietnam", null, null, null, null, null));
        var c =
                categories.create(
                        new com.travelhub.category.dto.CategoryCreateRequest(
                                "Cached category", "cached-category", null, null));
        var u =
                users.create(
                        new com.travelhub.user.dto.UserCreateRequest(
                                "cache-review@example.test", "test-password", "Reviewer", null));
        var p =
                places.create(
                        new com.travelhub.place.dto.PlaceCreateRequest(
                                d.id(),
                                "Cache Place",
                                null,
                                null,
                                null,
                                null,
                                1,
                                true,
                                java.util.Set.of(c.id())));
        Long reviewId = null;
        var page = org.springframework.data.domain.PageRequest.of(0, 20);
        try {
            assertThat(places.get(p.id()).reviewCount()).isZero();
            assertThat(places.popular(page).content()).hasSize(1);
            assertThat(places.popular(page).content()).hasSize(1);
            assertThat(places.topRated(page).content()).hasSize(1);
            assertThat(ai.summary(p.id()).reviewCount()).isZero();
            assertThat(ai.summary(p.id()).reviewCount()).isZero();
            var review =
                    reviews.create(
                            p.id(),
                            new com.travelhub.review.dto.ReviewCreateRequest(
                                    u.id(), 5, "Great", "A good visit", java.util.List.of()));
            reviewId = review.id();
            assertThat(caches.getCache("placeDetails").get(p.id())).isNull();
            assertThat(caches.getCache("reviewSummaries").get(p.id())).isNull();
            assertThat(places.get(p.id()).averageRating()).isEqualByComparingTo("5");
            assertThat(places.popular(page).content().getFirst().reviewCount()).isEqualTo(1);
            assertThat(places.topRated(page).content().getFirst().averageRating())
                    .isEqualByComparingTo("5");
            categories.update(
                    c.id(),
                    new com.travelhub.category.dto.CategoryUpdateRequest(
                            "Renamed category", "cached-category", null, null));
            assertThat(places.get(p.id()).categories().getFirst().name())
                    .isEqualTo("Renamed category");
            reviews.delete(reviewId);
            reviewId = null;
            assertThat(places.get(p.id()).reviewCount()).isZero();
        } finally {
            if (reviewId != null) reviews.delete(reviewId);
            places.delete(p.id());
            categories.delete(c.id());
            destinations.delete(d.id());
            users.delete(u.id());
        }
    }

    @Test
    void roundTripsTypedJsonAndEvictsAfterCommittedMutation() {
        var created =
                destinations.create(
                        new DestinationCreateRequest(
                                "Cached", "Vietnam", null, null, null, null, null));
        try {
            assertThat(destinations.get(created.id()).name()).isEqualTo("Cached");
            assertThat(
                            caches.getCache("destinationDetails")
                                    .get(created.id(), DestinationResponse.class)
                                    .name())
                    .isEqualTo("Cached");
            destinations.update(
                    created.id(),
                    new DestinationUpdateRequest(
                            "Updated", "Vietnam", null, null, null, null, null));
            assertThat(caches.getCache("destinationDetails").get(created.id())).isNull();
            assertThat(destinations.get(created.id()).name()).isEqualTo("Updated");
            assertThat(destinations.get(created.id()).name()).isEqualTo("Updated");
        } finally {
            destinations.delete(created.id());
        }
    }
}

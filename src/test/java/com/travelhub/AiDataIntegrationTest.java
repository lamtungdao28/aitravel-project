package com.travelhub;

import static org.assertj.core.api.Assertions.*;

import com.travelhub.ai.service.AiDataService;
import com.travelhub.destination.entity.Destination;
import com.travelhub.favorite.entity.*;
import com.travelhub.place.entity.Place;
import com.travelhub.review.entity.Review;
import com.travelhub.trip.entity.Trip;
import com.travelhub.user.entity.User;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestPropertySource(
        properties = {"ai.max-reviews=2", "ai.max-review-chars=50", "ai.max-candidates=2"})
class AiDataIntegrationTest extends PostgresIntegrationTest {
    @Autowired AiDataService data;
    @Autowired EntityManager em;

    @Test
    void boundsReviewInputAndSelectsCandidatesBeforeAi() {
        User u = new User();
        u.setEmail("ai@example.test");
        u.setFullName("AI User");
        u.setPassword("test-only");
        em.persist(u);
        Destination d = new Destination();
        d.setName("Hanoi");
        d.setCountry("Vietnam");
        em.persist(d);
        Place favorite = place(d, "Favorite"),
                first = place(d, "First"),
                second = place(d, "Second");
        Favorite f = new Favorite();
        f.setId(new FavoriteId(u.getId(), favorite.getId()));
        f.setUser(u);
        f.setPlace(favorite);
        em.persist(f);
        Trip trip = new Trip();
        trip.setName("Hanoi Trip");
        trip.setUser(u);
        trip.setDestination(d);
        em.persist(trip);
        for (int i = 0; i < 3; i++) {
            Review r = new Review();
            r.setUser(u);
            r.setPlace(favorite);
            r.setRating(4);
            r.setContent("a".repeat(200));
            em.persist(r);
        }
        em.flush();
        var summary = data.reviewInput(favorite.getId());
        assertThat(summary.reviews())
                .hasSize(2)
                .allSatisfy(r -> assertThat(r.content()).hasSize(50));
        var recommendations = data.recommendationInput(u.getId(), d.getId());
        assertThat(recommendations.candidates())
                .hasSize(2)
                .noneSatisfy(c -> assertThat(c.placeId()).isEqualTo(favorite.getId()));
        assertThat(recommendations.favoritePlaces()).containsExactly("Favorite");
        assertThat(data.recommendationInput(u.getId(), null).candidates()).hasSize(2);
    }

    private Place place(Destination d, String name) {
        Place p = new Place();
        p.setName(name);
        p.setDestination(d);
        em.persist(p);
        return p;
    }
}

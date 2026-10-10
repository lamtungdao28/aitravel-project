package com.travelhub;

import com.travelhub.destination.entity.Destination;
import com.travelhub.favorite.entity.Favorite;
import com.travelhub.favorite.entity.FavoriteId;
import com.travelhub.place.entity.Place;
import com.travelhub.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class FavoriteApiIntegrationTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;

    @Test void addDuplicateListAndIdempotentRemove() throws Exception {
        User user = user("favorite@example.test");
        Place place = place("Lake");
        String path = "/api/v1/users/" + user.getId() + "/favorites";
        mvc.perform(post(path + "/" + place.getId())).andExpect(status().isCreated())
                .andExpect(header().exists("Location")).andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.placeId").value(place.getId()));
        mvc.perform(post(path + "/" + place.getId())).andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Lake"));
        em.flush(); em.clear();
        assertThat(em.find(Favorite.class, new FavoriteId(user.getId(), place.getId()))).isNotNull();
        mvc.perform(delete(path + "/" + place.getId())).andExpect(status().isNoContent());
        mvc.perform(delete(path + "/" + place.getId())).andExpect(status().isNoContent());
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        em.flush(); em.clear();
        assertThat(em.find(Place.class, place.getId())).isNotNull();
        assertThat(em.find(User.class, user.getId())).isNotNull();
    }

    @Test void listsAreUserScopedPaginatedAndSorted() throws Exception {
        User first = user("first-favorite@example.test");
        User second = user("second-favorite@example.test");
        Place alpha = place("Alpha");
        Place beta = place("Beta");
        String firstPath = "/api/v1/users/" + first.getId() + "/favorites";
        String secondPath = "/api/v1/users/" + second.getId() + "/favorites";
        mvc.perform(post(firstPath + "/" + alpha.getId())).andExpect(status().isCreated());
        mvc.perform(post(firstPath + "/" + beta.getId())).andExpect(status().isCreated());
        mvc.perform(post(secondPath + "/" + alpha.getId())).andExpect(status().isCreated());
        mvc.perform(get(firstPath).param("size", "1").param("page", "1").param("sort", "place.name,asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(beta.getId()));
        mvc.perform(get(secondPath)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get(firstPath).param("size", "1000")).andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
        mvc.perform(get(firstPath).param("sort", "user.password,asc")).andExpect(status().isBadRequest());
    }

    @Test void missingResourcesAndInvalidIdentifiersAreRejected() throws Exception {
        User user = user("missing-favorite@example.test");
        Place place = place("Lake");
        mvc.perform(post("/api/v1/users/{userId}/favorites/{placeId}", Long.MAX_VALUE, place.getId()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/users/{userId}/favorites/{placeId}", user.getId(), Long.MAX_VALUE))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/users/{userId}/favorites", Long.MAX_VALUE)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/users/{userId}/favorites/{placeId}", user.getId(), Long.MAX_VALUE))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/users/0/favorites/1")).andExpect(status().isBadRequest());
    }

    private User user(String email) {
        User user = new User(); user.setEmail(email); user.setFullName("Favorite User");
        user.setPassword("phase-eight-test-only"); em.persist(user); return user;
    }
    private Place place(String name) {
        Destination destination = new Destination(); destination.setName("Hanoi"); destination.setCountry("Vietnam"); em.persist(destination);
        Place place = new Place(); place.setName(name); place.setDestination(destination); em.persist(place); return place;
    }
}

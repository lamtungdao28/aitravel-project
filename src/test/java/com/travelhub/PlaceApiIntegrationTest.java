package com.travelhub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelhub.booking.entity.Booking;
import com.travelhub.category.entity.Category;
import com.travelhub.destination.entity.Destination;
import com.travelhub.favorite.entity.Favorite;
import com.travelhub.favorite.entity.FavoriteId;
import com.travelhub.place.entity.Place;
import com.travelhub.place.entity.PlaceCategory;
import com.travelhub.place.entity.PlaceCategoryId;
import com.travelhub.review.entity.Review;
import com.travelhub.trip.entity.Trip;
import com.travelhub.trip.entity.TripPlace;
import com.travelhub.trip.entity.TripPlaceId;
import com.travelhub.user.entity.User;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@AutoConfigureMockMvc
@Transactional
class PlaceApiIntegrationTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;

    @Test
    void placeCrudReplacesCategoryAssignmentsAndKeepsCalculatedFieldsServerOwned()
            throws Exception {
        Destination destination = destination("Hanoi");
        Category nature = category("Nature", "nature");
        Category park = category("Park", "park");
        String response =
                mvc.perform(
                                post("/api/v1/places")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                json.writeValueAsString(
                                                        Map.of(
                                                                "destinationId",
                                                                destination.getId(),
                                                                "name",
                                                                "West Lake",
                                                                "priceLevel",
                                                                0,
                                                                "categoryIds",
                                                                List.of(
                                                                        nature.getId(),
                                                                        park.getId()),
                                                                "averageRating",
                                                                5,
                                                                "reviewCount",
                                                                999))))
                        .andExpect(status().isCreated())
                        .andExpect(header().exists("Location"))
                        .andExpect(jsonPath("$.destinationId").value(destination.getId()))
                        .andExpect(jsonPath("$.averageRating").value(0))
                        .andExpect(jsonPath("$.reviewCount").value(0))
                        .andExpect(jsonPath("$.bookingEnabled").value(false))
                        .andExpect(jsonPath("$.categories.length()").value(2))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long id = json.readTree(response).get("id").asLong();
        em.flush();
        em.clear();

        mvc.perform(get("/api/v1/places/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destination.id").value(destination.getId()))
                .andExpect(jsonPath("$.destination.name").value("Hanoi"))
                .andExpect(jsonPath("$.destination.country").value("Vietnam"))
                .andExpect(jsonPath("$.categories.length()").value(2));
        mvc.perform(
                        put("/api/v1/places/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                Map.of(
                                                        "destinationId",
                                                        destination.getId(),
                                                        "name",
                                                        "Lake Park",
                                                        "priceLevel",
                                                        1,
                                                        "bookingEnabled",
                                                        true,
                                                        "categoryIds",
                                                        List.of(park.getId())))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lake Park"))
                .andExpect(jsonPath("$.priceLevel").value(1))
                .andExpect(jsonPath("$.bookingEnabled").value(true))
                .andExpect(jsonPath("$.categories.length()").value(1))
                .andExpect(jsonPath("$.categories[0].id").value(park.getId()));
        em.flush();
        em.clear();
        assertThat(em.find(PlaceCategory.class, new PlaceCategoryId(id, nature.getId()))).isNull();
        assertThat(em.find(PlaceCategory.class, new PlaceCategoryId(id, park.getId()))).isNotNull();

        mvc.perform(delete("/api/v1/places/{id}", id)).andExpect(status().isNoContent());
        em.flush();
        em.clear();
        assertThat(em.find(PlaceCategory.class, new PlaceCategoryId(id, park.getId()))).isNull();
        mvc.perform(get("/api/v1/places/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void combinedFiltersExcludeNearMatchesAndReturnAccuratePagination() throws Exception {
        Destination hanoi = destination("Hanoi");
        Destination danang = destination("Da Nang");
        Category cafe = category("Cafe", "cafe");
        Category museum = category("Museum", "museum");
        place(hanoi, cafe, "Lake Alpha", "4.50", 10, 2, true);
        Place second = place(hanoi, cafe, "Lake Beta", "4.20", 5, 1, true);
        place(danang, cafe, "Lake Wrong Destination", "4.80", 20, 1, true);
        place(hanoi, museum, "Lake Wrong Category", "4.80", 20, 1, true);
        place(hanoi, cafe, "River Wrong Keyword", "4.80", 20, 1, true);
        place(hanoi, cafe, "Lake Low Rating", "3.90", 20, 1, true);
        place(hanoi, cafe, "Lake Expensive", "4.80", 20, 3, true);
        place(hanoi, cafe, "Lake No Booking", "4.80", 20, 1, false);
        em.flush();
        em.clear();

        mvc.perform(
                        get("/api/v1/places")
                                .param("keyword", "LAKE")
                                .param("destinationId", hanoi.getId().toString())
                                .param("categoryId", cafe.getId().toString())
                                .param("minRating", "4")
                                .param("maxPriceLevel", "2")
                                .param("bookingEnabled", "true")
                                .param("page", "1")
                                .param("size", "1")
                                .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.content[0].id").value(second.getId()))
                .andExpect(jsonPath("$.content[0].destinationId").value(hanoi.getId()))
                .andExpect(jsonPath("$.content[0].averageRating").value(4.2));
    }

    @Test
    void popularAndTopRatedUseTheirOwnRankingAndSupportPagination() throws Exception {
        Destination destination = destination("Hanoi");
        Place mostReviewed = place(destination, null, "Popular", "4.10", 200, 1, false);
        Place highestRated = place(destination, null, "Excellent", "4.90", 10, 2, false);
        place(destination, null, "Average", "3.50", 30, 1, false);
        em.flush();

        mvc.perform(get("/api/v1/places/popular").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].id").value(mostReviewed.getId()));
        mvc.perform(get("/api/v1/places/top-rated").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].id").value(highestRated.getId()));
    }

    @Test
    void missingReferencesAndMissingPlacesReturnNotFound() throws Exception {
        Destination destination = destination("Hanoi");
        mvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                Map.of(
                                                        "destinationId",
                                                        Long.MAX_VALUE,
                                                        "name",
                                                        "Lake",
                                                        "priceLevel",
                                                        0,
                                                        "categoryIds",
                                                        List.of()))))
                .andExpect(status().isNotFound());
        mvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                Map.of(
                                                        "destinationId",
                                                        destination.getId(),
                                                        "name",
                                                        "Lake",
                                                        "priceLevel",
                                                        0,
                                                        "categoryIds",
                                                        List.of(Long.MAX_VALUE)))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/places/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/places/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    void invalidRequestsAndFiltersReturnStructuredBadRequestErrors() throws Exception {
        mvc.perform(post("/api/v1/places").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.destinationId").exists())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.priceLevel").exists())
                .andExpect(jsonPath("$.errors.categoryIds").exists());
        mvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"destinationId":1,"name":"Invalid","priceLevel":5,"categoryIds":[],"latitude":91,"longitude":181}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.priceLevel").exists())
                .andExpect(jsonPath("$.errors.latitude").exists())
                .andExpect(jsonPath("$.errors.longitude").exists());
        mvc.perform(get("/api/v1/places").param("minRating", "6"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/places").param("maxPriceLevel", "-1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/places").param("sort", "destination.password,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void referencedPlacesCannotBeDeleted() throws Exception {
        Destination destination = destination("Hanoi");
        User user = new User();
        user.setEmail("place-reference@example.test");
        user.setFullName("Reference User");
        user.setPassword("phase-five-test-only");
        em.persist(user);
        Place reviewed = place(destination, null, "Reviewed", "0", 0, 0, false);
        Place booked = place(destination, null, "Booked", "0", 0, 1, true);
        Place favorited = place(destination, null, "Favorited", "0", 0, 0, false);
        Place planned = place(destination, null, "Planned", "0", 0, 0, false);
        Review review = new Review();
        review.setUser(user);
        review.setPlace(reviewed);
        review.setRating(5);
        review.setContent("A lovely place");
        em.persist(review);
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setPlace(booked);
        booking.setBookingDate(LocalDate.now().plusDays(10));
        booking.setBookingTime(LocalTime.NOON);
        booking.setNumberOfPeople(2);
        em.persist(booking);
        Favorite favorite = new Favorite();
        favorite.setId(new FavoriteId(user.getId(), favorited.getId()));
        favorite.setUser(user);
        favorite.setPlace(favorited);
        em.persist(favorite);
        Trip trip = new Trip();
        trip.setUser(user);
        trip.setDestination(destination);
        trip.setName("Hanoi Trip");
        em.persist(trip);
        TripPlace itinerary = new TripPlace();
        itinerary.setId(new TripPlaceId(trip.getId(), planned.getId()));
        itinerary.setTrip(trip);
        itinerary.setPlace(planned);
        em.persist(itinerary);
        em.flush();
        em.clear();

        for (Place referenced : List.of(reviewed, booked, favorited, planned)) {
            mvc.perform(delete("/api/v1/places/{id}", referenced.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
            assertThat(em.find(Place.class, referenced.getId())).isNotNull();
        }
    }

    @Test
    void destinationPlacesArePaginatedAndScopedToExistingDestination() throws Exception {
        Destination hanoi = destination("Hanoi");
        Destination danang = destination("Da Nang");
        place(hanoi, null, "Alpha", "0", 0, 0, false);
        Place second = place(hanoi, null, "Beta", "0", 0, 0, false);
        place(danang, null, "Gamma", "0", 0, 0, false);
        em.flush();

        mvc.perform(
                        get("/api/v1/destinations/{id}/places", hanoi.getId())
                                .param("page", "1")
                                .param("size", "1")
                                .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(second.getId()));
        mvc.perform(get("/api/v1/destinations/{id}/places", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    private Destination destination(String name) {
        Destination destination = new Destination();
        destination.setName(name);
        destination.setCountry("Vietnam");
        em.persist(destination);
        return destination;
    }

    private Category category(String name, String slug) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        em.persist(category);
        return category;
    }

    private Place place(
            Destination destination,
            Category category,
            String name,
            String rating,
            int reviewCount,
            int priceLevel,
            boolean bookingEnabled) {
        Place place = new Place();
        place.setDestination(destination);
        place.setName(name);
        place.setAverageRating(new BigDecimal(rating));
        place.setReviewCount(reviewCount);
        place.setPriceLevel(priceLevel);
        place.setBookingEnabled(bookingEnabled);
        em.persist(place);
        if (category != null) {
            PlaceCategory assignment = new PlaceCategory();
            assignment.setId(new PlaceCategoryId(place.getId(), category.getId()));
            assignment.setPlace(place);
            assignment.setCategory(category);
            em.persist(assignment);
        }
        return place;
    }
}

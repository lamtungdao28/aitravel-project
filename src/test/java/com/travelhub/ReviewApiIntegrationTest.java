package com.travelhub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelhub.destination.entity.Destination;
import com.travelhub.place.entity.Place;
import com.travelhub.review.entity.ReviewImage;
import com.travelhub.user.entity.User;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class ReviewApiIntegrationTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;

    @Test void reviewMutationsPersistRoundedAverageAndCountIncludingTheEmptyState() throws Exception {
        long placeId = place("West Lake");
        long first = create(placeId, user("first-review@example.test"), 5, List.of()).get("id").asLong();
        long second = create(placeId, user("second-review@example.test"), 4, List.of()).get("id").asLong();
        long third = create(placeId, user("third-review@example.test"), 1, List.of()).get("id").asLong();
        assertRating(placeId, "3.33", 3);

        mvc.perform(put("/api/v1/reviews/{id}", third).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":3,"title":"Updated title","content":"An updated review"}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rating").value(3))
                .andExpect(jsonPath("$.title").value("Updated title"));
        assertRating(placeId, "4.00", 3);
        mvc.perform(get("/api/v1/reviews/{id}", third)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("An updated review"))
                .andExpect(jsonPath("$.createdAt").exists()).andExpect(jsonPath("$.updatedAt").exists());

        mvc.perform(delete("/api/v1/reviews/{id}", first)).andExpect(status().isNoContent());
        assertRating(placeId, "3.50", 2);
        mvc.perform(delete("/api/v1/reviews/{id}", second)).andExpect(status().isNoContent());
        assertRating(placeId, "3.00", 1);
        mvc.perform(delete("/api/v1/reviews/{id}", third)).andExpect(status().isNoContent());
        assertRating(placeId, "0", 0);
        mvc.perform(get("/api/v1/reviews/{id}", third)).andExpect(status().isNotFound());
    }

    @Test void reviewImagesCanBeAddedRemovedAndAreDeletedWithTheirReview() throws Exception {
        long placeId = place("Museum");
        long userId = user("review-images@example.test");
        JsonNode review = create(placeId, userId, 4,
                List.of("https://example.test/first.jpg", "https://example.test/second.jpg"));
        long reviewId = review.get("id").asLong();
        long firstImageId = review.get("images").get(0).get("id").asLong();
        assertThat(review.get("userId").asLong()).isEqualTo(userId);
        assertThat(review.get("userFullName").asText()).isEqualTo("Review Test User");
        assertThat(review.get("images").size()).isEqualTo(2);

        String added = mvc.perform(post("/api/v1/reviews/{reviewId}/images", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imageUrl":"https://example.test/third.jpg"}
                                """))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.imageUrl").value("https://example.test/third.jpg"))
                .andReturn().getResponse().getContentAsString();
        long addedId = json.readTree(added).get("id").asLong();
        mvc.perform(get("/api/v1/reviews/{id}", reviewId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.images.length()").value(3));
        mvc.perform(delete("/api/v1/review-images/{id}", firstImageId)).andExpect(status().isNoContent());
        em.flush();
        em.clear();
        assertThat(em.find(ReviewImage.class, firstImageId)).isNull();
        mvc.perform(get("/api/v1/reviews/{id}", reviewId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.images.length()").value(2));
        assertRating(placeId, "4", 1);

        mvc.perform(delete("/api/v1/reviews/{id}", reviewId)).andExpect(status().isNoContent());
        em.flush();
        em.clear();
        assertThat(em.find(ReviewImage.class, addedId)).isNull();
        assertThat(em.createQuery("select count(i) from ReviewImage i where i.review.id = :id", Long.class)
                .setParameter("id", reviewId).getSingleResult()).isZero();
        assertRating(placeId, "0", 0);
    }

    @Test void reviewPaginationOnlyIncludesTheRequestedPlace() throws Exception {
        long firstPlace = place("Lake");
        long otherPlace = place("Cafe");
        create(firstPlace, user("review-list-first@example.test"), 5, List.of());
        long expectedId = create(firstPlace, user("review-list-second@example.test"), 4,
                List.of("https://example.test/review.jpg")).get("id").asLong();
        create(otherPlace, user("review-list-other@example.test"), 1, List.of());

        mvc.perform(get("/api/v1/places/{placeId}/reviews", firstPlace)
                        .param("page", "1").param("size", "1").param("sort", "id,asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.content[0].id").value(expectedId))
                .andExpect(jsonPath("$.content[0].placeId").value(firstPlace))
                .andExpect(jsonPath("$.content[0].images.length()").value(1));
        mvc.perform(get("/api/v1/places/{placeId}/reviews", firstPlace).param("size", "1000"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(100));
    }

    @Test void validationRejectsMissingFieldsInvalidRatingsAndMalformedImages() throws Exception {
        long placeId = place("Validation Place");
        long userId = user("review-validation@example.test");
        mvc.perform(post("/api/v1/places/{placeId}/reviews", placeId)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rating").exists()).andExpect(jsonPath("$.errors.content").exists());
        for (int rating : List.of(0, 6)) {
            mvc.perform(post("/api/v1/places/{placeId}/reviews", placeId).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("userId", userId, "rating", rating, "content", "Review"))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.rating").exists());
        }
        long reviewId = create(placeId, userId, 4, List.of()).get("id").asLong();
        mvc.perform(put("/api/v1/reviews/{id}", reviewId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":5,"content":" "}
                                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.content").exists());
        mvc.perform(post("/api/v1/reviews/{id}/images", reviewId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imageUrl":"not-a-url"}
                                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.imageUrl").exists());
        mvc.perform(get("/api/v1/places/{placeId}/reviews", placeId).param("sort", "user.password,asc"))
                .andExpect(status().isBadRequest());
        assertRating(placeId, "4", 1);
    }

    @Test void imageLimitsApplyToInitialAndAdditionalImagesWithoutChangingExistingData() throws Exception {
        long placeId = place("Image Limit Place");
        long userId = user("review-image-limit@example.test");
        List<String> imageUrls = IntStream.range(0, 11).mapToObj(i -> "https://example.test/" + i + ".jpg").toList();
        mvc.perform(post("/api/v1/places/{placeId}/reviews", placeId).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("userId", userId, "rating", 5, "content", "Review",
                                "imageUrls", imageUrls))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.imageUrls").exists());
        assertRating(placeId, "0", 0);
        long reviewId = create(placeId, userId, 5, imageUrls.subList(0, 10)).get("id").asLong();
        mvc.perform(post("/api/v1/reviews/{id}/images", reviewId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imageUrl":"https://example.test/extra.jpg"}
                                """))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/reviews/{id}", reviewId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.images.length()").value(10));
        assertRating(placeId, "5", 1);
    }

    @Test void missingReferencesReviewsAndImagesReturnNotFound() throws Exception {
        long placeId = place("Existing Place");
        long userId = user("review-not-found@example.test");
        long missingId = Long.MAX_VALUE;
        mvc.perform(post("/api/v1/places/{placeId}/reviews", missingId).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("userId", userId, "rating", 4, "content", "Review"))))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/places/{placeId}/reviews", placeId).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("userId", missingId, "rating", 4, "content", "Review"))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/places/{placeId}/reviews", missingId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/reviews/{id}", missingId)).andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/reviews/{id}", missingId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":4,"content":"Review"}
                                """))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/reviews/{id}", missingId)).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/reviews/{id}/images", missingId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imageUrl":"https://example.test/missing.jpg"}
                                """))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/review-images/{id}", missingId)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private JsonNode create(long placeId, long userId, int rating, List<String> imageUrls) throws Exception {
        String response = mvc.perform(post("/api/v1/places/{placeId}/reviews", placeId).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("userId", userId, "rating", rating,
                                "content", "A review for this place", "imageUrls", imageUrls))))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.placeId").value(placeId))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    private long user(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFullName("Review Test User");
        user.setPassword("phase-seven-test-only");
        em.persist(user);
        return user.getId();
    }

    private long place(String name) {
        Destination destination = new Destination();
        destination.setName("Hanoi");
        destination.setCountry("Vietnam");
        em.persist(destination);
        Place place = new Place();
        place.setDestination(destination);
        place.setName(name);
        em.persist(place);
        return place.getId();
    }

    private void assertRating(long placeId, String average, int count) {
        em.flush();
        em.clear();
        Place place = em.find(Place.class, placeId);
        assertThat(place.getAverageRating()).isEqualByComparingTo(average);
        assertThat(place.getReviewCount()).isEqualTo(count);
    }
}

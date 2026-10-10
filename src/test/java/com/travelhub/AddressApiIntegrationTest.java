package com.travelhub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelhub.user.entity.User;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@AutoConfigureMockMvc
@Transactional
class AddressApiIntegrationTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;

    @Test
    void defaultChangesAcrossCreateActionAndUpdateAreIsolatedByUser() throws Exception {
        long userId = user("address-owner@example.test");
        long otherUserId = user("other-address-owner@example.test");
        long first = create(userId, "First", false);
        long second = create(userId, "Second", false);
        long other = create(otherUserId, "Other", false);
        assertDefault(first, true);
        assertDefault(second, false);

        long third = create(userId, "Third", true);
        assertDefault(first, false);
        assertDefault(third, true);
        assertDefaultCount(userId, 1);

        mvc.perform(put("/api/v1/addresses/{id}/default", second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultAddress").value(true));
        assertDefault(third, false);
        assertDefaultCount(userId, 1);

        mvc.perform(
                        put("/api/v1/addresses/{id}", first)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("First Updated", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultAddress").value(true));
        assertDefault(second, false);
        assertDefaultCount(userId, 1);
        assertDefault(other, true);
        assertDefaultCount(otherUserId, 1);
    }

    @Test
    void updatesPersistContactFieldsAndMayClearTheOnlyDefault() throws Exception {
        long userId = user("address-update@example.test");
        long id = create(userId, "Original", false);
        mvc.perform(
                        put("/api/v1/addresses/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                Map.of(
                                                        "recipientName",
                                                        "Updated Recipient",
                                                        "phone",
                                                        "+84 901 234 567",
                                                        "addressLine",
                                                        "25 New Street",
                                                        "city",
                                                        "Da Nang",
                                                        "state",
                                                        "Da Nang",
                                                        "country",
                                                        "Vietnam",
                                                        "postalCode",
                                                        "550000",
                                                        "defaultAddress",
                                                        false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.userId").value(userId));
        em.flush();
        em.clear();

        mvc.perform(get("/api/v1/addresses/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientName").value("Updated Recipient"))
                .andExpect(jsonPath("$.phone").value("+84 901 234 567"))
                .andExpect(jsonPath("$.addressLine").value("25 New Street"))
                .andExpect(jsonPath("$.city").value("Da Nang"))
                .andExpect(jsonPath("$.country").value("Vietnam"))
                .andExpect(jsonPath("$.postalCode").value("550000"))
                .andExpect(jsonPath("$.defaultAddress").value(false))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
        assertDefaultCount(userId, 0);
    }

    @Test
    void deletingDefaultPromotesOldestRemainingAndDeletingLastLeavesAnEmptyList() throws Exception {
        long userId = user("address-delete@example.test");
        long oldest = create(userId, "Oldest", false);
        long middle = create(userId, "Middle", false);
        long newest = create(userId, "Newest", true);

        mvc.perform(delete("/api/v1/addresses/{id}", newest)).andExpect(status().isNoContent());
        assertDefault(oldest, true);
        assertDefault(middle, false);
        assertDefaultCount(userId, 1);
        mvc.perform(get("/api/v1/addresses/{id}", newest)).andExpect(status().isNotFound());

        mvc.perform(delete("/api/v1/addresses/{id}", middle)).andExpect(status().isNoContent());
        assertDefault(oldest, true);
        mvc.perform(delete("/api/v1/addresses/{id}", oldest)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/users/{userId}/addresses", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void addressListsArePaginatedAndNeverIncludeOtherUsers() throws Exception {
        long userId = user("address-list@example.test");
        long otherUserId = user("address-list-other@example.test");
        create(userId, "First", false);
        long second = create(userId, "Second", false);
        create(userId, "Third", false);
        create(otherUserId, "Other", false);

        mvc.perform(
                        get("/api/v1/users/{userId}/addresses", userId)
                                .param("page", "1")
                                .param("size", "1")
                                .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(second))
                .andExpect(jsonPath("$.content[0].userId").value(userId));
        mvc.perform(get("/api/v1/users/{userId}/addresses", userId).param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void invalidAddressFieldsReturnStructuredValidationErrors() throws Exception {
        long userId = user("address-validation@example.test");
        mvc.perform(
                        post("/api/v1/users/{userId}/addresses", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.recipientName").exists())
                .andExpect(jsonPath("$.errors.phone").exists())
                .andExpect(jsonPath("$.errors.addressLine").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/users/" + userId + "/addresses"));
        mvc.perform(
                        post("/api/v1/users/{userId}/addresses", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                Map.of(
                                                        "recipientName",
                                                        "Recipient",
                                                        "phone",
                                                        "0901234567",
                                                        "addressLine",
                                                        "x".repeat(501)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.addressLine").exists());
        long id = create(userId, "Valid", false);
        mvc.perform(
                        put("/api/v1/addresses/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.recipientName").exists());
        mvc.perform(
                        get("/api/v1/users/{userId}/addresses", userId)
                                .param("sort", "user.password,asc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingUsersAndAddressesReturnNotFound() throws Exception {
        long missingId = Long.MAX_VALUE;
        mvc.perform(
                        post("/api/v1/users/{userId}/addresses", missingId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("Missing", false)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/users/{userId}/addresses", missingId))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/addresses/{id}", missingId)).andExpect(status().isNotFound());
        mvc.perform(
                        put("/api/v1/addresses/{id}", missingId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("Missing", false)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/addresses/{id}/default", missingId))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/addresses/{id}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private long user(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFullName("Address Test User");
        user.setPassword("phase-six-test-only");
        em.persist(user);
        return user.getId();
    }

    private long create(long userId, String recipientName, boolean defaultAddress)
            throws Exception {
        String response =
                mvc.perform(
                                post("/api/v1/users/{userId}/addresses", userId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body(recipientName, defaultAddress)))
                        .andExpect(status().isCreated())
                        .andExpect(header().exists("Location"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    private String body(String recipientName, boolean defaultAddress) throws Exception {
        return json.writeValueAsString(
                Map.of(
                        "recipientName",
                        recipientName,
                        "phone",
                        "0901234567",
                        "addressLine",
                        "12 Lake Street",
                        "city",
                        "Hanoi",
                        "country",
                        "Vietnam",
                        "defaultAddress",
                        defaultAddress));
    }

    private void assertDefault(long id, boolean expected) throws Exception {
        em.flush();
        em.clear();
        mvc.perform(get("/api/v1/addresses/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultAddress").value(expected));
    }

    private void assertDefaultCount(long userId, long expected) {
        em.flush();
        em.clear();
        long count =
                em.createQuery(
                                "select count(a) from Address a where a.user.id = :userId and"
                                    + " a.defaultAddress = true",
                                Long.class)
                        .setParameter("userId", userId)
                        .getSingleResult();
        assertThat(count).isEqualTo(expected);
    }
}

package com.travelhub;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.travelhub.destination.entity.Destination;
import com.travelhub.place.entity.Place;
import com.travelhub.security.entity.RefreshToken;
import com.travelhub.user.entity.*;
import com.travelhub.user.repository.*;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Transactional
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@WithAnonymousUser
class SecurityIntegrationTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired RolePermissionRepository links;
    @Autowired PasswordEncoder encoder;
    private static final String PASSWORD = "security-test-password";

    @Test
    void registrationHashesPasswordAndNeverAcceptsRoleEscalation() throws Exception {
        var token = register("member@example.test");
        var user = users.findByEmail("member@example.test").orElseThrow();
        assertThat(user.getRole().getName()).isEqualTo("USER");
        assertThat(user.getPassword()).isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, user.getPassword())).isTrue();
        mvc.perform(get("/api/v1/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.role").value("USER"));
        mvc.perform(
                        post("/api/v1/auth/login")
                                .contentType("application/json")
                                .content(
                                        json.writeValueAsString(
                                                Map.of(
                                                        "email",
                                                        "MEMBER@example.test",
                                                        "password",
                                                        PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
        mvc.perform(
                        post("/api/v1/auth/login")
                                .contentType("application/json")
                                .content(
                                        json.writeValueAsString(
                                                Map.of(
                                                        "email",
                                                        "member@example.test",
                                                        "password",
                                                        "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void anonymousAndMembersCannotAccessAdministration() throws Exception {
        mvc.perform(get("/api/v1/destinations")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.path").value("/api/v1/me"));
        var token = register("restricted@example.test");
        mvc.perform(get("/api/v1/admin/dashboard/overview").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/v1/destinations")
                                .header("Authorization", bearer(token))
                                .contentType("application/json")
                                .content("{\"name\":\"Hanoi\",\"country\":\"Vietnam\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer malformed"))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        get("/api/v1/me")
                                .header(
                                        "Authorization",
                                        "Bearer " + token.path("refreshToken").asText()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void personalResourcesRejectCrossUserAccessAndIgnoreSpoofedUserId() throws Exception {
        var a = register("first@example.test");
        var b = register("second@example.test");
        long first = users.findByEmail("first@example.test").orElseThrow().getId();
        long second = users.findByEmail("second@example.test").orElseThrow().getId();
        long place = place();
        var review =
                postJson(
                        "/api/v1/places/" + place + "/reviews",
                        a,
                        Map.of("userId", second, "rating", 5, "content", "Great visit"));
        assertThat(review.path("userId").asLong()).isEqualTo(first);
        mvc.perform(
                        delete("/api/v1/reviews/" + review.path("id").asLong())
                                .header("Authorization", bearer(b)))
                .andExpect(status().isForbidden());
        var image =
                postJson(
                        "/api/v1/reviews/" + review.path("id").asLong() + "/images",
                        a,
                        Map.of("imageUrl", "https://example.com/image.jpg"));
        mvc.perform(
                        delete("/api/v1/review-images/" + image.path("id").asLong())
                                .header("Authorization", bearer(b)))
                .andExpect(status().isForbidden());
        var address =
                postJson(
                        "/api/v1/me/addresses",
                        a,
                        Map.of(
                                "recipientName",
                                "First",
                                "phone",
                                "12345678",
                                "addressLine",
                                "Street"));
        mvc.perform(
                        get("/api/v1/addresses/" + address.path("id").asLong())
                                .header("Authorization", bearer(b)))
                .andExpect(status().isForbidden());
        var trip = postJson("/api/v1/trips", a, Map.of("userId", second, "name", "My Trip"));
        assertThat(trip.path("userId").asLong()).isEqualTo(first);
        mvc.perform(
                        get("/api/v1/trips/" + trip.path("id").asLong() + "/places")
                                .header("Authorization", bearer(b)))
                .andExpect(status().isForbidden());
        var booking =
                postJson(
                        "/api/v1/bookings",
                        a,
                        Map.of(
                                "userId",
                                second,
                                "placeId",
                                place,
                                "bookingDate",
                                LocalDate.now().plusDays(2).toString(),
                                "bookingTime",
                                "19:00",
                                "numberOfPeople",
                                2));
        assertThat(booking.path("userId").asLong()).isEqualTo(first);
        mvc.perform(
                        get("/api/v1/bookings/" + booking.path("id").asLong())
                                .header("Authorization", bearer(b)))
                .andExpect(status().isForbidden());
        mvc.perform(
                        put("/api/v1/bookings/" + booking.path("id").asLong() + "/confirm")
                                .header("Authorization", bearer(a)))
                .andExpect(status().isForbidden());
        mvc.perform(
                        put("/api/v1/bookings/" + booking.path("id").asLong() + "/cancel")
                                .header("Authorization", bearer(a)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/me/favorites/" + place).header("Authorization", bearer(a)))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/me/favorites").header("Authorization", bearer(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/users/" + first + "/favorites").header("Authorization", bearer(b)))
                .andExpect(status().isForbidden());
        mvc.perform(
                        get("/api/v1/users/" + first + "/recommendations")
                                .header("Authorization", bearer(b)))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshTokensRotateAndExpiredOrReusedTokensAreRejected() throws Exception {
        var token = register("refresh@example.test");
        var next =
                body(
                        mvc.perform(
                                        post("/api/v1/auth/refresh")
                                                .contentType("application/json")
                                                .content(
                                                        json.writeValueAsString(
                                                                Map.of(
                                                                        "refreshToken",
                                                                        token.path("refreshToken")
                                                                                .asText()))))
                                .andExpect(status().isOk()));
        assertThat(next.path("refreshToken").asText())
                .isNotEqualTo(token.path("refreshToken").asText());
        mvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType("application/json")
                                .content(
                                        json.writeValueAsString(
                                                Map.of(
                                                        "refreshToken",
                                                        token.path("refreshToken").asText()))))
                .andExpect(status().isUnauthorized());
        var active =
                em.createQuery(
                                "select t from RefreshToken t where t.revoked=false",
                                RefreshToken.class)
                        .getSingleResult();
        assertThat(active.getTokenHash())
                .hasSize(64)
                .isNotEqualTo(next.path("refreshToken").asText());
        active.setExpiresAt(Instant.now().minusSeconds(1));
        em.flush();
        mvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType("application/json")
                                .content(
                                        json.writeValueAsString(
                                                Map.of(
                                                        "refreshToken",
                                                        next.path("refreshToken").asText()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleAndPermissionChangesAffectExistingAccessTokens() throws Exception {
        var admin = register("admin@example.test");
        var member = register("roles@example.test");
        var user = users.findByEmail("admin@example.test").orElseThrow();
        user.setRole(roles.findByName("ADMIN").orElseThrow());
        em.flush();
        mvc.perform(get("/api/v1/admin/dashboard/overview").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        var custom =
                postJson(
                        "/api/v1/admin/roles",
                        admin,
                        Map.of("name", "READER", "description", "Read own profile"));
        long permission =
                em.createQuery("select p.id from Permission p where p.name='USER_READ'", Long.class)
                        .getSingleResult();
        String rolePath = "/api/v1/admin/roles/" + custom.path("id").asLong() + "/permissions";
        String permissions = json.writeValueAsString(Map.of("permissionIds", List.of(permission)));
        mvc.perform(
                        put(rolePath)
                                .header("Authorization", bearer(admin))
                                .contentType("application/json")
                                .content(permissions))
                .andExpect(status().isOk());
        mvc.perform(
                        put(rolePath)
                                .header("Authorization", bearer(admin))
                                .contentType("application/json")
                                .content(permissions))
                .andExpect(status().isOk());
        var target = users.findByEmail("roles@example.test").orElseThrow();
        mvc.perform(
                        put("/api/v1/admin/users/" + target.getId() + "/role")
                                .header("Authorization", bearer(admin))
                                .contentType("application/json")
                                .content(
                                        json.writeValueAsString(
                                                Map.of("roleId", custom.path("id").asLong()))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/me").header("Authorization", bearer(member)))
                .andExpect(status().isOk());
        mvc.perform(
                        put(rolePath)
                                .header("Authorization", bearer(admin))
                                .contentType("application/json")
                                .content("{\"permissionIds\":[]}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/me").header("Authorization", bearer(member)))
                .andExpect(status().isForbidden());
        user.setRole(roles.findByName("USER").orElseThrow());
        em.flush();
        mvc.perform(get("/api/v1/admin/dashboard/overview").header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletingAccountInvalidatesAccessTokenAndRemovesRefreshTokens() throws Exception {
        var token = register("delete-account@example.test");
        mvc.perform(delete("/api/v1/me").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        em.clear();
        mvc.perform(get("/api/v1/me").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
        assertThat(
                        em.createQuery("select count(t) from RefreshToken t", Long.class)
                                .getSingleResult())
                .isZero();
    }

    private JsonNode register(String email) throws Exception {
        return body(
                mvc.perform(
                                post("/api/v1/auth/register")
                                        .contentType("application/json")
                                        .content(
                                                json.writeValueAsString(
                                                        Map.of(
                                                                "email",
                                                                email,
                                                                "password",
                                                                PASSWORD,
                                                                "fullName",
                                                                "Member",
                                                                "role",
                                                                "ADMIN"))))
                        .andExpect(status().isCreated()));
    }

    private JsonNode postJson(String path, JsonNode token, Object request) throws Exception {
        return body(
                mvc.perform(
                                post(path)
                                        .header("Authorization", bearer(token))
                                        .contentType("application/json")
                                        .content(json.writeValueAsString(request)))
                        .andExpect(status().isCreated()));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private String bearer(JsonNode token) {
        return "Bearer " + token.path("accessToken").asText();
    }

    private long place() {
        Destination d = new Destination();
        d.setName("Hanoi");
        d.setCountry("Vietnam");
        em.persist(d);
        Place p = new Place();
        p.setDestination(d);
        p.setName("Cafe");
        p.setBookingEnabled(true);
        em.persist(p);
        em.flush();
        return p.getId();
    }
}

package com.travelhub;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelhub.destination.entity.Destination;
import com.travelhub.place.entity.Place;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class DestinationApiIntegrationTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;

    @Test
    void destinationCrudAndFilteredPagination() throws Exception {
        String response =
                mvc.perform(
                                post("/api/v1/destinations")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"name":"Da Nang","country":"Vietnam","timezone":"Asia/Ho_Chi_Minh"}
                                                """))
                        .andExpect(status().isCreated())
                        .andExpect(header().exists("Location"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long id = json.readTree(response).get("id").asLong();
        mvc.perform(get("/api/v1/destinations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Da Nang"));
        mvc.perform(
                        get("/api/v1/destinations")
                                .param("keyword", "DA")
                                .param("country", "vietnam")
                                .param("size", "10")
                                .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Da Nang"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
        mvc.perform(
                        put("/api/v1/destinations/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"name":"Danang","country":"Vietnam"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Danang"));
        mvc.perform(delete("/api/v1/destinations/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/destinations/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void invalidBodiesAndSortsHaveConsistentErrors() throws Exception {
        mvc.perform(
                        post("/api/v1/destinations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/destinations"));
        mvc.perform(
                        post("/api/v1/destinations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/api/v1/destinations").param("sort", "unknown,desc"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/v1/destinations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"name":"Paris","country":"France","timezone":"Invalid/Zone"}
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void destinationWithPlacesCannotBeDeleted() throws Exception {
        Destination destination = new Destination();
        destination.setName("Hanoi");
        destination.setCountry("Vietnam");
        em.persist(destination);
        Place place = new Place();
        place.setName("Lake");
        place.setDestination(destination);
        em.persist(place);
        em.flush();
        mvc.perform(delete("/api/v1/destinations/{id}", destination.getId()))
                .andExpect(status().isBadRequest());
    }
}

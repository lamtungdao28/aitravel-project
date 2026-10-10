package com.travelhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelhub.category.entity.Category;
import com.travelhub.destination.entity.Destination;
import com.travelhub.place.entity.Place;
import com.travelhub.place.entity.PlaceCategory;
import com.travelhub.place.entity.PlaceCategoryId;
import jakarta.persistence.EntityManager;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class CategoryApiIntegrationTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;

    @Test void categoryCrudNormalizesInputAndPreservesDetails() throws Exception {
        String response = mvc.perform(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  Outdoor Activities  ","slug":"Outdoor-Activities",
                                 "description":"Activities outside","imageUrl":"https://example.test/outdoors.jpg"}
                                """))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Outdoor Activities"))
                .andExpect(jsonPath("$.slug").value("outdoor-activities"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(response).get("id").asLong();

        em.flush();
        em.clear();
        mvc.perform(get("/api/v1/categories/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Activities outside"))
                .andExpect(jsonPath("$.imageUrl").value("https://example.test/outdoors.jpg"));

        mvc.perform(put("/api/v1/categories/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Outdoor Activities","slug":"outdoor-activities","description":"Updated description"}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.description").value("Updated description"));
        mvc.perform(put("/api/v1/categories/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  Nature  ","slug":"NATURE"}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Nature"))
                .andExpect(jsonPath("$.slug").value("nature"));
        em.flush();
        em.clear();
        assertThat(em.find(Category.class, id).getSlug()).isEqualTo("nature");

        mvc.perform(delete("/api/v1/categories/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/categories/{id}", id)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/v1/categories/" + id));
    }

    @Test void categoriesSupportStablePaginationAndAllowedSorting() throws Exception {
        createCategory("Beach", "beach");
        createCategory("Cafe", "cafe");
        createCategory("Museum", "museum");

        mvc.perform(get("/api/v1/categories").param("page", "1").param("size", "1").param("sort", "name,asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Cafe"))
                .andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(false)).andExpect(jsonPath("$.last").value(false));
        mvc.perform(get("/api/v1/categories").param("size", "1000").param("sort", "slug,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.content[0].slug").value("museum"));
        mvc.perform(get("/api/v1/categories").param("sort", "id,asc"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/categories").param("sort", "description,asc"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test void requiredFieldsAndMalformedBodiesHaveStructuredErrors() throws Exception {
        mvc.perform(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.slug").exists()).andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/categories"));
        mvc.perform(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        long id = createCategory("Nature", "nature");
        mvc.perform(put("/api/v1/categories/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" ","slug":"nature"}
                                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.name").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {"two words", "-nature", "nature-", "nature--park", "nature/park", "nature_park"})
    void malformedSlugsAreRejected(String slug) throws Exception {
        mvc.perform(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "Nature", "slug", slug))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.slug").exists());
    }

    @Test void duplicateNamesAreComparedAfterTrimmingWithoutCaseSensitivity() throws Exception {
        createCategory("Nature", "nature");
        mvc.perform(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  NATURE  ","slug":"outdoor"}
                                """))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    }

    @Test void duplicateNormalizedSlugsAreRejected() throws Exception {
        createCategory("Nature", "nature");
        mvc.perform(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Outdoor","slug":"NATURE"}
                                """))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    }

    @Test void updateCannotTakeAnotherCategoryNameOrSlug() throws Exception {
        createCategory("Nature", "nature");
        long id = createCategory("Beach", "beach");
        mvc.perform(put("/api/v1/categories/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"NATURE","slug":"beach"}
                                """))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/v1/categories/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Beach","slug":"NATURE"}
                                """))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/categories/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Beach")).andExpect(jsonPath("$.slug").value("beach"));
    }

    @Test void missingCategoriesReturnNotFoundForEveryItemOperation() throws Exception {
        long missingId = Long.MAX_VALUE;
        mvc.perform(get("/api/v1/categories/{id}", missingId)).andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/categories/{id}", missingId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Nature","slug":"nature"}
                                """))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/categories/{id}", missingId)).andExpect(status().isNotFound());
    }

    @Test void assignedCategoriesCannotBeDeletedAndKeepTheirAssociation() throws Exception {
        long id = createCategory("Nature", "nature");
        Category category = em.find(Category.class, id);
        Destination destination = new Destination();
        destination.setName("Hanoi");
        destination.setCountry("Vietnam");
        em.persist(destination);
        Place place = new Place();
        place.setName("West Lake");
        place.setDestination(destination);
        em.persist(place);
        PlaceCategory assignment = new PlaceCategory();
        assignment.setId(new PlaceCategoryId(place.getId(), id));
        assignment.setPlace(place);
        assignment.setCategory(category);
        em.persist(assignment);
        em.flush();
        em.clear();

        mvc.perform(delete("/api/v1/categories/{id}", id)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        assertThat(em.find(Category.class, id)).isNotNull();
        assertThat(em.find(PlaceCategory.class, assignment.getId())).isNotNull();
    }

    private long createCategory(String name, String slug) throws Exception {
        String response = mvc.perform(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", name, "slug", slug))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }
}

package com.travelhub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelhub.address.entity.Address;
import com.travelhub.user.entity.User;
import com.travelhub.user.repository.RoleRepository;
import com.travelhub.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class UserApiIntegrationTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired EntityManager entityManager;

    @Test
    void crudNormalizesEmailAndNeverExposesPassword() throws Exception {
        String response =
                mvc.perform(
                                post("/api/v1/users")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"email":"MEMBER@example.test","password":"test-password","fullName":"Member"}
                                                """))
                        .andExpect(status().isCreated())
                        .andExpect(header().exists("Location"))
                        .andExpect(jsonPath("$.email").value("member@example.test"))
                        .andExpect(jsonPath("$.role").value("USER"))
                        .andExpect(jsonPath("$.password").doesNotExist())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long id = json.readTree(response).get("id").asLong();
        mvc.perform(get("/api/v1/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(
                        put("/api/v1/users/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"updated@example.test\",\"fullName\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated"))
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(delete("/api/v1/users/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/users/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void emailUniquenessUsesNormalizedValue() throws Exception {
        createUser("member@example.test");
        mvc.perform(
                        post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"MEMBER@example.test","password":"test-password","fullName":"Duplicate"}
                                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void validationRejectsInvalidProfileData() throws Exception {
        mvc.perform(
                        post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"invalid\",\"password\":\"tiny\",\"fullName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.fullName").exists());
    }

    @Test
    void referenceRolesExistAndCannotBeOverriddenThroughCreateBody() throws Exception {
        assertThat(roleRepository.findByName("USER")).isPresent();
        assertThat(roleRepository.findByName("OWNER")).isPresent();
        assertThat(roleRepository.findByName("ADMIN")).isPresent();
        mvc.perform(
                        post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"role@example.test","password":"test-password","fullName":"Member","role":"ADMIN"}
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void deletionRejectsDependentAddresses() throws Exception {
        User user = createUser("address-owner@example.test");
        Address address = new Address();
        address.setUser(user);
        address.setRecipientName("Member");
        address.setPhone("0123456789");
        address.setAddressLine("1 Main Street");
        entityManager.persist(address);
        entityManager.flush();
        mvc.perform(delete("/api/v1/users/{id}", user.getId())).andExpect(status().isBadRequest());
        assertThat(userRepository.existsById(user.getId())).isTrue();
    }

    private User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("test-password");
        user.setFullName("Member");
        user.setRole(roleRepository.findByName("USER").orElseThrow());
        return userRepository.saveAndFlush(user);
    }
}

package com.travelhub;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.travelhub.admin.service.AdminService;
import com.travelhub.user.entity.User;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class AdminIntegrationTest extends PostgresIntegrationTest {
    @Autowired AdminService admin;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;

    @Test
    void aggregatesExistingDataAndFiltersManagementLists() throws Exception {
        long before = admin.overview().totalUsers();
        User u = new User();
        u.setEmail("dashboard@example.test");
        u.setFullName("Dashboard User");
        u.setPassword("never-return-this");
        em.persist(u);
        em.flush();
        assertThat(admin.overview().totalUsers()).isEqualTo(before + 1);
        assertThat(admin.userStatistics().newUsersLast30Days()).isGreaterThanOrEqualTo(1);
        mvc.perform(get("/api/v1/admin/users").param("keyword", "dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].password").doesNotExist());
        for (String path : new String[] {"overview", "users", "bookings", "places"})
            mvc.perform(get("/api/v1/admin/dashboard/" + path)).andExpect(status().isOk());
        mvc.perform(
                        get("/api/v1/admin/bookings")
                                .param("dateFrom", "2030-01-02")
                                .param("dateTo", "2030-01-01"))
                .andExpect(status().isBadRequest());
    }
}

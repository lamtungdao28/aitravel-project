package com.travelhub;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class BusinessApiIntegrationTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void validatesBookingAndTripBodiesAndPreservesFrameworkStatuses() throws Exception {
        mvc.perform(post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.bookingDate").exists())
                .andExpect(jsonPath("$.errors.numberOfPeople").exists());
        mvc.perform(
                        post("/api/v1/trips")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"budget\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.budget").exists());
        mvc.perform(get("/api/v1/trips/9223372036854775807")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/places/top-rated")).andExpect(status().isMethodNotAllowed());
    }
}

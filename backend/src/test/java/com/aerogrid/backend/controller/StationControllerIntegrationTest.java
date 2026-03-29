package com.aerogrid.backend.controller;

import com.aerogrid.backend.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StationControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should return all stations from the test database script")
    @Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldReturnAllStations() throws Exception {
        mockMvc.perform(get("/api/v1/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // We inserted 2 stations
                .andExpect(jsonPath("$[0].code", is("OFF-001")))
                .andExpect(jsonPath("$[0].name", is("Central Park Station")))
                .andExpect(jsonPath("$[1].code", is("AG-A1B2C3D4")))
                .andExpect(jsonPath("$[1].name", is("Citizen Station 1")));
    }

    @Test
    @DisplayName("Should return specific station by ID")
    @Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldReturnStationById() throws Exception {
        mockMvc.perform(get("/api/v1/stations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("OFF-001")))
                .andExpect(jsonPath("$.name", is("Central Park Station")))
                .andExpect(jsonPath("$.municipality", is("New York")));
    }
}


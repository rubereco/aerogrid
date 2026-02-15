package com.aerogrid.backend.controller;

import com.aerogrid.backend.ingestion.citizen.CitizenIngestionService;
import com.aerogrid.backend.ingestion.citizen.StationIngestionDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for StationIngestionController.
 * Tests all endpoint scenarios including successful ingestion and error cases.
 */
@WebMvcTest
@AutoConfigureMockMvc
class StationIngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CitizenIngestionService ingestionService;

    private StationIngestionDto validDto;
    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String VALID_API_KEY = "sk_live_12345";
    private static final String ENDPOINT = "/api/v1/ingest";

    @BeforeEach
    void setUp() {
        validDto = new StationIngestionDto();
        validDto.setPollutant("NO2");
        validDto.setValue(45.5);
    }

    @Test
    @DisplayName("Should return 200 OK when data is valid and API key is correct")
    void testIngestMeasurement_Success() throws Exception {
        doNothing().when(ingestionService).processIngestion(eq(VALID_API_KEY), any(StationIngestionDto.class));

        mockMvc.perform(post(ENDPOINT)
                        .header(API_KEY_HEADER, VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isOk())
                .andExpect(content().string("Data accepted"));
    }

    @Test
    @DisplayName("Should return 401 UNAUTHORIZED when API key is invalid")
    void testIngestMeasurement_InvalidApiKey() throws Exception {
        doThrow(new SecurityException("Invalid API Key"))
                .when(ingestionService).processIngestion(eq("invalid_key"), any(StationIngestionDto.class));

        mockMvc.perform(post(ENDPOINT)
                        .header(API_KEY_HEADER, "invalid_key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid or inactive API Key"));
    }

    @Test
    @DisplayName("Should return 401 UNAUTHORIZED when API key is inactive")
    void testIngestMeasurement_InactiveApiKey() throws Exception {
        doThrow(new SecurityException("API Key is inactive"))
                .when(ingestionService).processIngestion(eq(VALID_API_KEY), any(StationIngestionDto.class));

        mockMvc.perform(post(ENDPOINT)
                        .header(API_KEY_HEADER, VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid or inactive API Key"));
    }

    @Test
    @DisplayName("Should return 400 BAD REQUEST when pollutant is unknown")
    void testIngestMeasurement_UnknownPollutant() throws Exception {
        StationIngestionDto invalidDto = new StationIngestionDto();
        invalidDto.setPollutant("UNKNOWN_POLLUTANT");
        invalidDto.setValue(45.5);

        doThrow(new IllegalArgumentException("Unknown or null pollutant: UNKNOWN_POLLUTANT"))
                .when(ingestionService).processIngestion(eq(VALID_API_KEY), any(StationIngestionDto.class));

        mockMvc.perform(post(ENDPOINT)
                        .header(API_KEY_HEADER, VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid data: Unknown or null pollutant: UNKNOWN_POLLUTANT"));
    }

    @Test
    @DisplayName("Should return 400 BAD REQUEST when pollutant is null")
    void testIngestMeasurement_NullPollutant() throws Exception {
        StationIngestionDto invalidDto = new StationIngestionDto();
        invalidDto.setPollutant(null);
        invalidDto.setValue(45.5);

        doThrow(new IllegalArgumentException("Unknown or null pollutant: null"))
                .when(ingestionService).processIngestion(eq(VALID_API_KEY), any(StationIngestionDto.class));

        mockMvc.perform(post(ENDPOINT)
                        .header(API_KEY_HEADER, VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid data: Unknown or null pollutant: null"));
    }

    @Test
    @DisplayName("Should return 500 INTERNAL SERVER ERROR when database error occurs")
    void testIngestMeasurement_DatabaseError() throws Exception {
        doThrow(new RuntimeException("Database error"))
                .when(ingestionService).processIngestion(eq(VALID_API_KEY), any(StationIngestionDto.class));

        mockMvc.perform(post(ENDPOINT)
                        .header(API_KEY_HEADER, VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Server error: Database error"));
    }

    @Test
    @DisplayName("Should return 400 BAD REQUEST when API key header is missing")
    void testIngestMeasurement_MissingApiKeyHeader() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 BAD REQUEST when request body is invalid JSON")
    void testIngestMeasurement_InvalidJson() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .header(API_KEY_HEADER, VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should accept different pollutant types")
    void testIngestMeasurement_DifferentPollutants() throws Exception {
        String[] pollutants = {"NO2", "PM10", "PM2_5", "O3", "SO2", "CO"};

        for (String pollutant : pollutants) {
            StationIngestionDto dto = new StationIngestionDto();
            dto.setPollutant(pollutant);
            dto.setValue(50.0);

            doNothing().when(ingestionService).processIngestion(eq(VALID_API_KEY), any(StationIngestionDto.class));

            mockMvc.perform(post(ENDPOINT)
                            .header(API_KEY_HEADER, VALID_API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Data accepted"));
        }
    }

    @Test
    @DisplayName("Should accept various valid measurement values")
    void testIngestMeasurement_DifferentValues() throws Exception {
        Double[] values = {0.0, 1.5, 50.0, 100.0, 999.99};

        for (Double value : values) {
            StationIngestionDto dto = new StationIngestionDto();
            dto.setPollutant("NO2");
            dto.setValue(value);

            doNothing().when(ingestionService).processIngestion(eq(VALID_API_KEY), any(StationIngestionDto.class));

            mockMvc.perform(post(ENDPOINT)
                            .header(API_KEY_HEADER, VALID_API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Data accepted"));
        }
    }
}


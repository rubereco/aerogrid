package com.aerogrid.backend.controller;

import com.aerogrid.backend.controller.dto.StationMapDto;
import com.aerogrid.backend.controller.mapper.StationMapper;
import com.aerogrid.backend.domain.Station;
import com.aerogrid.backend.domain.SourceType;
import com.aerogrid.backend.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for StationController.
 * Tests all endpoints with various scenarios including success cases, edge cases, and error conditions.
 */
@WebMvcTest(controllers = {StationController.class, GlobalExceptionHandler.class})
class StationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StationRepository stationRepository;

    @MockitoBean
    private StationMapper stationMapper;

    private GeometryFactory geometryFactory;
    private Station station1;
    private Station station2;
    private Station station3;
    private StationMapDto dto1;
    private StationMapDto dto2;
    private StationMapDto dto3;

    @BeforeEach
    void setUp() {
        geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

        // Create test stations with different locations
        station1 = Station.builder()
                .id(1L)
                .code("GENCAT-001")
                .name("Barcelona Centro")
                .municipality("Barcelona")
                .location(createPoint(41.3851, 2.1734))
                .sourceType(SourceType.OFFICIAL)
                .isActive(true)
                .build();

        station2 = Station.builder()
                .id(2L)
                .code("AG-USER-001")
                .name("My Home Station")
                .municipality("Barcelona")
                .location(createPoint(41.3900, 2.1800))
                .sourceType(SourceType.CITIZEN)
                .isActive(true)
                .build();

        station3 = Station.builder()
                .id(3L)
                .code("GENCAT-002")
                .name("Madrid Centro")
                .municipality("Madrid")
                .location(createPoint(40.4168, -3.7038))
                .sourceType(SourceType.OFFICIAL)
                .isActive(true)
                .build();

        // Create corresponding DTOs
        dto1 = StationMapDto.builder()
                .id(1L)
                .code("GENCAT-001")
                .name("Barcelona Centro")
                .latitude(41.3851)
                .longitude(2.1734)
                .currentAqi(null)
                .worstPollutant(null)
                .build();

        dto2 = StationMapDto.builder()
                .id(2L)
                .code("AG-USER-001")
                .name("My Home Station")
                .latitude(41.3900)
                .longitude(2.1800)
                .currentAqi(null)
                .worstPollutant(null)
                .build();

        dto3 = StationMapDto.builder()
                .id(3L)
                .code("GENCAT-002")
                .name("Madrid Centro")
                .latitude(40.4168)
                .longitude(-3.7038)
                .currentAqi(null)
                .worstPollutant(null)
                .build();
    }

    private Point createPoint(double lat, double lon) {
        return geometryFactory.createPoint(new Coordinate(lon, lat));
    }

    // ==================== GET /api/v1/stations ====================

    @Test
    @DisplayName("Should return all stations when no filters are provided")
    void testGetStations_NoFilters_ReturnsAllStations() throws Exception {
        List<Station> stations = Arrays.asList(station1, station2, station3);
        when(stationRepository.findAll()).thenReturn(stations);
        when(stationMapper.toDto(station1)).thenReturn(dto1);
        when(stationMapper.toDto(station2)).thenReturn(dto2);
        when(stationMapper.toDto(station3)).thenReturn(dto3);

        mockMvc.perform(get("/api/v1/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].code", is("GENCAT-001")))
                .andExpect(jsonPath("$[0].name", is("Barcelona Centro")))
                .andExpect(jsonPath("$[0].latitude", is(41.3851)))
                .andExpect(jsonPath("$[0].longitude", is(2.1734)))
                .andExpect(jsonPath("$[1].code", is("AG-USER-001")))
                .andExpect(jsonPath("$[2].code", is("GENCAT-002")));
    }

    @Test
    @DisplayName("Should return empty list when no stations exist")
    void testGetStations_NoStations_ReturnsEmptyList() throws Exception {
        when(stationRepository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should return stations within bounding box when bbox parameters are provided")
    void testGetStations_WithBoundingBox_ReturnsFilteredStations() throws Exception {
        // Only Barcelona stations should be in this bounding box
        List<Station> barcelonaStations = Arrays.asList(station1, station2);
        when(stationRepository.findStationsInBoundingBox(2.0, 41.3, 2.2, 41.4))
                .thenReturn(barcelonaStations);
        when(stationMapper.toDto(station1)).thenReturn(dto1);
        when(stationMapper.toDto(station2)).thenReturn(dto2);

        mockMvc.perform(get("/api/v1/stations")
                        .param("minLat", "41.3")
                        .param("minLon", "2.0")
                        .param("maxLat", "41.4")
                        .param("maxLon", "2.2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].code", is("GENCAT-001")))
                .andExpect(jsonPath("$[1].code", is("AG-USER-001")));
    }

    @Test
    @DisplayName("Should return empty list when no stations in bounding box")
    void testGetStations_BoundingBoxWithNoStations_ReturnsEmptyList() throws Exception {
        when(stationRepository.findStationsInBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/stations")
                        .param("minLat", "0.0")
                        .param("minLon", "0.0")
                        .param("maxLat", "1.0")
                        .param("maxLon", "1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should return stations by user ID when userId parameter is provided")
    void testGetStations_WithUserId_ReturnsUserStations() throws Exception {
        List<Station> userStations = Collections.singletonList(station2);
        when(stationRepository.findByOwnerId(123L)).thenReturn(userStations);
        when(stationMapper.toDto(station2)).thenReturn(dto2);

        mockMvc.perform(get("/api/v1/stations")
                        .param("userId", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code", is("AG-USER-001")))
                .andExpect(jsonPath("$[0].name", is("My Home Station")));
    }

    @Test
    @DisplayName("Should return empty list when user has no stations")
    void testGetStations_UserWithNoStations_ReturnsEmptyList() throws Exception {
        when(stationRepository.findByOwnerId(anyLong())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/stations")
                        .param("userId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should prioritize bounding box filter over userId when both are provided")
    void testGetStations_BothFilters_PrioritizesBoundingBox() throws Exception {
        List<Station> barcelonaStations = Arrays.asList(station1, station2);
        when(stationRepository.findStationsInBoundingBox(2.0, 41.3, 2.2, 41.4))
                .thenReturn(barcelonaStations);
        when(stationMapper.toDto(station1)).thenReturn(dto1);
        when(stationMapper.toDto(station2)).thenReturn(dto2);

        mockMvc.perform(get("/api/v1/stations")
                        .param("minLat", "41.3")
                        .param("minLon", "2.0")
                        .param("maxLat", "41.4")
                        .param("maxLon", "2.2")
                        .param("userId", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("Should use findAll when only some bbox parameters are provided")
    void testGetStations_PartialBoundingBox_FallsBackToFindAll() throws Exception {
        List<Station> allStations = Arrays.asList(station1, station2, station3);
        when(stationRepository.findAll()).thenReturn(allStations);
        when(stationMapper.toDto(any(Station.class)))
                .thenReturn(dto1, dto2, dto3);

        // Missing maxLat and maxLon
        mockMvc.perform(get("/api/v1/stations")
                        .param("minLat", "41.3")
                        .param("minLon", "2.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("Should handle negative coordinates in bounding box")
    void testGetStations_NegativeCoordinates_ReturnsStations() throws Exception {
        List<Station> stations = Collections.singletonList(station3);
        when(stationRepository.findStationsInBoundingBox(-4.0, 40.0, -3.0, 41.0))
                .thenReturn(stations);
        when(stationMapper.toDto(station3)).thenReturn(dto3);

        mockMvc.perform(get("/api/v1/stations")
                        .param("minLat", "40.0")
                        .param("minLon", "-4.0")
                        .param("maxLat", "41.0")
                        .param("maxLon", "-3.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code", is("GENCAT-002")));
    }

    @Test
    @DisplayName("Should handle extreme coordinate values")
    void testGetStations_ExtremeCoordinates_ReturnsStations() throws Exception {
        when(stationRepository.findStationsInBoundingBox(-180.0, -90.0, 180.0, 90.0))
                .thenReturn(Arrays.asList(station1, station2, station3));
        when(stationMapper.toDto(any(Station.class)))
                .thenReturn(dto1, dto2, dto3);

        mockMvc.perform(get("/api/v1/stations")
                        .param("minLat", "-90.0")
                        .param("minLon", "-180.0")
                        .param("maxLat", "90.0")
                        .param("maxLon", "180.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    // ==================== GET /api/v1/stations/{code} ====================

    @Test
    @DisplayName("Should return station details when station exists")
    void testGetStationDetails_StationExists_ReturnsStation() throws Exception {
        when(stationRepository.findByCode("GENCAT-001")).thenReturn(Optional.of(station1));

        mockMvc.perform(get("/api/v1/stations/GENCAT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("GENCAT-001")))
                .andExpect(jsonPath("$.name", is("Barcelona Centro")))
                .andExpect(jsonPath("$.municipality", is("Barcelona")));
    }

    @Test
    @DisplayName("Should return 404 when station does not exist")
    void testGetStationDetails_StationNotFound_Returns404() throws Exception {
        when(stationRepository.findByCode("NONEXISTENT")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/stations/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return station details for citizen station")
    void testGetStationDetails_CitizenStation_ReturnsStation() throws Exception {
        when(stationRepository.findByCode("AG-USER-001")).thenReturn(Optional.of(station2));

        mockMvc.perform(get("/api/v1/stations/AG-USER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("AG-USER-001")))
                .andExpect(jsonPath("$.name", is("My Home Station")))
                .andExpect(jsonPath("$.sourceType", is("CITIZEN")));
    }

    @Test
    @DisplayName("Should return station details for official station")
    void testGetStationDetails_OfficialStation_ReturnsStation() throws Exception {
        when(stationRepository.findByCode("GENCAT-001")).thenReturn(Optional.of(station1));

        mockMvc.perform(get("/api/v1/stations/GENCAT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("GENCAT-001")))
                .andExpect(jsonPath("$.sourceType", is("OFFICIAL")));
    }

    @Test
    @DisplayName("Should handle special characters in station code")
    void testGetStationDetails_SpecialCharacters_ReturnsStation() throws Exception {
        Station specialStation = Station.builder()
                .id(99L)
                .code("AG-TEST-123")
                .name("Test Station")
                .municipality("Test City")
                .location(createPoint(41.0, 2.0))
                .sourceType(SourceType.CITIZEN)
                .isActive(true)
                .build();

        when(stationRepository.findByCode("AG-TEST-123")).thenReturn(Optional.of(specialStation));

        mockMvc.perform(get("/api/v1/stations/AG-TEST-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("AG-TEST-123")));
    }

    @Test
    @DisplayName("Should return 404 for empty station code")
    void testGetStationDetails_EmptyCode_Returns404() throws Exception {
        when(stationRepository.findByCode("")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/stations/"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should handle URL encoded station codes")
    void testGetStationDetails_UrlEncodedCode_ReturnsStation() throws Exception {
        when(stationRepository.findByCode("AG USER 001")).thenReturn(Optional.of(station2));

        mockMvc.perform(get("/api/v1/stations/AG%20USER%20001"))
                .andExpect(status().isOk());
    }

    // ==================== Edge Cases and Error Scenarios ====================

    @Test
    @DisplayName("Should handle invalid parameter types gracefully")
    void testGetStations_InvalidParameterType_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/stations")
                        .param("minLat", "not-a-number"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle very large user IDs")
    void testGetStations_VeryLargeUserId_ReturnsStations() throws Exception {
        when(stationRepository.findByOwnerId(Long.MAX_VALUE)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/stations")
                        .param("userId", String.valueOf(Long.MAX_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should return stations with null AQI values")
    void testGetStations_NullAqiValues_ReturnsStations() throws Exception {
        when(stationRepository.findAll()).thenReturn(Collections.singletonList(station1));
        when(stationMapper.toDto(station1)).thenReturn(dto1);

        mockMvc.perform(get("/api/v1/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currentAqi").doesNotExist())
                .andExpect(jsonPath("$[0].worstPollutant").doesNotExist());
    }

    @Test
    @DisplayName("Should handle repository exceptions gracefully")
    void testGetStations_RepositoryException_ReturnsInternalServerError() throws Exception {
        when(stationRepository.findAll()).thenThrow(new RuntimeException("Database connection error"));

        mockMvc.perform(get("/api/v1/stations"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should handle mapper exceptions gracefully")
    void testGetStations_MapperException_ReturnsInternalServerError() throws Exception {
        when(stationRepository.findAll()).thenReturn(Collections.singletonList(station1));
        when(stationMapper.toDto(station1)).thenThrow(new RuntimeException("Mapping error"));

        mockMvc.perform(get("/api/v1/stations"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should handle zero values in bounding box")
    void testGetStations_ZeroBoundingBox_ReturnsStations() throws Exception {
        when(stationRepository.findStationsInBoundingBox(0.0, 0.0, 0.0, 0.0))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/stations")
                        .param("minLat", "0.0")
                        .param("minLon", "0.0")
                        .param("maxLat", "0.0")
                        .param("maxLon", "0.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}





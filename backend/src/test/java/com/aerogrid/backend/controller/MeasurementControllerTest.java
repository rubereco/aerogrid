package com.aerogrid.backend.controller;

import com.aerogrid.backend.controller.dto.MeasurementDto;
import com.aerogrid.backend.controller.mapper.MeasurementMapper;
import com.aerogrid.backend.domain.Measurement;
import com.aerogrid.backend.domain.Pollutant;
import com.aerogrid.backend.domain.Station;
import com.aerogrid.backend.repository.MeasurementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for MeasurementController.
 * Tests all endpoints with various scenarios including success cases, edge cases, and error conditions.
 */
@WebMvcTest(controllers = {MeasurementController.class, GlobalExceptionHandler.class})
class MeasurementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeasurementRepository measurementRepository;

    @MockitoBean
    private MeasurementMapper measurementMapper;

    private Station testStation;
    private Measurement measurement1;
    private Measurement measurement2;
    private Measurement measurement3;
    private MeasurementDto dto1;
    private MeasurementDto dto2;
    private MeasurementDto dto3;
    private LocalDateTime now;
    private LocalDateTime oneDayAgo;
    private LocalDateTime twoDaysAgo;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        oneDayAgo = now.minusDays(1);
        twoDaysAgo = now.minusDays(2);

        testStation = Station.builder()
                .id(1L)
                .code("GENCAT-001")
                .name("Barcelona Centro")
                .build();

        // Create test measurements
        measurement1 = Measurement.builder()
                .id(1L)
                .station(testStation)
                .pollutant(Pollutant.NO2)
                .value(45.5)
                .timestamp(twoDaysAgo)
                .aqi(85)
                .build();

        measurement2 = Measurement.builder()
                .id(2L)
                .station(testStation)
                .pollutant(Pollutant.PM10)
                .value(32.0)
                .timestamp(oneDayAgo)
                .aqi(60)
                .build();

        measurement3 = Measurement.builder()
                .id(3L)
                .station(testStation)
                .pollutant(Pollutant.O3)
                .value(78.3)
                .timestamp(now)
                .aqi(120)
                .build();

        // Create corresponding DTOs
        dto1 = MeasurementDto.builder()
                .stationCode("GENCAT-001")
                .pollutant("NO2")
                .value(45.5)
                .timestamp(twoDaysAgo)
                .build();

        dto2 = MeasurementDto.builder()
                .stationCode("GENCAT-001")
                .pollutant("PM10")
                .value(32.0)
                .timestamp(oneDayAgo)
                .build();

        dto3 = MeasurementDto.builder()
                .stationCode("GENCAT-001")
                .pollutant("O3")
                .value(78.3)
                .timestamp(now)
                .build();
    }

    // ==================== GET /api/v1/measurements ====================

    @Test
    @DisplayName("Should return last 24 hours of measurements when no time range specified")
    void testGetHistory_NoTimeRange_ReturnsLast24Hours() throws Exception {
        List<Measurement> measurements = Arrays.asList(measurement2, measurement3);
        when(measurementRepository.findByStationCodeAndTimestampBetween(
                eq("GENCAT-001"),
                any(LocalDateTime.class),
                any(LocalDateTime.class)))
                .thenReturn(measurements);
        when(measurementMapper.toDto(measurement2)).thenReturn(dto2);
        when(measurementMapper.toDto(measurement3)).thenReturn(dto3);

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "GENCAT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].pollutant", is("PM10")))
                .andExpect(jsonPath("$[0].value", is(32.0)))
                .andExpect(jsonPath("$[1].pollutant", is("O3")))
                .andExpect(jsonPath("$[1].value", is(78.3)));
    }

    @Test
    @DisplayName("Should return measurements for custom time range")
    void testGetHistory_CustomTimeRange_ReturnsFilteredMeasurements() throws Exception {
        LocalDateTime from = twoDaysAgo.minusHours(1);
        LocalDateTime to = oneDayAgo.plusHours(1);
        List<Measurement> measurements = Arrays.asList(measurement1, measurement2);

        when(measurementRepository.findByStationCodeAndTimestampBetween(
                "GENCAT-001", from, to))
                .thenReturn(measurements);
        when(measurementMapper.toDto(measurement1)).thenReturn(dto1);
        when(measurementMapper.toDto(measurement2)).thenReturn(dto2);

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "GENCAT-001")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].pollutant", is("NO2")))
                .andExpect(jsonPath("$[1].pollutant", is("PM10")));
    }

    @Test
    @DisplayName("Should return empty list when no measurements in time range")
    void testGetHistory_NoMeasurementsInRange_ReturnsEmptyList() throws Exception {
        when(measurementRepository.findByStationCodeAndTimestampBetween(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "GENCAT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should return measurements when only 'from' parameter is provided")
    void testGetHistory_OnlyFromParameter_UsesNowAsEndTime() throws Exception {
        LocalDateTime from = twoDaysAgo;
        List<Measurement> measurements = Arrays.asList(measurement1, measurement2, measurement3);

        when(measurementRepository.findByStationCodeAndTimestampBetween(
                eq("GENCAT-001"), eq(from), any(LocalDateTime.class)))
                .thenReturn(measurements);
        when(measurementMapper.toDto(any(Measurement.class)))
                .thenReturn(dto1, dto2, dto3);

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "GENCAT-001")
                        .param("from", from.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("Should return measurements when only 'to' parameter is provided")
    void testGetHistory_OnlyToParameter_Uses24HoursBeforeAsStartTime() throws Exception {
        LocalDateTime to = now;
        List<Measurement> measurements = Arrays.asList(measurement2, measurement3);

        when(measurementRepository.findByStationCodeAndTimestampBetween(
                eq("GENCAT-001"), any(LocalDateTime.class), eq(to)))
                .thenReturn(measurements);
        when(measurementMapper.toDto(any(Measurement.class)))
                .thenReturn(dto2, dto3);

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "GENCAT-001")
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("Should return measurements for different station code")
    void testGetHistory_DifferentStationCode_ReturnsMeasurements() throws Exception {
        when(measurementRepository.findByStationCodeAndTimestampBetween(
                eq("AG-USER-001"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(measurement1));
        when(measurementMapper.toDto(measurement1)).thenReturn(dto1);

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "AG-USER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Should handle measurements with different pollutants")
    void testGetHistory_MultiplePollutants_ReturnsAllMeasurements() throws Exception {
        List<Measurement> measurements = Arrays.asList(measurement1, measurement2, measurement3);
        when(measurementRepository.findByStationCodeAndTimestampBetween(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(measurements);
        when(measurementMapper.toDto(measurement1)).thenReturn(dto1);
        when(measurementMapper.toDto(measurement2)).thenReturn(dto2);
        when(measurementMapper.toDto(measurement3)).thenReturn(dto3);

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "GENCAT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].pollutant", is("NO2")))
                .andExpect(jsonPath("$[1].pollutant", is("PM10")))
                .andExpect(jsonPath("$[2].pollutant", is("O3")));
    }

    @Test
    @DisplayName("Should handle measurements with zero values")
    void testGetHistory_ZeroValues_ReturnsMeasurements() throws Exception {
        Measurement zeroMeasurement = Measurement.builder()
                .id(99L)
                .station(testStation)
                .pollutant(Pollutant.CO)
                .value(0.0)
                .timestamp(now)
                .aqi(0)
                .build();

        MeasurementDto zeroDto = MeasurementDto.builder()
                .stationCode("GENCAT-001")
                .pollutant("CO")
                .value(0.0)
                .timestamp(now)
                .build();

        when(measurementRepository.findByStationCodeAndTimestampBetween(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(zeroMeasurement));
        when(measurementMapper.toDto(zeroMeasurement)).thenReturn(zeroDto);

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "GENCAT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value", is(0.0)));
    }

    @Test
    @DisplayName("Should handle measurements with very high values")
    void testGetHistory_HighValues_ReturnsMeasurements() throws Exception {
        Measurement highMeasurement = Measurement.builder()
                .id(99L)
                .station(testStation)
                .pollutant(Pollutant.PM10)
                .value(999.99)
                .timestamp(now)
                .aqi(500)
                .build();

        MeasurementDto highDto = MeasurementDto.builder()
                .stationCode("GENCAT-001")
                .pollutant("PM10")
                .value(999.99)
                .timestamp(now)
                .build();

        when(measurementRepository.findByStationCodeAndTimestampBetween(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(highMeasurement));
        when(measurementMapper.toDto(highMeasurement)).thenReturn(highDto);

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "GENCAT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value", is(999.99)));
    }

    @Test
    @DisplayName("Should handle measurements with decimal values")
    void testGetHistory_DecimalValues_ReturnsMeasurements() throws Exception {
        Measurement decimalMeasurement = Measurement.builder()
                .id(99L)
                .station(testStation)
                .pollutant(Pollutant.NO2)
                .value(12.345)
                .timestamp(now)
                .aqi(25)
                .build();

        MeasurementDto decimalDto = MeasurementDto.builder()
                .stationCode("GENCAT-001")
                .pollutant("NO2")
                .value(12.345)
                .timestamp(now)
                .build();

        when(measurementRepository.findByStationCodeAndTimestampBetween(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(decimalMeasurement));
        when(measurementMapper.toDto(decimalMeasurement)).thenReturn(decimalDto);

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "GENCAT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value", closeTo(12.345, 0.001)));
    }

    // ==================== Edge Cases and Error Scenarios ====================

    @Test
    @DisplayName("Should return 400 when station code is missing")
    void testGetHistory_MissingStationCode_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/measurements"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle empty station code")
    void testGetHistory_EmptyStationCode_ReturnsEmptyList() throws Exception {
        when(measurementRepository.findByStationCodeAndTimestampBetween(
                eq(""), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should handle special characters in station code")
    void testGetHistory_SpecialCharactersInCode_ReturnsMeasurements() throws Exception {
        when(measurementRepository.findByStationCodeAndTimestampBetween(
                eq("AG-TEST-123"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(measurement1));
        when(measurementMapper.toDto(measurement1)).thenReturn(dto1);

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "AG-TEST-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Should return 400 when date format is invalid")
    void testGetHistory_InvalidDateFormat_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "GENCAT-001")
                        .param("from", "invalid-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle 'from' date after 'to' date")
    void testGetHistory_FromAfterTo_ReturnsEmptyList() throws Exception {
        LocalDateTime from = now;
        LocalDateTime to = twoDaysAgo;

        when(measurementRepository.findByStationCodeAndTimestampBetween(
                anyString(), eq(from), eq(to)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "GENCAT-001")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should handle very old timestamps")
    void testGetHistory_VeryOldTimestamp_ReturnsMeasurements() throws Exception {
        LocalDateTime veryOld = LocalDateTime.of(2000, 1, 1, 0, 0);
        when(measurementRepository.findByStationCodeAndTimestampBetween(
                anyString(), eq(veryOld), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "GENCAT-001")
                        .param("from", veryOld.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should handle future timestamps")
    void testGetHistory_FutureTimestamp_ReturnsEmptyList() throws Exception {
        LocalDateTime future = LocalDateTime.now().plusYears(1);
        when(measurementRepository.findByStationCodeAndTimestampBetween(
                anyString(), any(LocalDateTime.class), eq(future)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "GENCAT-001")
                        .param("to", future.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should handle same 'from' and 'to' timestamp")
    void testGetHistory_SameFromAndTo_ReturnsMeasurements() throws Exception {
        LocalDateTime sameTime = now;
        when(measurementRepository.findByStationCodeAndTimestampBetween(
                anyString(), eq(sameTime), eq(sameTime)))
                .thenReturn(Collections.singletonList(measurement3));
        when(measurementMapper.toDto(measurement3)).thenReturn(dto3);

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "GENCAT-001")
                        .param("from", sameTime.toString())
                        .param("to", sameTime.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Should handle repository exceptions gracefully")
    void testGetHistory_RepositoryException_ReturnsInternalServerError() throws Exception {
        when(measurementRepository.findByStationCodeAndTimestampBetween(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database connection error"));

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "GENCAT-001"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should handle mapper exceptions gracefully")
    void testGetHistory_MapperException_ReturnsInternalServerError() throws Exception {
        when(measurementRepository.findByStationCodeAndTimestampBetween(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(measurement1));
        when(measurementMapper.toDto(measurement1))
                .thenThrow(new RuntimeException("Mapping error"));

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "GENCAT-001"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should handle large result sets efficiently")
    void testGetHistory_LargeResultSet_ReturnsMeasurements() throws Exception {
        // Create 1000 measurements
        List<Measurement> largeMeasurementList = Collections.nCopies(1000, measurement1);

        when(measurementRepository.findByStationCodeAndTimestampBetween(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(largeMeasurementList);
        when(measurementMapper.toDto(any(Measurement.class))).thenReturn(dto1);

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "GENCAT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1000)));
    }

    @Test
    @DisplayName("Should preserve measurement order from repository")
    void testGetHistory_PreservesOrder_ReturnsMeasurementsInOrder() throws Exception {
        List<Measurement> orderedMeasurements = Arrays.asList(measurement1, measurement2, measurement3);
        when(measurementRepository.findByStationCodeAndTimestampBetween(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(orderedMeasurements);
        when(measurementMapper.toDto(measurement1)).thenReturn(dto1);
        when(measurementMapper.toDto(measurement2)).thenReturn(dto2);
        when(measurementMapper.toDto(measurement3)).thenReturn(dto3);

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "GENCAT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pollutant", is("NO2")))
                .andExpect(jsonPath("$[1].pollutant", is("PM10")))
                .andExpect(jsonPath("$[2].pollutant", is("O3")));
    }

    @Test
    @DisplayName("Should handle URL encoded station codes")
    void testGetHistory_UrlEncodedStationCode_ReturnsMeasurements() throws Exception {
        when(measurementRepository.findByStationCodeAndTimestampBetween(
                eq("AG USER 001"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(measurement1));
        when(measurementMapper.toDto(measurement1)).thenReturn(dto1);

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "AG USER 001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Should handle ISO 8601 date format")
    void testGetHistory_Iso8601DateFormat_ReturnsMeasurements() throws Exception {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 31, 23, 59, 59);

        when(measurementRepository.findByStationCodeAndTimestampBetween(
                anyString(), eq(from), eq(to)))
                .thenReturn(Collections.singletonList(measurement1));
        when(measurementMapper.toDto(measurement1)).thenReturn(dto1);

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "GENCAT-001")
                        .param("from", "2026-01-01T00:00:00")
                        .param("to", "2026-01-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Should handle measurements with null AQI values")
    void testGetHistory_NullAqiValues_ReturnsMeasurements() throws Exception {
        Measurement nullAqiMeasurement = Measurement.builder()
                .id(99L)
                .station(testStation)
                .pollutant(Pollutant.NO2)
                .value(45.5)
                .timestamp(now)
                .aqi(null)
                .build();

        MeasurementDto nullAqiDto = MeasurementDto.builder()
                .stationCode("GENCAT-001")
                .pollutant("NO2")
                .value(45.5)
                .timestamp(now)
                .build();

        when(measurementRepository.findByStationCodeAndTimestampBetween(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(nullAqiMeasurement));
        when(measurementMapper.toDto(nullAqiMeasurement)).thenReturn(nullAqiDto);

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "GENCAT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value", is(45.5)));
    }

    @Test
    @DisplayName("Should handle consecutive requests for different stations")
    void testGetHistory_ConsecutiveRequests_ReturnsDifferentResults() throws Exception {
        // First request for station 1
        when(measurementRepository.findByStationCodeAndTimestampBetween(
                eq("GENCAT-001"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(measurement1));
        when(measurementMapper.toDto(measurement1)).thenReturn(dto1);

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "GENCAT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].pollutant", is("NO2")));

        // Second request for station 2
        when(measurementRepository.findByStationCodeAndTimestampBetween(
                eq("AG-USER-001"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(measurement2));
        when(measurementMapper.toDto(measurement2)).thenReturn(dto2);

        mockMvc.perform(get("/api/v1/measurements")
                        .param("stationCode", "AG-USER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].pollutant", is("PM10")));
    }
}




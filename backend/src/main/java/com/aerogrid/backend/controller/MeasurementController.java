package com.aerogrid.backend.controller;

import com.aerogrid.backend.controller.dto.MeasurementDto;
import com.aerogrid.backend.controller.mapper.MeasurementMapper;
import com.aerogrid.backend.domain.Measurement;
import com.aerogrid.backend.repository.MeasurementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for measurement data queries.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/measurements")
@RequiredArgsConstructor
public class MeasurementController {
    private final MeasurementRepository measurementRepository;
    private final MeasurementMapper measurementMapper;

    /**
     * Retrieves historical measurement data for a station.
     * If no time range is specified, returns the last 24 hours of data.
     *
     * @param stationCode the station code
     * @param from start timestamp (optional, defaults to 24 hours before end)
     * @param to end timestamp (optional, defaults to now)
     * @return list of measurements within the specified time range
     */
    @GetMapping
    public ResponseEntity<List<MeasurementDto>> getHistory(
            @RequestParam String stationCode,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to
    ) {
        try {
            LocalDateTime end = (to != null) ? to : LocalDateTime.now();
            LocalDateTime start = (from != null) ? from : end.minusHours(24);

            List<Measurement> data = measurementRepository.findByStationCodeAndTimestampBetween(
                    stationCode, start, end);

            List<MeasurementDto> dtos = data.stream()
                    .map(measurementMapper::toDto)
                    .toList();

            return ResponseEntity.ok(dtos);

        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters for measurement query: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            log.error("Error retrieving measurements for station: {}", stationCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}

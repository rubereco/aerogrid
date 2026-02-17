package com.aerogrid.backend.controller;

import com.aerogrid.backend.controller.dto.StationMapDto;
import com.aerogrid.backend.controller.mapper.StationMapper;
import com.aerogrid.backend.domain.Station;
import com.aerogrid.backend.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for station management and queries.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stations")
@RequiredArgsConstructor
public class StationController {
    private final StationRepository stationRepository;
    private final StationMapper stationMapper;

    /**
     * Retrieves stations based on filtering criteria.
     * Supports three query modes:
     * 1. Bounding box filter (minLat, minLon, maxLat, maxLon) - returns stations within geographic bounds
     * 2. User filter (userId) - returns stations owned by a specific user
     * 3. No filter - returns all stations
     *
     * @param minLat minimum latitude for bounding box filter
     * @param minLon minimum longitude for bounding box filter
     * @param maxLat maximum latitude for bounding box filter
     * @param maxLon maximum longitude for bounding box filter
     * @param userId user ID to filter stations by owner
     * @return list of station map DTOs
     */
    @GetMapping
    public ResponseEntity<List<StationMapDto>> getStations(
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double minLon,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double maxLon,
            @RequestParam(required = false) Long userId
    ) {
        try {
            List<Station> stations;

            // When the next phase begins, the logic will be moved to the service layer
            if (minLat != null && minLon != null && maxLat != null && maxLon != null) {
                stations = stationRepository.findStationsInBoundingBox(minLon, minLat, maxLon, maxLat);
            } else if (userId != null) {
                stations = stationRepository.findByOwnerId(userId);
            } else {
                stations = stationRepository.findAll();
            }

            List<StationMapDto> dtos = stations.stream()
                    .map(stationMapper::toDto)
                    .toList();

            return ResponseEntity.ok(dtos);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters for station query: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            log.error("Error retrieving stations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Retrieves detailed information for a specific station.
     *
     * @param code the station code
     * @return station details or 404 if not found
     */
    @GetMapping("/{code}")
    public ResponseEntity<Station> getStationDetails(@PathVariable String code) {
        try {
            return stationRepository.findByCode(code)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving station details for code: {}", code, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

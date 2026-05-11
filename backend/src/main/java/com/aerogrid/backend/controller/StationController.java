package com.aerogrid.backend.controller;

import com.aerogrid.backend.controller.dto.StationCreationResponseDto;
import com.aerogrid.backend.controller.dto.StationDetailsDto;
import com.aerogrid.backend.controller.dto.StationMapDto;
import com.aerogrid.backend.controller.mapper.StationMapper;
import com.aerogrid.backend.domain.Station;
import com.aerogrid.backend.domain.User;
import com.aerogrid.backend.repository.StationRepository;
import com.aerogrid.backend.repository.projection.StationMapProjection;
import com.aerogrid.backend.repository.projection.AggregatedMeasurementProjection;
import com.aerogrid.backend.service.StationService;
import com.aerogrid.backend.repository.MeasurementRepository;
import com.aerogrid.backend.repository.StationApiKeyRepository;
import com.aerogrid.backend.repository.VoteRepository;
import com.aerogrid.backend.controller.dto.MyStationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    /* Tota la lògica que hi ha en aquesta classe es moura en el service */
    private final StationRepository stationRepository;
    private final StationMapper stationMapper;
    private final StationService stationService;
    private final MeasurementRepository measurementRepository;
    private final StationApiKeyRepository stationApiKeyRepository;
    private final VoteRepository voteRepository;

    /**
     * Retrieves the stations owned by the currently authenticated user,
     * including their active API keys.
     *
     * @param currentUser the authenticated user
     * @return list of MyStationDto
     */
    @GetMapping("/me")
    public ResponseEntity<List<MyStationDto>> getMyStations(@AuthenticationPrincipal User currentUser) {
        try {
            List<Station> stations = stationRepository.findAllByOwner(currentUser);
            List<MyStationDto> dtos = stations.stream().map(station -> {
                String apiKey = stationApiKeyRepository.findActiveKeyByStationId(station.getId())
                        .map(k -> k.getApiKey())
                        .orElse(null);
                
                return MyStationDto.builder()
                        .id(station.getId())
                        .code(station.getCode())
                        .name(station.getName())
                        .municipality(station.getMunicipality())
                        .isActive(station.getIsActive())
                        .apiKey(apiKey)
                        .build();
            }).toList();
            
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error("Error retrieving user's stations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

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
     * @param targetTime optional target time for querying station data
     * @return list of station map DTOs
     */
    @GetMapping
    public ResponseEntity<List<StationMapDto>> getStations(
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double minLon,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double maxLon,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime targetTime
    ) {
        try {
            List<StationMapProjection> projections;

            java.time.LocalDateTime actualTargetTime = targetTime != null ? targetTime : java.time.LocalDateTime.now();
            java.time.LocalDateTime minTime = actualTargetTime.minusHours(48); // Extended to 48 hours for data elasticity

            if (minLat != null && minLon != null && maxLat != null && maxLon != null) {
                projections = stationRepository.findStationsInBoundingBox(minLon, minLat, maxLon, maxLat, actualTargetTime, minTime);
            } else if (userId != null) {
                projections = stationRepository.findByOwnerIdProjection(userId, actualTargetTime, minTime);
            } else {
                projections = stationRepository.findAllStationsWithStatus(actualTargetTime, minTime);
            }

            List<StationMapDto> dtos = projections.stream()
                    .map(p -> StationMapDto.builder()
                            .id(p.getId())
                            .code(p.getCode())
                            .name(p.getName())
                            .latitude(p.getLatitude())
                            .longitude(p.getLongitude())
                            .aqi(p.getAqi())
                            .pollutant(p.getPollutant())
                            .trustScore(p.getTrustScore())
                            .sourceType(p.getSourceType())
                            .build())
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
     * @return station details DTO or 404 if not found
     */
    @GetMapping("/{code}")
    public ResponseEntity<StationDetailsDto> getStationDetails(@PathVariable String code) {
        try {
            return stationRepository.findByCode(code)
                    .map(station -> {
                        StationDetailsDto dto = stationMapper.toDetailsDto(station);
                        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusMonths(6);
                        List<Object[]> counts = voteRepository.getVoteCountsForStationSince(station.getId(), since);
                        if (!counts.isEmpty() && counts.get(0)[0] != null) {
                            dto.setUpvotes(((Number) counts.get(0)[0]).longValue());
                            dto.setDownvotes(((Number) counts.get(0)[1]).longValue());
                        } else {
                            dto.setUpvotes(0L);
                            dto.setDownvotes(0L);
                        }
                        return dto;
                    })
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving station details for code: {}", code, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{stationId}/measurements")
    public ResponseEntity<List<AggregatedMeasurementProjection>> getAggregatedMeasurements(
            @PathVariable("stationId") String stationCode,
            @RequestParam("startDate") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @RequestParam("endDate") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate,
            @RequestParam("resolution") String resolution
    ) {
        try {
            List<AggregatedMeasurementProjection> result;
            switch (resolution.toUpperCase()) {
                case "1D":
                case "HOURLY":
                    result = measurementRepository.aggregateHourly(stationCode, startDate, endDate);
                    break;
                case "1W":
                case "SIX_HOURS":
                    result = measurementRepository.aggregateSixHours(stationCode, startDate, endDate);
                    break;
                case "1M":
                case "DAILY":
                    result = measurementRepository.aggregateDaily(stationCode, startDate, endDate);
                    break;
                case "1Y":
                case "WEEKLY":
                    result = measurementRepository.aggregateWeekly(stationCode, startDate, endDate);
                    break;
                default:
                    return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error aggregating measurements", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<StationCreationResponseDto> createStation(
            @RequestBody StationDetailsDto stationDetails,
            @AuthenticationPrincipal User currentUser) {
        try {
            StationCreationResponseDto response = stationService.createStation(stationDetails, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid station data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            log.error("Error creating station", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

    }

    @PutMapping("/{id}")
    public ResponseEntity<StationDetailsDto> updateStation(
            @PathVariable Long id,
            @RequestBody StationDetailsDto stationDetails,
            @AuthenticationPrincipal User currentUser) {
        try {
            StationDetailsDto response = stationService.updateStation(id, stationDetails, currentUser);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating station", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStation(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        try {
            stationService.deleteStation(id, currentUser);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting station", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
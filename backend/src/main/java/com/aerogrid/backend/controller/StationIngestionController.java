package com.aerogrid.backend.controller;

import com.aerogrid.backend.ingestion.citizen.CitizenIngestionService;
import com.aerogrid.backend.ingestion.citizen.StationIngestionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ingest")
@RequiredArgsConstructor
public class StationIngestionController {

    private final CitizenIngestionService ingestionService;

    /**
     * Endpoint for uploading measurements from citizen stations.
     * Example CURL:
     * curl -X POST http://localhost:8080/api/v1/ingest \
     * -H "X-API-KEY: sk_live_12345" \
     * -H "Content-Type: application/json" \
     * -d '{"pollutant": "NO2", "value": 45.5}'
     */
    @PostMapping
    public ResponseEntity<String> ingestMeasurement(
            @RequestHeader("X-API-KEY") String apiKey,
            @RequestBody StationIngestionDto dto) {

        try {
            ingestionService.processIngestion(apiKey, dto);
            return ResponseEntity.ok("Data accepted");

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or inactive API Key");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid data: " + e.getMessage());

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error: " + e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error: " + e.getMessage());
        }
    }
}
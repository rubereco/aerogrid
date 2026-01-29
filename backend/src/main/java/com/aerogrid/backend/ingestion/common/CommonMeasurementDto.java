package com.aerogrid.backend.ingestion.common;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Common DTO for Measurement data, used to standardize data from different providers.
 */
@Data
@Builder
public class CommonMeasurementDto {
    private String stationCode;
    private String pollutant;
    private Double value;
    private LocalDateTime timestamp;
}
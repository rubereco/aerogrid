package com.aerogrid.backend.ingestion.common;

import lombok.Builder;
import lombok.Data;

/**
 * Common DTO for Station data, used to standardize data from different providers.
 */
@Data
@Builder
public class CommonStationDto {
    private String code;
    private String name;
    private String municipality;
    private Double latitude;
    private Double longitude;
    private String type;
}
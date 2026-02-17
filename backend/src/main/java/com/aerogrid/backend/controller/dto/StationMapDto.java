package com.aerogrid.backend.controller.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for station information displayed on map.
 */
@Data
@Builder
public class StationMapDto {
    /** Station identifier. */
    private Long id;

    /** Station code. */
    private String code;

    /** Station name. */
    private String name;

    /** Station latitude coordinate. */
    private Double latitude;

    /** Station longitude coordinate. */
    private Double longitude;

    /** Current worst AQI value. */
    private Integer currentAqi;

    /** Worst pollutant identifier. */
    private String worstPollutant;
}
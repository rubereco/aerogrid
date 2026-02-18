package com.aerogrid.backend.controller.dto;

import com.aerogrid.backend.domain.Pollutant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for station information displayed on map.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private Pollutant worstPollutant;
}
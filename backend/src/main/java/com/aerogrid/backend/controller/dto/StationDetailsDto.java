package com.aerogrid.backend.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for detailed station information.
 * Contains all station data excluding sensitive fields like owner.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationDetailsDto {
    /** Station identifier. */
    private Long id;

    /** Station code. */
    private String code;

    /** Station name. */
    private String name;

    /** Municipality where the station is located. */
    private String municipality;

    /** Station latitude coordinate. */
    private Double latitude;

    /** Station longitude coordinate. */
    private Double longitude;

    /** Type of station source (OFFICIAL or CITIZEN). */
    private String sourceType;

    /** Community trust score based on user votes. */
    private Integer trustScore;

    /** Whether the station is currently active. */
    private Boolean isActive;

    /** Timestamp when the station was created. */
    private LocalDateTime createdAt;

    /** Timestamp when the station was last updated. */
    private LocalDateTime updatedAt;
}


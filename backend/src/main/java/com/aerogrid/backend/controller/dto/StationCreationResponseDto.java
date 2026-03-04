package com.aerogrid.backend.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for newly created station response.
 * Includes the full station details and the generated API key.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationCreationResponseDto {
    /** The details of the created station */
    private StationDetailsDto station;

    /** The private API key for the station (only returned on creation) */
    private String apiKey;
}


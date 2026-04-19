package com.aerogrid.backend.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for displaying a user's own station details including its API Key.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyStationDto {
    private Long id;
    private String code;
    private String name;
    private String municipality;
    private Boolean isActive;

    /** The active API key for managing the station */
    private String apiKey;
}


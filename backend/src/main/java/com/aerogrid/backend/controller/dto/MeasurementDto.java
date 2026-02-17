package com.aerogrid.backend.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MeasurementDto {
    private String stationCode;
    private String pollutant;
    private Double value;
    private LocalDateTime timestamp;
}

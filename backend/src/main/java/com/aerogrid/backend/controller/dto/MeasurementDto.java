package com.aerogrid.backend.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasurementDto {
    private String stationCode;
    private String pollutant;
    private Double value;
    private LocalDateTime timestamp;
}

package com.aerogrid.backend.ingestion.citizen;

import lombok.Data;

@Data
public class StationIngestionDto {

    private String pollutant;
    private Double value;
}
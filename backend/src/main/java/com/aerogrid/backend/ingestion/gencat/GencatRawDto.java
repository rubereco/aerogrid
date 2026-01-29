package com.aerogrid.backend.ingestion.gencat;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * This DTO represents the "Flat" format from Generalitat.
 * It contains BOTH station information AND hourly measurements.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GencatRawDto {

    @JsonProperty("codi_eoi")
    private String stationCode;

    @JsonProperty("nom_estacio")
    private String stationName;

    @JsonProperty("municipi")
    private String municipality;

    @JsonProperty("latitud")
    private String latitude;

    @JsonProperty("longitud")
    private String longitude;

    @JsonProperty("tipus_estacio")
    private String stationType;

    @JsonProperty("data")
    private String date;

    @JsonProperty("contaminant")
    private String pollutant;

    @JsonProperty("unitats")
    private String units;

    private Map<String, String> hourlyValues = new HashMap<>();

    @JsonAnySetter
    public void addHour(String key, String value) {
        if (key.startsWith("h")) {
            hourlyValues.put(key, value);
        }
    }
}
package com.aerogrid.backend.ingestion.gencat;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * Data Transfer Object representing the raw data format from Generalitat de Catalunya API.
 * Contains station metadata and hourly pollutant measurements.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GencatRawDto {

    /** Identifier code of the monitoring station. */
    @JsonProperty("codi_eoi")
    private String stationCode;

    /** Official name of the station. */
    @JsonProperty("nom_estacio")
    private String stationName;

    /** Municipality where the station is located. */
    @JsonProperty("municipi")
    private String municipality;

    /** Latitude coordinate of the station. */
    @JsonProperty("latitud")
    private String latitude;

    /** Longitude coordinate of the station. */
    @JsonProperty("longitud")
    private String longitude;

    /** Classification type of the station. */
    @JsonProperty("tipus_estacio")
    private String stationType;

    /** Date of the measurement in ISO format. */
    @JsonProperty("data")
    private String date;

    /** Name of the pollutant being measured. */
    @JsonProperty("contaminant")
    private String pollutant;

    /** Unit of measurement for the pollutant. */
    @JsonProperty("unitats")
    private String units;

    /** Storage for dynamic hourly measurement values (h01-h24). */
    private Map<String, String> hourlyValues = new HashMap<>();

    /**
     * Captures dynamic properties starting with "h" which represent hourly measurements.
     *
     * @param key   The property name (e.g., "h01").
     * @param value The measurement value.
     */
    @JsonAnySetter
    public void addHour(String key, String value) {
        if (key.startsWith("h")) {
            hourlyValues.put(key, value);
        }
    }
}
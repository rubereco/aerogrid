package com.aerogrid.backend.ingestion.common;

import com.aerogrid.backend.domain.Pollutant;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MeasurementValidator {

    /**
     * Validates that a measurement falls within realistic atmospheric physical boundaries
     * and that it is not in the future.
     * Throws IllegalArgumentException if the bounds are exceeded.
     *
     * @param pollutant The measured pollutant
     * @param value The recorded value
     * @param timestamp The measurement timestamp
     */
    public void validate(Pollutant pollutant, Double value, LocalDateTime timestamp) {
        if (timestamp != null && timestamp.isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new IllegalArgumentException("La data de la mesura no pot ser en el futur");
        }

        if (value == null) {
            throw new IllegalArgumentException("El valor de la mesura no pot ser nul");
        }
        if (value < 0) {
            throw new IllegalArgumentException("El valor de la mesura ha de ser positiu o zero");
        }

        // Extremely generous atmospheric physical limits.
        // If values exceed these boundaries, sensors are likely corrupted or malfunctioning.
        double maxAllowed = switch (pollutant.name()) {
            case "CO" -> 150.0; // mg/m³
            case "PM2_5", "PM25", "PM1" -> 500.0; // µg/m³
            case "NO2", "O3", "PM10", "H2S", "C6H6" -> 1000.0; // µg/m³
            case "SO2" -> 2000.0; // µg/m³
            default -> 2000.0;
        };

        if (value > maxAllowed) {
            throw new IllegalArgumentException("El valor " + value + " excedeix el límit global establert (" + maxAllowed + ") pel contaminant " + pollutant.name());
        }
    }
}

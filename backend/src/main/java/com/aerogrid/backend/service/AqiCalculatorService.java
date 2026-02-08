package com.aerogrid.backend.service;

import com.aerogrid.backend.domain.Pollutant;
import org.springframework.stereotype.Service;

/**
 * Service responsible for calculating the Air Quality Index (AQI) based on pollutant concentrations.
 * <p>
 * This service implements the logic to map raw concentration values (e.g., µg/m³)
 * to a standardized index, following European and Spanish national standards.
 * </p>
 */
@Service
public class AqiCalculatorService {

    /**
     * Calculates the Air Quality Index (AQI) for a given pollutant and value.
     * <p>
     * The AQI is calculated based on the <b>Spanish National Air Quality Index (ICA)</b>
     * and the <b>European Air Quality Index (EAQI)</b> standards.
     * </p>
     * <p>
     * <b>AQI Scale (1-6):</b>
     * <ul>
     * <li><b>1: Good</b> (Satisfactory air quality, little or no risk)</li>
     * <li><b>2: Fair</b> (Acceptable quality, matches legal limits)</li>
     * <li><b>3: Moderate</b> (May affect sensitive groups)</li>
     * <li><b>4: Poor</b> (Health effects possible for general population)</li>
     * <li><b>5: Very Poor</b> (Health warnings of emergency conditions)</li>
     * <li><b>6: Extremely Poor</b> (Serious health effects for everyone)</li>
     * </ul>
     *
     * @param pollutantName The name of the pollutant (e.g., "NO2", "PM10", "CO").
     * @param value         The measured concentration value.
     * @return The AQI level (1 to 6), or {@code null} if the pollutant is not supported
     * (e.g., H2S, C6H6) or the value is invalid.
     */
    public Integer calculateAqi(String pollutantName, Double value) {
        if (value == null || value < 0) return null;

        try {
            Pollutant pollutant = Pollutant.valueOf(pollutantName);
            return switch (pollutant) {
                case NO2 -> calculateNo2(value);
                case PM10 -> calculatePm10(value);
                case PM25 -> calculatePm25(value);
                case O3 -> calculateO3(value);
                case SO2 -> calculateSo2(value);
                case CO -> calculateCo(value);
                default -> null;
            };
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Calculates AQI for Nitrogen Dioxide (NO2).
     * <br>
     * <b>Unit:</b> µg/m³ (micrograms per cubic meter).
     * <br>
     * <b>Standard:</b> European Air Quality Index (EAQI) / ICA (Spain).
     */
    private Integer calculateNo2(Double v) {
        if (v <= 40) return 1;
        if (v <= 90) return 2;
        if (v <= 120) return 3;
        if (v <= 230) return 4;
        if (v <= 340) return 5;
        return 6;
    }

    /**
     * Calculates AQI for Particulate Matter < 10 µm (PM10).
     * <br>
     * <b>Unit:</b> µg/m³ (micrograms per cubic meter).
     * <br>
     * <b>Standard:</b> European Air Quality Index (EAQI) / ICA (Spain).
     */
    private Integer calculatePm10(Double v) {
        if (v <= 20) return 1;
        if (v <= 40) return 2;
        if (v <= 50) return 3;
        if (v <= 100) return 4;
        if (v <= 150) return 5;
        return 6;
    }

    /**
     * Calculates AQI for Particulate Matter < 2.5 µm (PM2.5).
     * <br>
     * <b>Unit:</b> µg/m³ (micrograms per cubic meter).
     * <br>
     * <b>Standard:</b> European Air Quality Index (EAQI) / ICA (Spain).
     */
    private Integer calculatePm25(Double v) {
        if (v <= 10) return 1;
        if (v <= 20) return 2;
        if (v <= 25) return 3;
        if (v <= 50) return 4;
        if (v <= 75) return 5;
        return 6;
    }

    /**
     * Calculates AQI for Ozone (O3).
     * <br>
     * <b>Unit:</b> µg/m³ (micrograms per cubic meter).
     * <br>
     * <b>Standard:</b> European Air Quality Index (EAQI) / ICA (Spain).
     */
    private Integer calculateO3(Double v) {
        if (v <= 50) return 1;
        if (v <= 100) return 2;
        if (v <= 130) return 3;
        if (v <= 240) return 4;
        if (v <= 380) return 5;
        return 6;
    }

    /**
     * Calculates AQI for Sulfur Dioxide (SO2).
     * <br>
     * <b>Unit:</b> µg/m³ (micrograms per cubic meter).
     * <br>
     * <b>Standard:</b> European Air Quality Index (EAQI) / ICA (Spain).
     */
    private Integer calculateSo2(Double v) {
        if (v <= 100) return 1;
        if (v <= 200) return 2;
        if (v <= 350) return 3;
        if (v <= 500) return 4;
        if (v <= 750) return 5;
        return 6;
    }

    /**
     * Calculates AQI for Carbon Monoxide (CO).
     * <br>
     * <b>Unit:</b> mg/m³ (milligrams per cubic meter).
     * <br>
     * <b>Standard:</b> National Air Quality Index (ICA) - MITECO (Spain).
     */
    private Integer calculateCo(Double v) {
        if (v <= 5) return 1;
        if (v <= 10) return 2;
        if (v <= 15) return 3;
        if (v <= 25) return 4;
        if (v <= 50) return 5;
        return 6;
    }
}
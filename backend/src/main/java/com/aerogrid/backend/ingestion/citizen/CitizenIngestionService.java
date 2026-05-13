package com.aerogrid.backend.ingestion.citizen;

import com.aerogrid.backend.domain.Pollutant;
import com.aerogrid.backend.domain.Station;
import com.aerogrid.backend.domain.StationApiKey;
import com.aerogrid.backend.ingestion.common.CommonMapper;
import com.aerogrid.backend.repository.MeasurementRepository;
import com.aerogrid.backend.repository.StationApiKeyRepository;
import com.aerogrid.backend.service.AqiCalculatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitizenIngestionService {

    private final StationApiKeyRepository apiKeyRepository;
    private final MeasurementRepository measurementRepository;
    private final AqiCalculatorService aqiCalculator;
    private final CommonMapper commonMapper;

    /**
     * Processes a measurement ingestion request from a citizen station.
     *
     * @param apiKey The API key for authentication.
     * @param dto    The measurement data transfer object.
     * @throws SecurityException        If the API key is invalid or inactive.
     * @throws IllegalArgumentException If the pollutant is unknown or data is invalid.
     * @throws RuntimeException         If there's a database error.
     */
    public void processIngestion(String apiKey, StationIngestionDto dto) {
        StationApiKey keyEntity = apiKeyRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new SecurityException("Invalid API Key"));

        if (!keyEntity.isActive()) {
            throw new SecurityException("API Key is inactive");
        }

        Station station = keyEntity.getStation();

        Pollutant pollutant = commonMapper.mapPollutantString(dto.getPollutant());
        if (pollutant == null) {
            log.warn("Unknown or null pollutant '{}' from station {}", dto.getPollutant(), station.getCode());
            throw new IllegalArgumentException("Unknown or null pollutant: " + dto.getPollutant());
        }

        Integer aqi = aqiCalculator.calculateAqi(pollutant.name(), dto.getValue());

        saveMeasurement(station, pollutant, dto.getValue(), aqi);

        log.debug("Citizen data received [{}]: {} = {} (AQI: {})",
                station.getCode(), pollutant, dto.getValue(), aqi);
    }

    /**
     * Processes a CSV file ingestion request from a citizen station.
     * Expected CSV format: pollutant,value,timestamp
     *
     * @param apiKey The API key for authentication.
     * @param file   The CSV file containing measurements.
     */
    public void processCsvIngestion(String apiKey, MultipartFile file) {
        StationApiKey keyEntity = apiKeyRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new SecurityException("Invalid API Key"));

        if (!keyEntity.isActive()) {
            throw new SecurityException("API Key is inactive");
        }

        Station station = keyEntity.getStation();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstRow = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstRow) {
                    isFirstRow = false;
                    // Optional: check if header
                    if (line.toLowerCase().contains("pollutant")) {
                        continue;
                    }
                }
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    try {
                        String pollutantStr = parts[0].trim();
                        Double value = Double.valueOf(parts[1].trim());
                        LocalDateTime timestamp = LocalDateTime.parse(parts[2].trim());

                        Pollutant pollutant = commonMapper.mapPollutantString(pollutantStr);
                        if (pollutant != null) {
                            Integer aqi = aqiCalculator.calculateAqi(pollutant.name(), value);
                            measurementRepository.saveMeasurementNative(
                                    station.getId(),
                                    pollutant.name(),
                                    value,
                                    timestamp,
                                    aqi
                            );
                        } else {
                            log.warn("Unknown pollutant {} in CSV for station {}", pollutantStr, station.getCode());
                        }
                    } catch (Exception e) {
                        log.warn("Error parsing CSV line '{}': {}", line, e.getMessage());
                        // Continue processing other lines
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error reading CSV file", e);
            throw new RuntimeException("Error processing CSV: " + e.getMessage());
        }
    }

    /**
     * Saves a citizen-submitted measurement to the database.
     *
     * @param station   The station entity.
     * @param pollutant The pollutant type.
     * @param value     The measured value.
     * @param aqi       The calculated AQI.
     */
    private void saveMeasurement(Station station, Pollutant pollutant, Double value, Integer aqi) {
        try {
            measurementRepository.saveMeasurementNative(
                    station.getId(),
                    pollutant.name(),
                    value,
                    LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES),
                    aqi
            );

        } catch (Exception e) {
            log.error("Error saving citizen data: {}", e.getMessage());
            throw new RuntimeException("Database error");
        }
    }
}
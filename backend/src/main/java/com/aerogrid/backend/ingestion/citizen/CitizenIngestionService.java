package com.aerogrid.backend.ingestion.citizen;

import com.aerogrid.backend.domain.Pollutant;
import com.aerogrid.backend.domain.Station;
import com.aerogrid.backend.domain.StationApiKey;
import com.aerogrid.backend.ingestion.common.CommonMapper;
import com.aerogrid.backend.ingestion.common.MeasurementValidator;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitizenIngestionService {

    private final StationApiKeyRepository apiKeyRepository;
    private final MeasurementRepository measurementRepository;
    private final AqiCalculatorService aqiCalculator;
    private final CommonMapper commonMapper;
    private final MeasurementValidator measurementValidator;

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

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        measurementValidator.validate(pollutant, dto.getValue(), now);

        Integer aqi = aqiCalculator.calculateAqi(pollutant.name(), dto.getValue());

        saveMeasurement(station, pollutant, dto.getValue(), aqi, now);

        log.debug("Citizen data received [{}]: {} = {} (AQI: {})",
                station.getCode(), pollutant, dto.getValue(), aqi);
    }

    /**
     * Processes a CSV file ingestion request from a citizen station.
     * Expected CSV format: pollutant,value,timestamp
     *
     * @param apiKey The API key for authentication.
     * @param file   The CSV file containing measurements.
     * @return Map with result details.
     */
    public Map<String, Object> processCsvIngestion(String apiKey, MultipartFile file) {
        StationApiKey keyEntity = apiKeyRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new SecurityException("Invalid API Key"));

        if (!keyEntity.isActive()) {
            throw new SecurityException("API Key is inactive");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("L'arxiu CSV està buit");
        }
        
        String filename = file.getOriginalFilename();
        if (filename != null && !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Format d'arxiu invàlid. Ha de ser un CSV");
        }

        Station station = keyEntity.getStation();
        
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstRow = true;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
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
                            measurementValidator.validate(pollutant, value, timestamp);
                            
                            Integer aqi = aqiCalculator.calculateAqi(pollutant.name(), value);
                            measurementRepository.saveMeasurementNative(
                                    station.getId(),
                                    pollutant.name(),
                                    value,
                                    timestamp,
                                    aqi
                            );
                            successCount++;
                        } else {
                            failCount++;
                            errors.add("Línia " + lineNumber + ": Contaminant desconegut '" + pollutantStr + "'");
                        }
                    } catch (NumberFormatException e) {
                        failCount++;
                        errors.add("Línia " + lineNumber + ": Format de número invàlid");
                    } catch (IllegalArgumentException e) {
                        failCount++;
                        errors.add("Línia " + lineNumber + ": " + e.getMessage());
                    } catch (Exception e) {
                        failCount++;
                        errors.add("Línia " + lineNumber + ": Error: " + e.getMessage());
                    }
                } else {
                    failCount++;
                    errors.add("Línia " + lineNumber + ": Pocs camps a la línia (mínim 3)");
                }
            }
        } catch (Exception e) {
            log.error("Error reading CSV file", e);
            throw new RuntimeException("Error processant el CSV: " + e.getMessage());
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("successful", successCount);
        result.put("failed", failCount);
        result.put("errors", errors);
        return result;
    }

    /**
     * Saves a citizen-submitted measurement to the database.
     *
     * @param station   The station entity.
     * @param pollutant The pollutant type.
     * @param value     The measured value.
     * @param aqi       The calculated AQI.
     * @param timestamp The calculated timestamp.
     */
    private void saveMeasurement(Station station, Pollutant pollutant, Double value, Integer aqi, LocalDateTime timestamp) {
        try {
            measurementRepository.saveMeasurementNative(
                    station.getId(),
                    pollutant.name(),
                    value,
                    timestamp,
                    aqi
            );

        } catch (Exception e) {
            log.error("Error saving citizen data: {}", e.getMessage());
            throw new RuntimeException("Database error");
        }
    }
}
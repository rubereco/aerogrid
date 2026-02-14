package com.aerogrid.backend.ingestion.gencat;

import com.aerogrid.backend.domain.Measurement;
import com.aerogrid.backend.domain.Pollutant;
import com.aerogrid.backend.domain.Station;
import com.aerogrid.backend.ingestion.common.CommonMapper;
import com.aerogrid.backend.ingestion.common.CommonMeasurementDto;
import com.aerogrid.backend.ingestion.common.CommonStationDto;
import com.aerogrid.backend.ingestion.common.DataImportProvider;
import com.aerogrid.backend.repository.MeasurementRepository;
import com.aerogrid.backend.repository.StationRepository;
import com.aerogrid.backend.service.AqiCalculatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for importing data from the Generalitat de Catalunya API.
 * Handles fetching, mapping, and saving stations and measurements.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GencatImportService implements DataImportProvider {

    private final GencatApiClient apiClient;
    private final GencatMapper mapper;
    private final CommonMapper commonMapper;
    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;
    private final AqiCalculatorService aqiCalculatorService;
    private final Map<String, Station> stationCache = new HashMap<>();
    private int newStation = 0;
    private int newMeasurement = 0;


    /**
     * Imports all unique stations from the Gencat API and saves them to the database.
     */
    @Override
    public void importStations() {
        log.info("Starting station import for {}", getProviderName());
        List<GencatRawDto> rawData = apiClient.getStations();
        newStation = 0;

        for (GencatRawDto raw : rawData) {
            CommonStationDto station = mapper.toStationDto(raw);

            saveToDatabase(station);
        }
        log.info("Station import completed for {}. Added: {}", getProviderName(), newStation);
    }

    /**
     * Imports measurements for the current day.
     */
    @Override
    public void importMeasurements() {
        log.info("Starting current measurement import for {}", getProviderName());

        newMeasurement = 0;
        List<GencatRawDto> rawData = apiClient.getMeasurements(LocalDate.now().toString());

        log.debug("Retrieved {} current measurement records from {}", rawData.size(), getProviderName());

        processMeasurementRecords(rawData);

        log.info("Current measurement import completed for {}. New data: {}", getProviderName(), newMeasurement);
    }

    /**
     * Imports measurements for a specific date.
     *
     * @param date The date to import measurements for.
     */
    @Override
    public void importMeasurements(LocalDate date) {
        log.info("Ingesting historical data for day: {}", date);

        newMeasurement = 0;
        List<GencatRawDto> rawData = apiClient.getMeasurements(date.toString());

        if (rawData.isEmpty()) {
            log.warn("No data found for day {}", date);
            return;
        }

        processMeasurementRecords(rawData);

        log.info("Day {} completed. {} records processed. New data: {}", date, rawData.size(), newMeasurement);
    }

    /**
     * Processes a list of raw measurement records, handling station creation if necessary.
     *
     * @param rawData List of raw data DTOs.
     */
    private void processMeasurementRecords(List<GencatRawDto> rawData) {
        log.debug("Loading station catalog into memory...");
        stationRepository.findAll().forEach(s -> stationCache.put(s.getCode(), s));

        for (GencatRawDto raw : rawData) {

            Station station = stationCache.get(raw.getStationCode());

            if (station == null) {
                try {
                    log.info("Unknown station detected: {}. Attempting to create it...", raw.getStationCode());

                    CommonStationDto tempStation = mapper.toStationDto(raw);
                    saveToDatabase(tempStation);

                    station = stationRepository.findByCode(raw.getStationCode()).orElse(null);

                    if (station != null) {
                        stationCache.put(raw.getStationCode(), station);
                    } else {
                        continue;
                    }
                } catch (Exception e) {
                    log.error("Error handling new station: {}", e.getMessage());
                    continue;
                }
            }

            List<CommonMeasurementDto> measurements = mapper.toMeasurementDtos(raw);

            for (CommonMeasurementDto dto : measurements) {
                saveToDatabase(dto, station);
            }

            log.debug("Processed {} current measurement records from {}", measurements.size(), getProviderName());
        }
    }

    /**
     * Saves a single measurement to the database.
     *
     * @param dto     The measurement DTO.
     * @param station The associated station entity.
     */
    private void saveToDatabase(CommonMeasurementDto dto, Station station) {
        Pollutant pollutant = commonMapper.mapPollutantString(dto.getPollutant());

        if (pollutant == null) return;

        try {

            measurementRepository.saveMeasurementNative(
                    station.getId(),
                    pollutant.name(),
                    dto.getValue(),
                    dto.getTimestamp(),
                    aqiCalculatorService.calculateAqi(pollutant.name(), dto.getValue())
            );
            newMeasurement++;

        } catch (Exception e) {
            log.error("Critical error inserting measurement: {}", e.getMessage());
        }
    }

    /**
     * Saves a station to the database if it does not already exist.
     *
     * @param dto The station DTO.
     */
    private void saveToDatabase(CommonStationDto dto) {
        try {
            if (stationRepository.findByCode(dto.getCode()).isPresent()) {
                log.debug("Station {} already exists. Skipping...", dto.getCode());
                return;
            }

            Station station = commonMapper.toEntity(dto);
            stationRepository.save(station);
            newStation++;
            log.debug("New station added: {}", dto.getCode());

        } catch (Exception e) {
            log.error("Unexpected error saving station {}: {}", dto.getCode(), e.getMessage());
        }
    }

    /**
     * Returns the name of the data provider.
     *
     * @return The provider name.
     */
    @Override
    public String getProviderName() {
        return "GenCat";
    }
}
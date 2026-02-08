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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GencatImportService implements DataImportProvider {

    private final GencatApiClient apiClient;
    private final GencatMapper mapper;
    private final CommonMapper commonMapper;
    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;
    private final Map<String, Station> stationCache = new HashMap<>();
    private int newStation = 0;
    private int newMeasurement = 0;

    public GencatImportService(GencatApiClient apiClient, GencatMapper mapper,
                               CommonMapper commonMapper,
                               StationRepository stationRepository,
                               MeasurementRepository measurementRepository) {
        this.apiClient = apiClient;
        this.mapper = mapper;
        this.commonMapper = commonMapper;
        this.stationRepository = stationRepository;
        this.measurementRepository = measurementRepository;
    }


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

    @Override
    public void importMeasurements() {
        log.info("Starting current measurement import for {}", getProviderName());

        newMeasurement = 0;
        List<GencatRawDto> rawData = apiClient.getMeasurements(LocalDate.now().toString());

        log.debug("Retrieved {} current measurement records from {}", rawData.size(), getProviderName());

        processMeasurementRecords(rawData);

        log.info("Current measurement import completed for {}. New data: {}", getProviderName(), newMeasurement);
    }

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

    private void processMeasurementRecords(List<GencatRawDto> rawData) {
        log.debug("Carregant catàleg d'estacions a memòria...");
        stationRepository.findAll().forEach(s -> stationCache.put(s.getCode(), s));

        for (GencatRawDto raw : rawData) {

            Station station = stationCache.get(raw.getStationCode());

            if (station == null) {
                try {
                    log.info("Estació desconeguda detectada: {}. Intentant crear-la...", raw.getStationCode());

                    CommonStationDto tempStation = mapper.toStationDto(raw);
                    saveToDatabase(tempStation);

                    station = stationRepository.findByCode(raw.getStationCode()).orElse(null);

                    if (station != null) {
                        stationCache.put(raw.getStationCode(), station);
                    } else {
                        continue;
                    }
                } catch (Exception e) {
                    log.error("Error gestionant nova estació: {}", e.getMessage());
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

    private void saveToDatabase(CommonMeasurementDto dto, Station station) {
        Pollutant pollutant = commonMapper.mapPollutantString(dto.getPollutant());

        if (pollutant == null) return;

        try {

            measurementRepository.saveMeasurementNative(
                    station.getId(),
                    pollutant.name(),
                    dto.getValue(),
                    dto.getTimestamp(),
                    null
            );
            newMeasurement++;

        } catch (Exception e) {
            log.error("Error crític insertant mesura: {}", e.getMessage());
        }
    }

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

    @Override
    public String getProviderName() {
        return "GenCat";
    }
}
package com.aerogrid.backend.ingestion.gencat;

import com.aerogrid.backend.domain.Measurement;
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
import java.util.List;

@Slf4j
@Service
public class GencatImportService implements DataImportProvider {

    private final GencatApiClient apiClient;
    private final GencatMapper mapper;
    private final CommonMapper commonMapper;
    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;
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

        log.info("Retrieved {} current measurement records from {}", rawData.size(), getProviderName());

        for (GencatRawDto raw : rawData) {

            List<CommonMeasurementDto> measurements = mapper.toMeasurementDtos(raw);



            for (CommonMeasurementDto dto : measurements) {
                saveToDatabase(dto);
            }

            log.debug("Processed {} current measurement records from {}", measurements.size(), getProviderName());
        }
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

        for (GencatRawDto raw : rawData) {

            List<CommonMeasurementDto> measurements = mapper.toMeasurementDtos(raw);

            log.debug("Retrieved {} records for day {}", measurements.size(), date);

            for (CommonMeasurementDto dto : measurements) {
                saveToDatabase(dto);
            }
        }
        log.info("Day {} completed. {} records processed. New data: {}", date, rawData.size(), newMeasurement);
    }

    private void saveToDatabase(CommonMeasurementDto dto) {
        try {
            Station station = stationRepository.findByCode(dto.getStationCode())
                    .orElseThrow(() -> new RuntimeException("Station not found: " + dto.getStationCode()));

            Measurement measurement = commonMapper.toEntity(dto, station);

            if (measurement == null) {
                log.warn("Null measurement after mapping. Skipping...");
                return;
            }

            measurementRepository.save(measurement);
            newMeasurement++;
            log.debug("Measurement saved for station {}: {} = {} at {}",
                    dto.getStationCode(), dto.getPollutant(), dto.getValue(), dto.getTimestamp());

        } catch (DataIntegrityViolationException e) {
            log.debug("Could not save measurement for station {}: {}", dto.getStationCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error saving measurement for station {}: {}", dto.getStationCode(), e.getMessage());
        }
    }

    private void saveToDatabase(CommonStationDto dto) {
        try {
            Station station = commonMapper.toEntity(dto);
            stationRepository.save(station);
            newStation++;
            log.debug("New station added: {}", dto.getCode());

        } catch (DataIntegrityViolationException e) {
            log.debug("Station {} already exists. Skipping...", dto.getCode());
        } catch (Exception e) {
            log.error("Unexpected error saving station {}: {}", dto.getCode(), e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "GenCat";
    }
}
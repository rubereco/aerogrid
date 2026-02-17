package com.aerogrid.backend.controller.mapper;

import com.aerogrid.backend.controller.dto.MeasurementDto;
import com.aerogrid.backend.domain.Measurement;
import com.aerogrid.backend.domain.Pollutant;
import com.aerogrid.backend.domain.Station;
import com.aerogrid.backend.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Measurement entity and MeasurementDto.
 */
@Component
@RequiredArgsConstructor
public class MeasurementMapper {

    private final StationRepository stationRepository;

    /**
     * Converts a Measurement entity to MeasurementDto.
     *
     * @param measurement the measurement entity to convert
     * @return the measurement DTO
     */
    public MeasurementDto toDto(Measurement measurement) {
        if (measurement == null) {
            return null;
        }

        return MeasurementDto.builder()
                .stationCode(measurement.getStation() != null ? measurement.getStation().getCode() : null)
                .pollutant(measurement.getPollutant() != null ? measurement.getPollutant().name() : null)
                .value(measurement.getValue())
                .timestamp(measurement.getTimestamp())
                .build();
    }

    /**
     * Converts a MeasurementDto to Measurement entity.
     * Requires the station to exist in the database.
     *
     * @param dto the measurement DTO to convert
     * @return the measurement entity
     * @throws RuntimeException if the station code is not found
     */
    public Measurement toEntity(MeasurementDto dto) {
        if (dto == null) {
            return null;
        }

        Station station = null;
        if (dto.getStationCode() != null) {
            station = stationRepository.findByCode(dto.getStationCode())
                    .orElseThrow(() -> new RuntimeException("Station not found: " + dto.getStationCode()));
        }

        return Measurement.builder()
                .station(station)
                .pollutant(dto.getPollutant() != null ? Pollutant.valueOf(dto.getPollutant()) : null)
                .value(dto.getValue())
                .timestamp(dto.getTimestamp())
                .build();
    }

    /**
     * Converts a MeasurementDto to Measurement entity with a provided Station.
     * This is more efficient when the station is already loaded.
     *
     * @param dto the measurement DTO to convert
     * @param station the station entity to associate with the measurement
     * @return the measurement entity
     */
    public Measurement toEntity(MeasurementDto dto, Station station) {
        if (dto == null) {
            return null;
        }

        return Measurement.builder()
                .station(station)
                .pollutant(dto.getPollutant() != null ? Pollutant.valueOf(dto.getPollutant()) : null)
                .value(dto.getValue())
                .timestamp(dto.getTimestamp())
                .build();
    }
}

package com.aerogrid.backend.ingestion.common;

import com.aerogrid.backend.domain.Measurement;
import com.aerogrid.backend.domain.Pollutant;
import com.aerogrid.backend.domain.SourceType;
import com.aerogrid.backend.domain.Station;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;

/**
 * Mapper class to convert common DTOs to domain entities.
 */
@Component
public class CommonMapper {

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    /**
     * Converts a CommonStationDto to a Station entity.
     *
     * @param dto the station DTO to convert
     * @return the Station entity
     */
    public Station toEntity(CommonStationDto dto) {
        Point location = GEOMETRY_FACTORY.createPoint(
                new Coordinate(dto.getLongitude(), dto.getLatitude())
        );

        SourceType sourceType = dto.getType() != null && dto.getType().equalsIgnoreCase("CITIZEN")
                ? SourceType.CITIZEN
                : SourceType.OFFICIAL;

        return Station.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .municipality(dto.getMunicipality())
                .location(location)
                .sourceType(sourceType)
                .isActive(true)
                .build();
    }

    /**
     * Converts a CommonMeasurementDto to a Measurement entity.
     *
     * @param dto the measurement DTO to convert
     * @param station the associated station entity
     * @return the Measurement entity
     */
    public Measurement toEntity(CommonMeasurementDto dto, Station station) {

        Pollutant pollutant = mapPollutantString(dto.getPollutant());

        if (pollutant == null) {
            return null;
        }

        return Measurement.builder()
                .station(station)
                .pollutant(pollutant)
                .value(dto.getValue())
                .timestamp(dto.getTimestamp())
                .build();
    }

    /**
     * This method acts as a filter.
     * It only returns values for the pollutants we are interested in.
     * Others return null.
     */
    private Pollutant mapPollutantString(String raw) {
        if (raw == null) return null;

        String clean = raw.trim().toUpperCase();

        return switch (clean) {
            case "PM10" -> Pollutant.PM10;
            case "PM2.5" -> Pollutant.PM25;
            case "PM1" -> Pollutant.PM1;
            case "NO2" -> Pollutant.NO2;
            case "O3" -> Pollutant.O3;
            case "SO2" -> Pollutant.SO2;
            case "CO" -> Pollutant.CO;
            case "H2S" -> Pollutant.H2S;
            case "C6H6" -> Pollutant.C6H6;

            default -> null;
        };
    }
}

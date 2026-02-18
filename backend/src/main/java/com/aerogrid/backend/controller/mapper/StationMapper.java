package com.aerogrid.backend.controller.mapper;

import com.aerogrid.backend.controller.dto.StationDetailsDto;
import com.aerogrid.backend.controller.dto.StationMapDto;
import com.aerogrid.backend.domain.Station;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Station entity and StationMapDto.
 */
@Component
public class StationMapper {

    private static final int SRID_WGS84 = 4326;
    private final GeometryFactory geometryFactory;

    public StationMapper() {
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), SRID_WGS84);
    }

    /**
     * Converts a Station entity to StationMapDto.
     * Note: currentAqi and worstPollutant are not populated and must be set separately.
     *
     * @param station the station entity to convert
     * @return the station map DTO
     */
    public StationMapDto toDto(Station station) {
        if (station == null) {
            return null;
        }

        return StationMapDto.builder()
                .id(station.getId())
                .code(station.getCode())
                .name(station.getName())
                .latitude(station.getLocation() != null ? station.getLocation().getY() : null)
                .longitude(station.getLocation() != null ? station.getLocation().getX() : null)
                .currentAqi(null)  // Must be set from latest measurement
                .worstPollutant(null)  // Must be set from latest measurement
                .build();
    }

    /**
     * Converts a StationMapDto to Station entity.
     * Note: Only basic fields are populated. Additional fields like sourceType,
     * municipality, owner, etc. must be set separately.
     *
     * @param dto the station map DTO to convert
     * @return the station entity
     */
    public Station toEntity(StationMapDto dto) {
        if (dto == null) {
            return null;
        }

        Point location = null;
        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            location = geometryFactory.createPoint(new Coordinate(dto.getLongitude(), dto.getLatitude()));
        }

        return Station.builder()
                .id(dto.getId())
                .code(dto.getCode())
                .name(dto.getName())
                .location(location)
                .build();
    }

    /**
     * Converts a Station entity to StationDetailsDto.
     * Includes all station information except owner relationship.
     *
     * @param station the station entity to convert
     * @return the station details DTO
     */
    public StationDetailsDto toDetailsDto(Station station) {
        if (station == null) {
            return null;
        }

        return StationDetailsDto.builder()
                .id(station.getId())
                .code(station.getCode())
                .name(station.getName())
                .municipality(station.getMunicipality())
                .latitude(station.getLocation() != null ? station.getLocation().getY() : null)
                .longitude(station.getLocation() != null ? station.getLocation().getX() : null)
                .sourceType(station.getSourceType() != null ? station.getSourceType().name() : null)
                .trustScore(station.getTrustScore())
                .isActive(station.getIsActive())
                .createdAt(station.getCreatedAt())
                .updatedAt(station.getUpdatedAt())
                .build();
    }

    /**
     * Converts a StationDetailsDto to Station entity.
     * Note: Owner relationship is not populated from DTO.
     *
     * @param dto the station details DTO to convert
     * @return the station entity
     */
    public Station toEntityFromDetails(StationDetailsDto dto) {
        if (dto == null) {
            return null;
        }

        Point location = null;
        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            location = geometryFactory.createPoint(new Coordinate(dto.getLongitude(), dto.getLatitude()));
        }

        return Station.builder()
                .id(dto.getId())
                .code(dto.getCode())
                .name(dto.getName())
                .municipality(dto.getMunicipality())
                .location(location)
                .trustScore(dto.getTrustScore())
                .isActive(dto.getIsActive())
                .build();
    }
}

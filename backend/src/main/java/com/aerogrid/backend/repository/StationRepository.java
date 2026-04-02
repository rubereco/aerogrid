package com.aerogrid.backend.repository;

import com.aerogrid.backend.controller.dto.StationMapDto;
import com.aerogrid.backend.domain.Station;
import com.aerogrid.backend.repository.projection.StationMapProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Station entity.
 */
@Repository
public interface StationRepository extends JpaRepository<Station, Long> {

    /**
     * Finds a station by its unique code.
     *
     * @param code the station code
     * @return an Optional containing the station if found
     */
    Optional<Station> findByCode(String code);

    @Query(value = """
        SELECT s.id as id, s.code as code, s.name as name,
               CAST(ST_Y(s.location) AS double precision) as latitude,
               CAST(ST_X(s.location) AS double precision) as longitude,
               a.max_aqi as aqi, a.pollutant as pollutant
        FROM stations s
        LEFT JOIN LATERAL (
            SELECT max_aqi, pollutant
            FROM hourly_aqi_snapshots h
            WHERE h.station_id = s.id
            ORDER BY h.timestamp DESC LIMIT 1
        ) a ON true
        WHERE s.owner_id = :userId
        """, nativeQuery = true)
    List<StationMapProjection> findByOwnerIdProjection(@org.springframework.data.repository.query.Param("userId") Long userId);

    /**
     * Finds all active stations within a specified distance from a geographic point.
     * Uses PostGIS spatial functions with SRID 4326.
     *
     * @param lat latitude of the center point
     * @param lon longitude of the center point
     * @param distanceInDegrees search radius in degrees
     * @return list of stations within the specified distance
     */
    @Query(value = """
        SELECT s.* FROM stations s
        WHERE ST_DWithin(
            s.location, 
            ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), 
            :distanceInDegrees
        )
        AND s.is_active = true
        """, nativeQuery = true)
    List<Station> findStationsNearby(double lat, double lon, double distanceInDegrees);

    /**
     * Retrieves all active stations with their latest AQI and worst pollutant.
     * Optimized query that fetches all data in a single database call.
     *
     * @return list of station map DTOs with current status
     */
    @Query(value = """
        SELECT s.id as id, s.code as code, s.name as name,
               CAST(ST_Y(s.location) AS double precision) as latitude,
               CAST(ST_X(s.location) AS double precision) as longitude,
               a.max_aqi as aqi, a.pollutant as pollutant
        FROM stations s
        LEFT JOIN LATERAL (
            SELECT max_aqi, pollutant
            FROM hourly_aqi_snapshots h
            WHERE h.station_id = s.id
            ORDER BY h.timestamp DESC LIMIT 1
        ) a ON true
        WHERE s.is_active = true
        """, nativeQuery = true)
    List<StationMapProjection> findAllStationsWithStatus();

    /**
     * Cerca estacions dins d'un rectangle (Bounding Box).
     * Ideal per carregar el mapa (Viewport).
     *
     * @param minLon Longitud mínima (Oest) - XMin
     * @param minLat Latitud mínima (Sud) - YMin
     * @param maxLon Longitud màxima (Est) - XMax
     * @param maxLat Latitud màxima (Nord) - YMax
     */
    @Query(value = """
        SELECT s.id as id, s.code as code, s.name as name,
               CAST(ST_Y(s.location) AS double precision) as latitude,
               CAST(ST_X(s.location) AS double precision) as longitude,
               a.max_aqi as aqi, a.pollutant as pollutant
        FROM stations s
        LEFT JOIN LATERAL (
            SELECT max_aqi, pollutant
            FROM hourly_aqi_snapshots h
            WHERE h.station_id = s.id
            ORDER BY h.timestamp DESC LIMIT 1
        ) a ON true
        WHERE s.location && ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
        AND s.is_active = true
        """, nativeQuery = true)
    List<StationMapProjection> findStationsInBoundingBox(
            @org.springframework.data.repository.query.Param("minLon") double minLon,
            @org.springframework.data.repository.query.Param("minLat") double minLat,
            @org.springframework.data.repository.query.Param("maxLon") double maxLon,
            @org.springframework.data.repository.query.Param("maxLat") double maxLat);
}
package com.aerogrid.backend.repository;

import com.aerogrid.backend.controller.dto.StationMapDto;
import com.aerogrid.backend.domain.Station;
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

    /**
     * Finds all stations owned by a specific user.
     *
     * @param userId the owner's user ID
     * @return list of stations owned by the user
     */
    List<Station> findByOwnerId(Long userId);

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
    @Query("""
       SELECT new com.aerogrid.backend.controller.dto.StationMapDto(
           s.id, s.code, s.name, 
           CAST(ST_Y(s.location) AS double), 
           CAST(ST_X(s.location) AS double),
           (SELECT m.aqi FROM Measurement m WHERE m.station = s ORDER BY m.timestamp DESC LIMIT 1),
           (SELECT m.pollutant FROM Measurement m WHERE m.station = s ORDER BY m.timestamp DESC LIMIT 1)
       )
       FROM Station s
       WHERE s.isActive = true
       """)
    List<StationMapDto> findAllStationsWithStatus();

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
        SELECT * FROM stations s
        WHERE s.location && ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
        AND s.is_active = true
        """, nativeQuery = true)
    List<Station> findStationsInBoundingBox(double minLon, double minLat, double maxLon, double maxLat);
}
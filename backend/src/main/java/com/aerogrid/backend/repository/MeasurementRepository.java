package com.aerogrid.backend.repository;

import com.aerogrid.backend.domain.Measurement;
import com.aerogrid.backend.repository.projection.AggregatedMeasurementProjection;
import com.aerogrid.backend.repository.projection.HourlyAqiNativeProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Measurement entity.
 */
@Repository
public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    /**
     * Saves a measurement using native SQL with conflict resolution.
     * If a measurement already exists for the same station, timestamp, and pollutant, it will be ignored.
     *
     * @param stationId the station ID
     * @param pollutant the pollutant type
     * @param value the measurement value
     * @param timestamp the measurement timestamp
     * @param aqi the calculated AQI value
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO measurements (station_id, pollutant, value, timestamp, aqi)
        VALUES (:stationId, :pollutant, :value, :timestamp, :aqi)
        ON CONFLICT (station_id, timestamp, pollutant) DO NOTHING
        """, nativeQuery = true)
    void saveMeasurementNative(Long stationId, String pollutant, Double value, LocalDateTime timestamp, Integer aqi);
    /**
     * Finds the most recent measurement timestamp in the database.
     *
     * @return the latest timestamp, or null if no measurements exist
     */
    @Query("SELECT MAX(m.timestamp) FROM Measurement m")
    LocalDateTime findLatestTimestamp();

    /**
     * Finds measurements for a specific station within a time range.
     *
     * @param stationCode the station code
     * @param start the start timestamp (inclusive)
     * @param end the end timestamp (inclusive)
     * @return list of measurements ordered by timestamp
     */
    @Query("SELECT m FROM Measurement m WHERE m.station.code = :stationCode AND m.timestamp BETWEEN :start AND :end ORDER BY m.timestamp ASC")
    List<Measurement> findByStationCodeAndTimestampBetween(@Param("stationCode") String stationCode,
                                                            @Param("start") LocalDateTime start,
                                                            @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT m.station_id as stationId, 
               DATE_TRUNC('hour', m.timestamp) as timestamp, 
               MAX(m.aqi) as maxAqi, 
               MAX(m.pollutant) as pollutant
        FROM measurements m 
        WHERE m.timestamp BETWEEN :start AND :end 
        GROUP BY m.station_id, DATE_TRUNC('hour', m.timestamp)
        """, nativeQuery = true)
    List<HourlyAqiNativeProjection> findMaxAqiBetweenNative(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT 
            DATE_TRUNC('hour', m.timestamp) AS timestamp,
            m.pollutant AS pollutant,
            AVG(m.value) AS avgValue,
            AVG(m.aqi) AS avgAqi
        FROM measurements m JOIN stations s ON m.station_id = s.id
        WHERE s.code = :stationCode AND m.timestamp BETWEEN :start AND :end
        GROUP BY DATE_TRUNC('hour', m.timestamp), m.pollutant
        ORDER BY timestamp ASC
        """, nativeQuery = true)
    List<AggregatedMeasurementProjection> aggregateHourly(@Param("stationCode") String stationCode,
                                                        @Param("start") LocalDateTime start,
                                                        @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT 
            date_bin('6 hours', m.timestamp, TIMESTAMP '2024-01-01 00:00:00') AS timestamp,
            m.pollutant AS pollutant,
            AVG(m.value) AS avgValue,
            AVG(m.aqi) AS avgAqi
        FROM measurements m JOIN stations s ON m.station_id = s.id
        WHERE s.code = :stationCode AND m.timestamp BETWEEN :start AND :end
        GROUP BY date_bin('6 hours', m.timestamp, TIMESTAMP '2024-01-01 00:00:00'), m.pollutant
        ORDER BY timestamp ASC
        """, nativeQuery = true)
    List<AggregatedMeasurementProjection> aggregateSixHours(@Param("stationCode") String stationCode,
                                                            @Param("start") LocalDateTime start,
                                                            @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT 
            DATE_TRUNC('day', m.timestamp) AS timestamp,
            m.pollutant AS pollutant,
            AVG(m.value) AS avgValue,
            AVG(m.aqi) AS avgAqi
        FROM measurements m JOIN stations s ON m.station_id = s.id
        WHERE s.code = :stationCode AND m.timestamp BETWEEN :start AND :end
        GROUP BY DATE_TRUNC('day', m.timestamp), m.pollutant
        ORDER BY timestamp ASC
        """, nativeQuery = true)
    List<AggregatedMeasurementProjection> aggregateDaily(@Param("stationCode") String stationCode,
                                                        @Param("start") LocalDateTime start,
                                                        @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT 
            DATE_TRUNC('week', m.timestamp) AS timestamp,
            m.pollutant AS pollutant,
            AVG(m.value) AS avgValue,
            AVG(m.aqi) AS avgAqi
        FROM measurements m JOIN stations s ON m.station_id = s.id
        WHERE s.code = :stationCode AND m.timestamp BETWEEN :start AND :end
        GROUP BY DATE_TRUNC('week', m.timestamp), m.pollutant
        ORDER BY timestamp ASC
        """, nativeQuery = true)
    List<AggregatedMeasurementProjection> aggregateWeekly(@Param("stationCode") String stationCode,
                                                        @Param("start") LocalDateTime start,
                                                        @Param("end") LocalDateTime end);
}
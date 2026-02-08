package com.aerogrid.backend.repository;

import com.aerogrid.backend.domain.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Repository for Measurement entity.
 */
@Repository
public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    @Modifying // Indica que és una query d'escriptura
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
}
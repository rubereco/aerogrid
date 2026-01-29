package com.aerogrid.backend.repository;

import com.aerogrid.backend.domain.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Repository for Measurement entity.
 */
@Repository
public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    /**
     * Finds the most recent measurement timestamp in the database.
     *
     * @return the latest timestamp, or null if no measurements exist
     */
    @Query("SELECT MAX(m.timestamp) FROM Measurement m")
    LocalDateTime findLatestTimestamp();
}
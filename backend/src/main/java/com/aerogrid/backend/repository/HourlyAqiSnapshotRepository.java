package com.aerogrid.backend.repository;

import com.aerogrid.backend.domain.HourlyAqiSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface HourlyAqiSnapshotRepository extends JpaRepository<HourlyAqiSnapshot, Long> {
    boolean existsByStationIdAndTimestamp(Long stationId, LocalDateTime timestamp);
}

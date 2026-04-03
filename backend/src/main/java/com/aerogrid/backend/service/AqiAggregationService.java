package com.aerogrid.backend.service;

import com.aerogrid.backend.domain.HourlyAqiSnapshot;
import com.aerogrid.backend.domain.Station;
import com.aerogrid.backend.repository.HourlyAqiSnapshotRepository;
import com.aerogrid.backend.repository.MeasurementRepository;
import com.aerogrid.backend.repository.StationRepository;
import com.aerogrid.backend.repository.projection.HourlyAqiNativeProjection;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AqiAggregationService {

    private final MeasurementRepository measurementRepository;
    private final HourlyAqiSnapshotRepository hourlyAqiSnapshotRepository;
    private final StationRepository stationRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    @PostConstruct
    public void aggregateHourlyAqi() {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(2);

        log.info("Starting AQI aggregation for window: {} to {}", start, end);

        List<HourlyAqiNativeProjection> aggregates = measurementRepository.findMaxAqiBetweenNative(start, end);
        int savedCount = 0;

        for (HourlyAqiNativeProjection proj : aggregates) {
            try {
                boolean exists = hourlyAqiSnapshotRepository.existsByStationIdAndTimestamp(
                        proj.getStationId(), proj.getTimestamp()
                );

                if (!exists) {
                    Station station = stationRepository.findById(proj.getStationId()).orElse(null);
                    if (station != null) {
                        HourlyAqiSnapshot snapshot = HourlyAqiSnapshot.builder()
                                .station(station)
                                .timestamp(proj.getTimestamp())
                                .maxAqi(proj.getMaxAqi())
                                .pollutant(proj.getPollutant())
                                .build();

                        hourlyAqiSnapshotRepository.save(snapshot);
                        savedCount++;
                    }
                }
            } catch (Exception e) {
                log.error("Failed to save snapshot for stationId: {} at {}", proj.getStationId(), proj.getTimestamp(), e);
            }
        }

        log.info("Completed AQI aggregation. Processed {} hourly aggregates, saved {} new snapshots.", aggregates.size(), savedCount);
    }
}

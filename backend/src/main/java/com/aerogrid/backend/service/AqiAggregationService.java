package com.aerogrid.backend.service;

import com.aerogrid.backend.domain.HourlyAqiSnapshot;
import com.aerogrid.backend.domain.Station;
import com.aerogrid.backend.repository.HourlyAqiSnapshotRepository;
import com.aerogrid.backend.repository.MeasurementRepository;
import com.aerogrid.backend.repository.StationRepository;
import com.aerogrid.backend.repository.projection.HourlyAqiNativeProjection;
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
    public void aggregateHourlyAqi() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        LocalDateTime start = now.minusHours(1);
        LocalDateTime end = now.minusNanos(1);

        log.info("Starting hourly AQI aggregation for window: {} to {}", start, end);

        List<HourlyAqiNativeProjection> aggregates = measurementRepository.findMaxAqiBetweenNative(start, end);

        for (HourlyAqiNativeProjection proj : aggregates) {
            try {
                Station station = stationRepository.findById(proj.getStationId()).orElse(null);
                if (station != null) {
                    HourlyAqiSnapshot snapshot = HourlyAqiSnapshot.builder()
                            .station(station)
                            .timestamp(start)
                            .maxAqi(proj.getMaxAqi())
                            .pollutant(proj.getPollutant())
                            .build();

                    hourlyAqiSnapshotRepository.save(snapshot);
                }
            } catch (Exception e) {
                log.error("Failed to save snapshot for stationId: {}", proj.getStationId(), e);
            }
        }

        log.info("Completed hourly AQI aggregation. Processed {} stations.", aggregates.size());
    }
}

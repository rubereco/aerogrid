package com.aerogrid.backend.ingestion;

import com.aerogrid.backend.ingestion.common.DataImportProvider;
import com.aerogrid.backend.repository.MeasurementRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataIngestionFacade {

    private final List<DataImportProvider> providers;
    private final MeasurementRepository measurementRepository;


    @PostConstruct
    public void onStartup() {

        log.info("Initializing data ingestion on startup...");

        // Import stations first
        for (DataImportProvider provider : providers) {
            try {
                provider.importStations();
            } catch (Exception e) {
                log.error("Error importing stations for provider {}: {}", provider.getProviderName(), e.getMessage());
            }
        }

        // Check most recent measurement and import missing data
        LocalDateTime mostRecentMeasurement = measurementRepository.findLatestTimestamp();
        LocalDate today = LocalDate.now();

        if (mostRecentMeasurement != null) {
            LocalDate lastMeasurementDate = mostRecentMeasurement.toLocalDate();
            log.info("Most recent measurement date: {}, Today: {}", lastMeasurementDate, today);

            if (lastMeasurementDate.equals(today)) {
                // Only import today
                log.info("Importing today's data...");
                for (DataImportProvider provider : providers) {
                    try {
                        provider.importMeasurements(today);
                    } catch (Exception e) {
                        log.error("Error importing today's data for provider {}: {}", provider.getProviderName(), e.getMessage());
                    }
                }
            } else {
                // Import from lastMeasurementDate to today (inclusive)
                log.info("Importing data from {} to {}", lastMeasurementDate, today);
                LocalDate currentDate = lastMeasurementDate;
                while (!currentDate.isAfter(today)) {
                    LocalDate dateToImport = currentDate;
                    log.info("Importing data for date: {}", dateToImport);
                    for (DataImportProvider provider : providers) {
                        try {
                            provider.importMeasurements(dateToImport);
                        } catch (Exception e) {
                            log.error("Error importing data for {} from provider {}: {}", dateToImport, provider.getProviderName(), e.getMessage());
                        }
                    }
                    currentDate = currentDate.plusDays(1);
                }
            }
        } else {
            // No measurements in database, import today
            log.info("No measurements found in database. Importing today's data...");
            for (DataImportProvider provider : providers) {
                try {
                    provider.importMeasurements(today);
                } catch (Exception e) {
                    log.error("Error importing today's data for provider {}: {}", provider.getProviderName(), e.getMessage());
                }
            }
        }

    }

    /**
     * This method will execute automatically every hour.
     * Cron: Second 0, Minute 0, every Hour, every Day...
     */
    @Scheduled(cron = "0 0 * * * *")
    public void runAllImports() {
        log.info("--- STARTING GLOBAL INGESTION PROCESS ---");

        for (DataImportProvider provider : providers) {
            try {
                provider.importStations();

                provider.importMeasurements();

            } catch (Exception e) {
                log.error("Error in provider {}: {}", provider.getProviderName(), e.getMessage());
            }
        }

        log.info("--- END OF GLOBAL INGESTION PROCESS ---");
    }

    /**
     * Manual method to recover old data.
     * @param daysToLookBack How many days back (e.g., 30 days)
     */
    public void triggerBackfill(int daysToLookBack) {
        log.info("STARTING BACKFILL FOR {} DAYS BACK...", daysToLookBack);

        for (DataImportProvider provider : providers) {
            provider.importStations();
        }

        LocalDateTime lastDateInDb = measurementRepository.findLatestTimestamp();

        LocalDate startDate = (lastDateInDb != null) ? lastDateInDb.toLocalDate() : LocalDate.now();

        for (int i = 0; i < daysToLookBack; i++) {
            LocalDate targetDate = startDate.minusDays(i);

            log.info("Processing day {}/{} -> Date: {}", i + 1, daysToLookBack, targetDate);

            for (DataImportProvider provider : providers) {
                try {
                    provider.importMeasurements(targetDate);

                    Thread.sleep(500);

                } catch (Exception e) {
                    log.error("Error in backfill day {} provider {}: {}", targetDate, provider.getProviderName(), e.getMessage());
                }
            }
        }
        log.info("BACKFILL COMPLETED.");
    }
}
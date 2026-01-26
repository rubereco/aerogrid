package com.aerogrid.backend.ingestion;

import com.aerogrid.backend.ingestion.common.DataImportProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataIngestionFacade {

    private final List<DataImportProvider> providers;

    /**
     * Aquest mètode s'executarà cada hora automàticament.
     * Cron: Segon 0, Minut 0, cada Hora, cada Dia...
     */
    @Scheduled(cron = "0 0 * * * *")
    public void runAllImports() {
        log.info("--- INICIANT PROCES D'INGESTA GLOBAL ---");

        for (DataImportProvider provider : providers) {
            try {
                provider.importStations();

                provider.importMeasurements();

            } catch (Exception e) {
                log.error("Error al proveïdor {}: {}", provider.getProviderName(), e.getMessage());
            }
        }

        log.info("--- FI DEL PROCES D'INGESTA GLOBAL ---");
    }
}
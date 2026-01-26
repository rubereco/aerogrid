package com.aerogrid.backend.ingestion.gencat;

import com.aerogrid.backend.domain.Measurement;
import com.aerogrid.backend.ingestion.common.DataImportProvider;
import com.aerogrid.backend.repository.MeasurementRepository;
import com.aerogrid.backend.repository.StationRepository;
import com.aerogrid.backend.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GencatImportService implements DataImportProvider {


    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;

    public GencatImportService(StationRepository stationRepository, MeasurementRepository measurementRepository) {
        this.stationRepository = stationRepository;
        this.measurementRepository = measurementRepository;
    }

    @Override
    public String getProviderName() {
        return "Gencat Open Data";
    }

    @Override
    public void importStations() {
        log.info("Iniciant importació d'estacions de {}", getProviderName());
        // Aquí anirà la lògica de cridar al client i guardar a la BD
    }

    @Override
    public void importMeasurements() {
        log.info("Iniciant importació de mesures de {}", getProviderName());
        // Lògica de mesures
        GencatMeasurementDTO dto = new GencatMeasurementDTO();
        Measurement measurement = new Measurement();
        try {
            measurementRepository.save(measurement);
        } catch (DataIntegrityViolationException e) {
            // Aquesta excepció salta gràcies a la Unique Constraint que hem creat
            log.warn("Dada duplicada ignorada: Estació {} a les {}", dto.getStationCode(), dto.getDate());
            // No fem 'throw', així el bucle continua amb la següent!
        }
    }
}
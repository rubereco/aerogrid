package com.aerogrid.backend.ingestion.common;

import java.time.LocalDate;

/**
 * Interface defining the contract for data import providers.
 * Each provider (e.g., Gencat, specific API) must implement these methods.
 */
public interface DataImportProvider {

    /**
     * Gets the name of the provider.
     * @return the provider name
     */
    String getProviderName();

    /**
     * Imports station metadata from the provider.
     */
    void importStations();

    /**
     * Imports current measurements (typically for the current day).
     */
    void importMeasurements();

    /**
     * Imports historical measurements for a specific date.
     * @param date the date to import data for
     */
    void importMeasurements(LocalDate date);
}
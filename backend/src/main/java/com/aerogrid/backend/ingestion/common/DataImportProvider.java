package com.aerogrid.backend.ingestion.common;

public interface DataImportProvider {

    String getProviderName();

    void importStations();

    void importMeasurements();
}
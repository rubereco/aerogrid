package com.aerogrid.backend.ingestion.gencat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class GencatApiClient {

    private final RestClient restClient;
    private final String apiToken;
    private final String datasetId;

    /**
     * Constructor initializing the REST client with base URL and API token.
     *
     * @param baseUrl  The base URL of the Gencat API.
     * @param apiToken The authentication token for the API.
     * @param datasetId The dataset ID for air quality data.
     */
    public GencatApiClient(@Value("${gencat.api.url}") String baseUrl,
                           @Value("${gencat.api.token}") String apiToken,
                           @Value("${gencat.api.air-quality-dataset-id}") String datasetId) {

        this.apiToken = apiToken;
        this.datasetId = datasetId;

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Retrieves a list of unique stations from the API.
     * Uses SoQL to fetch distinct station metadata, avoiding duplicate measurement data.
     *
     * @return List of GencatRawDto containing station information.
     */
    public List<GencatRawDto> getStations() {
        String fields = "codi_eoi, nom_estacio, municipi, latitud, longitud, tipus_estacio";
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(datasetId)
                        .queryParam("$select", fields)
                        .queryParam("$group", fields)
                        .build())
                .header("X-App-Token", apiToken)
                .retrieve()
                .body(new ParameterizedTypeReference<List<GencatRawDto>>() {});
    }

    /**
     * Downloads measurements starting from a specific date.
     *
     * @param fromDate Date in ISO format (e.g., "2026-01-29T00:00:00").
     * @return List of GencatRawDto containing measurements.
     */
    public List<GencatRawDto> getMeasurements(String fromDate) {
        List<GencatRawDto> allMeasurements = new ArrayList<>();
        int limit = 50000;
        int offset = 0;
        boolean moreData = true;

        while (moreData) {
            int currentOffset = offset;
            List<GencatRawDto> batch = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(datasetId)
                            .queryParam("$where", "data >= '" + fromDate + "'")
                            .queryParam("$limit", String.valueOf(limit))
                            .queryParam("$offset", String.valueOf(currentOffset))
                            .queryParam("$order", "data ASC")
                            .build())
                    .header("X-App-Token", apiToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<GencatRawDto>>() {});

            if (batch == null || batch.isEmpty()) {
                moreData = false;
            } else {
                allMeasurements.addAll(batch);
                offset += limit;
                if (batch.size() < limit) {
                    moreData = false;
                }
            }
        }
        return allMeasurements;
    }
}
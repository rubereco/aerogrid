package com.aerogrid.backend.ingestion.gencat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class GencatApiClient {

    private final RestClient restClient;
    private final String apiToken;

    private static final String DATASET_ID = "/tasf-thgu.json";

    public GencatApiClient(@Value("${gencat.api.url}") String baseUrl,
                           @Value("${gencat.api.token}") String apiToken) {

        this.apiToken = apiToken;

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Descarrega el llistat d'estacions úniques.
     * Utilitza SoQL ($select=DISTINCT) per evitar baixar milions de mesures repetides.
     * Només ens interessa la metadada de l'estació.
     */
    public List<GencatRawDto> getStations() {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(DATASET_ID)
                        .queryParam("$select", "DISTINCT codi_eoi, nom_estacio, municipi, latitud, longitud, tipus_estacio")
                        .build())
                .header("X-App-Token", apiToken)
                .retrieve()
                .body(new ParameterizedTypeReference<List<GencatRawDto>>() {});
    }

    /**
     * Downloads measurements (data + hours) starting from a specific date.
     * @param fromDate Date in ISO format or text (e.g., "2026-01-29T00:00:00")
     */
    public List<GencatRawDto> getMeasurements(String fromDate) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(DATASET_ID)
                        .queryParam("$where", "data >= '" + fromDate + "'")
                        .queryParam("$limit", "50000")
                        .build())
                .header("X-App-Token", apiToken)
                .retrieve()
                .body(new ParameterizedTypeReference<List<GencatRawDto>>() {});
    }
}
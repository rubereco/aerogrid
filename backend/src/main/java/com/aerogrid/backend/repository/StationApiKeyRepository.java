package com.aerogrid.backend.repository;

import com.aerogrid.backend.controller.dto.StationMapDto;
import com.aerogrid.backend.domain.Station;
import com.aerogrid.backend.domain.StationApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing station API keys.
 */
@Repository
public interface StationApiKeyRepository extends JpaRepository<StationApiKey, Long> {

    /**
     * Finds an active API key and eagerly fetches its associated station.
     *
     * @param apiKey the API key to search for
     * @return optional containing the station API key if found and active
     */
    @Query("SELECT k FROM StationApiKey k JOIN FETCH k.station WHERE k.apiKey = :apiKey AND k.isActive = true")
    Optional<StationApiKey> findByApiKey(String apiKey);
}
package com.aerogrid.backend.repository;

import com.aerogrid.backend.domain.StationApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StationApiKeyRepository extends JpaRepository<StationApiKey, Long> {

    // Aquesta query busca la clau I a més carrega l'Estació en una sola consulta (optimització)
    @Query("SELECT k FROM StationApiKey k JOIN FETCH k.station WHERE k.apiKey = :apiKey AND k.isActive = true")
    Optional<StationApiKey> findByApiKey(String apiKey);
}
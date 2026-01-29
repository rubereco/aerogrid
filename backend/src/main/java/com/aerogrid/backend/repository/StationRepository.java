package com.aerogrid.backend.repository;

import com.aerogrid.backend.domain.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Station entity.
 */
@Repository
public interface StationRepository extends JpaRepository<Station, Long> {

    /**
     * Finds a station by its unique code.
     *
     * @param code the station code
     * @return an Optional containing the station if found
     */
    Optional<Station> findByCode(String code);
}
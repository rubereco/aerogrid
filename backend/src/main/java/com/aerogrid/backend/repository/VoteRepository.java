package com.aerogrid.backend.repository;

import com.aerogrid.backend.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link Vote} entities.
 * Includes methods to find individual user votes and custom queries to aggregate station voting sums.
 */
@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    /**
     * Finds a single vote cast by a specific user for a particular station.
     *
     * @param userId    The ID of the casting user.
     * @param stationId The ID of the station being investigated.
     * @return An Optional containing the exact Vote object if it exists.
     */
    Optional<Vote> findByUserIdAndStationId(Long userId, Long stationId);

    /**
     * Executes a bulk aggregation querying the database to calculate the global trust delta
     * for every voted station simultaneously.
     *
     * @return A list of arrays where index 0 is the station ID (Long), and index 1 is the total numeric sum of votes (Number).
     */
    @Query("SELECT v.station.id, SUM(v.value) FROM Vote v GROUP BY v.station.id")
    List<Object[]> getVoteSumsByStation();

    /**
     * Gets the total sum of numerical vote values for one specific station.
     *
     * @param stationId The ID of the station to calculate the sum for.
     * @return An Optional containing the exact sum numeric delta of the station.
     */
    @Query("SELECT SUM(v.value) FROM Vote v WHERE v.station.id = :stationId")
    Optional<Long> getVoteSumByStationId(Long stationId);

}
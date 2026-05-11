package com.aerogrid.backend.repository;

import com.aerogrid.backend.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
     * Executes a bulk aggregation querying the database to calculate the global trust stats
     * for every voted station simultaneously since a specific date.
     *
     * @param since The minimum timestamp for votes to be included.
     * @return A list of arrays where index 0 is the station ID (Long), index 1 is upvotes (Long), and index 2 is total votes (Long).
     */
    @Query("SELECT v.station.id, SUM(CASE WHEN v.value > 0 THEN 1 ELSE 0 END), COUNT(v) FROM Vote v WHERE v.timestamp >= :since GROUP BY v.station.id")
    List<Object[]> getVoteCountsByStationSince(LocalDateTime since);

    /**
     * Retrieves the exact count of upvotes and downvotes for a specific station since a specific date.
     *
     * @param stationId The ID of the station.
     * @param since The minimum timestamp.
     * @return A list of arrays where index 0 is upvotes (Long) and index 1 is downvotes (Long).
     */
    @Query("SELECT SUM(CASE WHEN v.value > 0 THEN 1 ELSE 0 END), SUM(CASE WHEN v.value < 0 THEN 1 ELSE 0 END) FROM Vote v WHERE v.station.id = :stationId AND v.timestamp >= :since")
    List<Object[]> getVoteCountsForStationSince(Long stationId, LocalDateTime since);

}
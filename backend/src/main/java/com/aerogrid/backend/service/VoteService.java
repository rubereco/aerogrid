package com.aerogrid.backend.service;

import com.aerogrid.backend.controller.dto.VoteRequestDto;
import com.aerogrid.backend.controller.dto.VoteResponseDto;
import com.aerogrid.backend.domain.SourceType;
import com.aerogrid.backend.domain.Station;
import com.aerogrid.backend.domain.User;
import com.aerogrid.backend.domain.Vote;
import com.aerogrid.backend.repository.StationRepository;
import com.aerogrid.backend.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service responsible for processing user votes and dynamically maintaining station trust scores.
 * <p>
 * Implements an in-memory client rate limiter to defend against rapid voting abuse
 * and performs asynchronous cron-based calculation jobs to scale trust score mutations efficiently.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final StationRepository stationRepository;

    private final ConcurrentHashMap<Long, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    /**
     * Internal fixed-window rate limiter ensuring users cannot exceed a precise threshold
     * of interactions (10 petitions per rolling minute).
     */
    private static class RateLimiter {
        AtomicInteger count = new AtomicInteger(0);
        long windowStart = System.currentTimeMillis();

        boolean allow() {
            long now = System.currentTimeMillis();
            if (now - windowStart > 60000) {
                windowStart = now;
                count.set(1);
                return true;
            }
            return count.incrementAndGet() <= 10;
        }
    }

    /**
     * Validates whether a specific user is authorized to perform another action
     * or if their rate limiter has been exhausted.
     *
     * @param userId The ID identifier of the acting user.
     * @throws ResponseStatusException HTTP 429 if the user triggered too many actions within 60 seconds.
     */
    private void checkRateLimit(Long userId) {
        RateLimiter limiter = rateLimiters.computeIfAbsent(userId, k -> new RateLimiter());
        if (!limiter.allow()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Try again in a minute.");
        }
    }

    /**
     * Casts or modifies a positive or negative vote for a chosen station.
     * Records the DB transaction instantly but defers score impacts to the background scheduler.
     *
     * @param stationId The targeted station ID to receive the vote.
     * @param user      The authenticated participant applying their vote.
     * @param dto       Payload carrying the desired VoteType (e.g. POSITIVE).
     * @return Resulting state matching the successful recorded VoteType.
     */
    @Transactional
    public VoteResponseDto castVote(Long stationId, User user, VoteRequestDto dto) {
        checkRateLimit(user.getId());

        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found"));

        Vote vote = voteRepository.findByUserIdAndStationId(user.getId(), stationId)
                .orElseGet(() -> Vote.builder()
                        .user(user)
                        .station(station)
                        .build());

        vote.setType(dto.getType());
        voteRepository.save(vote);

        return VoteResponseDto.builder().type(vote.getType()).build();
    }

    /**
     * Deletes and permanently reverts an existing vote from the logged-in user on a defined station.
     *
     * @param stationId The targeted station ID to retract the vote from.
     * @param user      The authenticated participant withdrawing their feedback.
     */
    @Transactional
    public void removeVote(Long stationId, User user) {
        checkRateLimit(user.getId());

        if (!stationRepository.existsById(stationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found");
        }

        voteRepository.findByUserIdAndStationId(user.getId(), stationId)
                .ifPresent(vote -> {
                    voteRepository.delete(vote);
                    voteRepository.flush();
                });
    }

    /**
     * Inspects the database without limits to return the current vote type that
     * the requesting authenticated user has placed onto the chosen station.
     *
     * @param stationId The targeted station ID in question.
     * @param user      The user asking to perceive their action state.
     * @return Response object containing the specific VoteType, or entirely null internally if unactioned.
     */
    @Transactional(readOnly = true)
    public VoteResponseDto getMyVote(Long stationId, User user) {
        return voteRepository.findByUserIdAndStationId(user.getId(), stationId)
                .map(vote -> VoteResponseDto.builder().type(vote.getType()).build())
                .orElse(VoteResponseDto.builder().type(null).build());
    }

    /**
     * Core asynchronous job acting as the heartbeat for AeroGrid's node trust ecosystem.
     * <p>
     * Triggers continuously every strictly two minutes, bulk queries exactly the delta sums of
     * all votes bound to respective stations, applies mathematical thresholds, and orchestrates
     * massive synchronized global updates to map metric caches.
     * Also garbage collects idle API memory rate limiters.
     * </p>
     */
    @Scheduled(cron = "0 */2 * * * *")
    @Transactional
    public void recalculateAllTrustScores() {
        log.info("Recalculating all station trust scores...");
        List<Station> stations = stationRepository.findAll();

        LocalDateTime since = LocalDateTime.now().minusMonths(6);
        List<Object[]> counts = voteRepository.getVoteCountsByStationSince(since);

        ConcurrentHashMap<Long, long[]> countMap = new ConcurrentHashMap<>();
        for (Object[] row : counts) {
            Long statId = (Long) row[0];
            long upvotes = ((Number) row[1]).longValue();
            long total = ((Number) row[2]).longValue();
            countMap.put(statId, new long[]{upvotes, total});
        }

        for (Station s : stations) {
            if (s.getCode() != null && s.getCode().startsWith("TEST-")) {
                continue; // Preserva els valors originals de les estacions de prova
            }
            long[] stats = countMap.getOrDefault(s.getId(), new long[]{0L, 0L});
            setTrustScoreBasedOnRules(s, stats[0], stats[1]);
        }
        stationRepository.saveAll(stations);
        
        // Evict old rate limiters occasionally (memory management)
        long now = System.currentTimeMillis();
        rateLimiters.entrySet().removeIf(entry -> now - entry.getValue().windowStart > 120000);
    }

    /**
     * Imposes the baseline boundaries dictating how votes transform into visible percentages.
     * Uses a strict Minimum Vote Threshold algorithm.
     *
     * @param s          The station target having calculations applied directly.
     * @param upvotes    The number of upvotes received in the valid time window.
     * @param totalVotes The total number of votes received in the valid time window.
     */
    private void setTrustScoreBasedOnRules(Station s, long upvotes, long totalVotes) {
        int minThreshold = 5;
        int base = s.getSourceType() == SourceType.OFFICIAL ? 50 : 0;
        int potential = s.getSourceType() == SourceType.OFFICIAL ? 50 : 100;

        if (totalVotes < minThreshold) {
            s.setTrustScore(base);
        } else {
            double ratio = (double) upvotes / totalVotes;
            int calculated = (int) Math.round(base + (potential * ratio));
            s.setTrustScore(Math.min(100, Math.max(0, calculated)));
        }
    }
}

package com.aerogrid.backend.controller;

import com.aerogrid.backend.controller.dto.VoteRequestDto;
import com.aerogrid.backend.controller.dto.VoteResponseDto;
import com.aerogrid.backend.domain.User;
import com.aerogrid.backend.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for managing user votes on air quality monitoring stations.
 * <p>
 * Provides endpoints to cast, remove, and retrieve personal votes for stations.
 * All endpoints require an authenticated user.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/stations")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    /**
     * Casts or updates a vote for a specific station.
     *
     * @param stationId  The ID of the station to vote on.
     * @param requestDto The vote payload containing the vote type (POSITIVE/NEGATIVE).
     * @param user       The currently authenticated user casting the vote.
     * @return ResponseEntity containing the recorded vote response.
     */
    @PutMapping("/{stationId}/votes/me")
    public ResponseEntity<VoteResponseDto> castVote(
            @PathVariable Long stationId,
            @RequestBody VoteRequestDto requestDto,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        VoteResponseDto response = voteService.castVote(stationId, user, requestDto);
        return ResponseEntity.ok(response);
    }

    /**
     * Removes the currently authenticated user's vote for a specific station.
     *
     * @param stationId The ID of the station.
     * @param user      The currently authenticated user removing their vote.
     * @return ResponseEntity with no content.
     */
    @DeleteMapping("/{stationId}/votes/me")
    public ResponseEntity<Void> removeVote(
            @PathVariable Long stationId,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        voteService.removeVote(stationId, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves the currently authenticated user's vote for a specific station.
     *
     * @param stationId The ID of the station.
     * @param user      The currently authenticated user.
     * @return ResponseEntity containing the user's vote status.
     */
    @GetMapping("/{stationId}/votes/me")
    public ResponseEntity<VoteResponseDto> getMyVote(
            @PathVariable Long stationId,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        VoteResponseDto response = voteService.getMyVote(stationId, user);
        return ResponseEntity.ok(response);
    }
}

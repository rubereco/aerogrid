package com.aerogrid.backend.controller.dto;

import com.aerogrid.backend.domain.VoteType;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Data Transfer Object representing the outcome or status of a user's vote.
 * Returned to the client after casting, fetching, or checking a vote state.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteResponseDto {
    /** The actual type of vote successfully recorded or fetched (null if strictly un-voted). */
    private VoteType type;
}

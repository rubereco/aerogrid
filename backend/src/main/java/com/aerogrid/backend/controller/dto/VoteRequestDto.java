package com.aerogrid.backend.controller.dto;

import com.aerogrid.backend.domain.VoteType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for casting a vote on a monitoring station.
 * Used to accept incoming petition requests from the frontend client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteRequestDto {
    /** The type of vote being cast (e.g. POSITIVE or NEGATIVE). */
    private VoteType type;
}

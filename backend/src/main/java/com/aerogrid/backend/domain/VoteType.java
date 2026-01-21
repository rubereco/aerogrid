package com.aerogrid.backend.domain;

import lombok.Getter;

/**
 * Enum representing the type of vote that can be cast on a monitoring station.
 * <p>
 * Votes can be either positive (upvote) or negative (downvote) and affect
 * the station's trust score.
 * </p>
 *
 * @author AeroGrid
 * @version 1.0
 * @since 2026-01-21
 */
@Getter
public enum VoteType {
    /** Positive vote (upvote) with value +1 */
    POSITIVE(1),
    /** Negative vote (downvote) with value -1 */
    NEGATIVE(-1);

    /** Numeric value of the vote used for trust score calculations */
    private final int value;

    /**
     * Constructs a VoteType with the specified numeric value.
     *
     * @param value the numeric value of the vote
     */
    VoteType(int value) {
        this.value = value;
    }

    /**
     * Maps a numeric value to its corresponding VoteType.
     *
     * @param value the numeric value to map
     * @return POSITIVE if value is greater than 0, NEGATIVE otherwise
     */
    public static VoteType fromValue(int value) {
        return (value > 0) ? POSITIVE : NEGATIVE;
    }
}
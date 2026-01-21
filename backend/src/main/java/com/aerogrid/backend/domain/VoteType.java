package com.aerogrid.backend.domain;

import lombok.Getter;

@Getter
public enum VoteType {
    POSITIVE(1),
    NEGATIVE(-1);

    private final int value;

    VoteType(int value) {
        this.value = value;
    }

    // Mapper
    public static VoteType fromValue(int value) {
        return (value > 0) ? POSITIVE : NEGATIVE;
    }
}
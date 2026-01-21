package com.aerogrid.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * Entity representing a user's vote on a monitoring station.
 * <p>
 * Users can cast one vote (positive or negative) per station to indicate
 * their trust in the station's data quality. Each user-station pair can
 * have only one vote at a time.
 * </p>
 *
 * @author AeroGrid
 * @version 1.0
 * @since 2026-01-21
 */
@Data                   // generates getters, setters, toString, equals, and hashCode methods
@Builder                // enables the builder pattern for object creation
@NoArgsConstructor      // generates a no-argument constructor
@AllArgsConstructor     // generates an all-arguments constructor
@Entity                 // marks this class as a JPA entity
@Table(name = "votes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "station_id"})
})
public class Vote {

    /** Unique identifier for the vote */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user who cast this vote */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    /** The station being voted on */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Station station;

    /** Numeric value of the vote (+1 for positive, -1 for negative) */
    @Column(name = "vote_value", nullable = false)
    private Integer value;

    /** Timestamp when the vote was cast */
    @CreationTimestamp
    private LocalDateTime timestamp;

    /**
     * Sets the vote type (positive or negative).
     *
     * @param type the vote type to set
     */
    public void setType(VoteType type) {
        this.value = type.getValue();
    }

    /**
     * Gets the vote type based on the numeric value.
     *
     * @return the vote type (POSITIVE or NEGATIVE)
     */
    public VoteType getType() {
        return VoteType.fromValue(this.value);
    }
}
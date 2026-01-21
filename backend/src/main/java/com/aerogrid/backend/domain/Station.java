package com.aerogrid.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

/**
 * Entity representing an air quality monitoring station.
 * <p>
 * Stations can be either official government-operated stations or citizen-operated
 * stations. Each station has a geographic location and a trust score based on user votes.
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
@Table(name = "stations")
public class Station {

    /** Unique identifier for the station */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Display name of the station */
    @Column(nullable = false)
    private String name;

    /** Geographic location of the station using WGS84 coordinate system (SRID 4326) */
    @Column(columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point location;

    /** Type of station source (official or citizen-operated) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceType sourceType;

    /** Community trust score based on user votes */
    @Column(nullable = false)
    private int trustScore;

    /** Whether the station is currently active and collecting data */
    @Column(nullable = false)
    private Boolean isActive;

    /** The user who owns this station (null for official stations) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User owner;

    /** Timestamp when the station was created */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /** Timestamp when the station was last updated */
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

package com.aerogrid.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * Entity class representing an air quality measurement from a monitoring station.
 * <p>
 * This class stores individual pollutant measurements collected at specific timestamps
 * from various monitoring stations. Each measurement includes the pollutant type,
 * its measured value, and an optional Air Quality Index (AQI) calculation.
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
                        // maps this entity to the "measurements" table
@Table(name = "measurements", indexes = {
        @Index(name = "idx_measurement_station_time", columnList = "station_id, timestamp")
})
public class Measurement {

    /**
     * The unique identifier for this measurement. Auto-generated.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The monitoring station where this measurement was recorded. Station cascade.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Station station;

    /**
     * The date and time when this measurement was recorded.
     * <p>
     * This field is required and is indexed along with station_id for efficient
     * time-series queries.
     * </p>
     */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    /**
     * The type of pollutant being measured (e.g., PM2.5, PM10, NO2, O3, CO, SO2).
     * <p>
     * Stored as a string in the database to maintain readability and flexibility.
     * This field is required for every measurement.
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Pollutant pollutant;

    /**
     * The measured concentration value of the pollutant.
     * <p>
     * The unit of measurement depends on the pollutant type (typically μg/m³ or ppm).
     * This field is required and must contain a valid numeric value.
     * </p>
     */
    @Column(nullable = false)
    private Double value;

    /** Air Quality Index calculated for this measurement (optional) */
    private Integer aqi;
}
package com.aerogrid.backend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hourly_aqi_snapshots", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"station_id", "timestamp"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HourlyAqiSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "max_aqi", nullable = true)
    private Integer maxAqi;

    @Column(nullable = true)
    private String pollutant;
}


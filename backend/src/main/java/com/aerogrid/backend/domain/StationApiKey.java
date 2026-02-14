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
import java.util.UUID;

/**
 * Entity representing an API Key associated with a Station.
 * Used for authenticating data ingestion from physical stations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "station_api_keys")
public class StationApiKey {

    /**
     * Unique identifier for the API Key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The actual API key string. Must be unique.
     */
    @Column(nullable = false, unique = true)
    private String apiKey;

    /**
     * The station associated with this API Key.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Station station;

    /**
     * Whether the API Key is currently active.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    /**
     * Timestamp when the API Key was created.
     */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * Generates a secure random API key string.
     * format: sk_ + random UUID (without dashes) + random suffix
     *
     * @return A randomly generated API key string.
     */
    public static String generateRandomKey() {
        return "sk_" + UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().substring(0, 10);
    }
}
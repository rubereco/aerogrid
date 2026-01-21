package com.aerogrid.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a user in the AeroGrid system.
 * <p>
 * Users can register, authenticate, vote on stations, and optionally own
 * citizen-operated monitoring stations.
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
@Table(name = "users")  // maps this entity to the "users" table
public class User {

    /** Unique identifier for the user */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique username for login */
    @Column(nullable = false, unique = true)
    private String username;

    /** Unique email address for the user */
    @Column(nullable = false, unique = true)
    private String email;

    /** Hashed password for authentication */
    @Column(nullable = false)
    private String password;

    /** User's role (USER or ADMIN) */
    @Enumerated(EnumType.STRING)
    private Role role;

    /** Timestamp when the user account was created */
    @CreationTimestamp  // automatically sets the creation timestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp when the user account was last updated */
    @UpdateTimestamp    // automatically updates the timestamp on modification
    private LocalDateTime updatedAt;
}
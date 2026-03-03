package com.aerogrid.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

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
public class User implements UserDetails {

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
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp when the user account was last updated */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
package com.aerogrid.backend.repository;

import com.aerogrid.backend.domain.HourlyAqiSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HourlyAqiSnapshotRepository extends JpaRepository<HourlyAqiSnapshot, Long> {
}


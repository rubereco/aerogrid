package com.aerogrid.backend.repository.projection;

import java.time.LocalDateTime;

public interface AggregatedMeasurementProjection {
    LocalDateTime getTimestamp();
    String getPollutant();
    Double getAvgValue();
    Double getAvgAqi();
}


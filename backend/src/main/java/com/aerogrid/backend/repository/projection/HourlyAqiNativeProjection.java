package com.aerogrid.backend.repository.projection;

import java.time.LocalDateTime;

public interface HourlyAqiNativeProjection {
    Long getStationId();
    Integer getMaxAqi();
    String getPollutant();
    LocalDateTime getTimestamp();
}

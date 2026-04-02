package com.aerogrid.backend.repository.projection;

public interface HourlyAqiNativeProjection {
    Long getStationId();
    Integer getMaxAqi();
    String getPollutant();
}

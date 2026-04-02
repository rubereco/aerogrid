package com.aerogrid.backend.repository.projection;

public interface StationMapProjection {
    Long getId();
    String getCode();
    String getName();
    Double getLatitude();
    Double getLongitude();
    Integer getAqi();
    String getPollutant();
}


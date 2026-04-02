package com.aerogrid.backend.repository.projection;

import com.aerogrid.backend.domain.Station;

public interface StationAqiProjection {
    Station getStation();
    Integer getMaxAqi();
    String getPollutant();
}


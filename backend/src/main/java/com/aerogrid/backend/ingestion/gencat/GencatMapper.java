package com.aerogrid.backend.ingestion.gencat;

import com.aerogrid.backend.domain.SourceType;
import com.aerogrid.backend.ingestion.common.CommonMeasurementDto;
import com.aerogrid.backend.ingestion.common.CommonStationDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class GencatMapper {

    public CommonStationDto toStationDto(GencatRawDto raw) {
        return CommonStationDto.builder()
                .code(raw.getStationCode())
                .name(raw.getStationName())
                .municipality(raw.getMunicipality())
                .latitude(parseDouble(raw.getLatitude()))
                .longitude(parseDouble(raw.getLongitude()))
                .type(SourceType.OFFICIAL.toString())
                .build();
    }

    /**
     * Converts a raw Gencat DTO containing hourly values to a list of CommonMeasurementDto.
     * @param raw the raw data containing 24h measurements
     * @return list of individual measurement DTOs
     */
    public List<CommonMeasurementDto> toMeasurementDtos(GencatRawDto raw) {
        List<CommonMeasurementDto> measurements = new ArrayList<>();

        LocalDateTime baseDate = LocalDateTime.parse(raw.getDate(), DateTimeFormatter.ISO_DATE_TIME);

        String stationCode = raw.getStationCode();
        String pollutant = raw.getPollutant();

        raw.getHourlyValues().forEach((key, valueStr) -> {
            try {
                int hourIndex = Integer.parseInt(key.substring(1));

                Double value = Double.valueOf(valueStr);

                LocalDateTime realTimestamp = baseDate.plusHours(hourIndex - 1);

                measurements.add(CommonMeasurementDto.builder()
                        .stationCode(stationCode)
                        .pollutant(pollutant)
                        .value(value)
                        .timestamp(realTimestamp)
                        .build());

            } catch (NumberFormatException e) {
                System.err.println("Error parsing value for hour " + key + ": " + valueStr + " - " + e.getMessage());
            }
        });

        return measurements;
    }

    private Double parseDouble(String value) {
        return (value != null) ? Double.valueOf(value) : null;
    }
}
package com.aerogrid.backend.service;

import com.aerogrid.backend.controller.dto.StationCreationResponseDto;
import com.aerogrid.backend.controller.dto.StationDetailsDto;
import com.aerogrid.backend.controller.mapper.StationMapper;
import com.aerogrid.backend.domain.Station;
import com.aerogrid.backend.domain.User;
import com.aerogrid.backend.domain.SourceType;
import com.aerogrid.backend.domain.StationApiKey;
import com.aerogrid.backend.repository.StationRepository;
import com.aerogrid.backend.repository.StationApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StationService {

    private final StationRepository stationRepository;
    private final StationApiKeyRepository stationApiKeyRepository;
    private final StationMapper stationMapper;

    @Transactional
    public StationCreationResponseDto createStation(StationDetailsDto stationDetailsDto, User user) {
        Station station = stationMapper.toEntityFromDetails(stationDetailsDto);
        station.setOwner(user);
        station.setSourceType(SourceType.CITIZEN);

        Station savedStation = stationRepository.save(station);

        String apiKeyString = StationApiKey.generateRandomKey();
        StationApiKey apiKey = StationApiKey.builder()
                .apiKey(apiKeyString)
                .station(savedStation)
                .isActive(true)
                .build();

        stationApiKeyRepository.save(apiKey);

        return StationCreationResponseDto.builder()
                .station(stationMapper.toDetailsDto(savedStation))
                .apiKey(apiKeyString)
                .build();
    }

    @Transactional
    public StationDetailsDto updateStation(Long id, StationDetailsDto dto, User user) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Station not found"));

        if (station.getOwner() == null || !station.getOwner().getId().equals(user.getId())) {
            throw new SecurityException("Not authorized to update this station");
        }

        if (dto.getName() != null) station.setName(dto.getName());
        if (dto.getMunicipality() != null) station.setMunicipality(dto.getMunicipality());
        if (dto.getIsActive() != null) station.setIsActive(dto.getIsActive());

        return stationMapper.toDetailsDto(stationRepository.save(station));
    }

    @Transactional
    public void deleteStation(Long id, User user) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Station not found"));

        if (station.getOwner() == null || !station.getOwner().getId().equals(user.getId())) {
            throw new SecurityException("Not authorized to delete this station");
        }

        // Hard delete, assume cascading is handled or no strict measurements link
        stationRepository.delete(station);
    }
}

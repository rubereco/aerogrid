import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Map, Source, Layer, Popup } from 'react-map-gl/maplibre';
import maplibregl from 'maplibre-gl';
import * as pmtiles from 'pmtiles';
import 'maplibre-gl/dist/maplibre-gl.css';
import api from '../../api/axios';
import StationDetailsPanel from './StationDetailsPanel';
import DateTimeFilter from './DateTimeFilter';
import StationPopup from './StationPopup';

const protocol = new pmtiles.Protocol();
maplibregl.addProtocol('pmtiles', protocol.tile);

/**
 * Main map component displaying air quality stations.
 * Uses WebGL and vector layers (GeoJSON) for high performance with thousands of points.
 *
 * @returns {JSX.Element} The rendered map component.
 */
export default function MapComponent() {
    const [stationsGeoJSON, setStationsGeoJSON] = useState(null);
    const [selectedStation, setSelectedStation] = useState(null);
    const [detailsStationCode, setDetailsStationCode] = useState(null);
    const [targetTime, setTargetTime] = useState(null);
    const [cursor, setCursor] = useState('auto');
    const mapRef = useRef(null);

    const fetchStationsInBounds = useCallback(async (bounds, timeOverride) => {
        try {
            const sw = bounds.getSouthWest();
            const ne = bounds.getNorthEast();

            const params = {
                minLon: sw.lng,
                minLat: sw.lat,
                maxLon: ne.lng,
                maxLat: ne.lat
            };

            const timeToUse = timeOverride !== undefined ? timeOverride : targetTime;
            if (timeToUse) {
                params.targetTime = timeToUse;
            }

            const response = await api.get('/api/v1/stations', { params });
            const stations = response.data;

            const geojsonData = {
                type: 'FeatureCollection',
                features: stations.map(station => ({
                    type: 'Feature',
                    geometry: {
                        type: 'Point',
                        coordinates: [station.longitude, station.latitude]
                    },
                    properties: {
                        id: station.id,
                        code: station.code,
                        name: station.name,
                        aqi: station.aqi || 0,
                        pollutant: station.pollutant,
                        trustScore: station.trustScore !== undefined ? station.trustScore : 100
                    }
                }))
            };

            setStationsGeoJSON(geojsonData);
        } catch (error) {
            console.error("Error fetching stations:", error);
        }
    }, [targetTime]);

    const onMoveEnd = useCallback((e) => {
        const map = e.target;
        const bounds = map.getBounds();
        fetchStationsInBounds(bounds);
    }, [fetchStationsInBounds]);

    useEffect(() => {
        // Initial fetch handled after map load or you can trigger a global find if you don't pass bounds.
        // For default we just do a general fetch until map loads:
        const fetchStations = async () => {
            try {
                const params = {};
                if (targetTime) params.targetTime = targetTime;

                const response = await api.get('/api/v1/stations', { params });
                const stations = response.data;
                const geojsonData = {
                    type: 'FeatureCollection',
                    features: stations.map(station => ({
                        type: 'Feature',
                        geometry: { type: 'Point', coordinates: [station.longitude, station.latitude] },
                        properties: { id: station.id, code: station.code, name: station.name, aqi: station.aqi || 0, pollutant: station.pollutant, trustScore: station.trustScore !== undefined ? station.trustScore : 100 }
                    }))
                };
                setStationsGeoJSON(geojsonData);
            } catch (error) { console.error(error); }
        };
        fetchStations();
    }, [targetTime]); // Added targetTime dependency to reload global data when time changes

    /**
     * Style layer for the points (circles).
     * The circle color is assigned dynamically based on the AQI value using Mapbox GL expressions.
     */
    const layerStyle = useMemo(() => ({
        id: 'stations-layer',
        type: 'circle',
        source: 'stations',
        filter: ['!', ['has', 'point_count']],
        paint: {
            'circle-radius': [
                'step',
                ['to-number', ['get', 'trustScore']],
                5,
                15, 7.5,
                30, 10
            ],
            'circle-opacity': [
                'step',
                ['to-number', ['get', 'trustScore']],
                0.6,
                15, 0.8,
                30, 1.0
            ],
            'circle-color': [
                'step',
                ['to-number', ['get', 'aqi']],
                '#9ca3af', // Default (0 o sense dades) -> Gris
                1, '#22c55e', // 1: Good -> Verd
                2, '#eab308', // 2: Fair -> Groc
                3, '#f97316', // 3: Moderate -> Taronja
                4, '#ef4444', // 4: Poor -> Vermell
                5, '#a855f7', // 5: Very Poor -> Lila
                6, '#881337'  // 6: Extremely Poor -> Granat fosc
            ],
            'circle-stroke-width': 2,
            'circle-stroke-color': '#ffffff'
        }
    }), []);

    const clusterLayerStyle = useMemo(() => ({
        id: 'clusters-layer',
        type: 'circle',
        source: 'stations',
        filter: ['has', 'point_count'],
        paint: {
            'circle-radius': ['step', ['get', 'point_count'], 16, 10, 20, 50, 24],
            'circle-color': [
                'step',
                ['%', ['to-number', ['get', 'max_trust_score_aqi']], 10],
                '#9ca3af',
                1, '#22c55e',
                2, '#eab308',
                3, '#f97316',
                4, '#ef4444',
                5, '#a855f7',
                6, '#881337'
            ],
            'circle-stroke-width': 2,
            'circle-stroke-color': '#ffffff',
            'circle-opacity-transition': { duration: 0 },
            'circle-radius-transition': { duration: 0 },
            'circle-color-transition': { duration: 0 }
        }
    }), []);

    const clusterCountLayerStyle = useMemo(() => ({
        id: 'clusters-count-layer',
        type: 'symbol',
        source: 'stations',
        filter: ['has', 'point_count'],
        layout: {
            'text-field': '{point_count_abbreviated}',
            'text-size': 12,
            'text-allow-overlap': true,
            'text-ignore-placement': true
        },
        paint: {
            'text-color': '#ffffff',
            'text-opacity-transition': { duration: 0 }
        }
    }), []);

    const clusterProperties = useMemo(() => ({
        max_trust_score_aqi: ['max', ['+', ['*', ['to-number', ['get', 'trustScore']], 10], ['to-number', ['get', 'aqi']]]]
    }), []);

    const onMouseEnter = useCallback(() => setCursor('pointer'), []);
    const onMouseLeave = useCallback(() => setCursor('auto'), []);

    /**
     * Handles clicks on the map.
     * Opens a popup if a station is clicked.
     *
     * @param {Object} event - The map click event object.
     */
    const onClick = useCallback((event) => {
        const feature = event.features && event.features[0];
        if (!feature) {
            setSelectedStation(null);
            return;
        }

        // Properly identify if the clicked feature is a cluster
        const isCluster = feature.properties.cluster ||
                          (feature.layer && (feature.layer.id === 'clusters-layer' || feature.layer.id === 'clusters-count-layer'));

        if (isCluster) {
            if (!stationsGeoJSON || !stationsGeoJSON.features) return;

            const pointCount = Number(feature.properties.point_count) || 2;
            const clusterLng = feature.geometry.coordinates ? feature.geometry.coordinates[0] : event.lngLat.lng;
            const clusterLat = feature.geometry.coordinates ? feature.geometry.coordinates[1] : event.lngLat.lat;

            // 1. Calculate squared distance from all stations to the cluster center
            const stationsWithDistance = stationsGeoJSON.features.map(f => {
                const lng = f.geometry.coordinates[0];
                const lat = f.geometry.coordinates[1];
                const distSq = Math.pow(lng - clusterLng, 2) + Math.pow(lat - clusterLat, 2);
                return { ...f, distSq };
            });

            // 2. Sort by distance to find the practically closest `pointCount` stations
            stationsWithDistance.sort((a, b) => a.distSq - b.distSq);

            // 3. Take the first `pointCount` stations (these are the ones making up the cluster)
            const clusterStations = stationsWithDistance.slice(0, pointCount);

            // 4. Find the maximum AQI among these assumed clustered stations
            let maxAqi = -1;
            clusterStations.forEach(st => {
                const aqi = Number(st.properties.aqi) || 0;
                if (aqi > maxAqi) maxAqi = aqi;
            });

            // 5. Filter down to only those with the exact max AQI
            const worstStations = clusterStations.filter(st => (Number(st.properties.aqi) || 0) === maxAqi);

            // 6. Because we already sorted by distance in step 2, worstStations[0] is automatically the one closest to the center!
            const selectedFeature = worstStations[0] || clusterStations[0];

            if (selectedFeature) {
                setSelectedStation({
                    longitude: selectedFeature.geometry.coordinates[0],
                    latitude: selectedFeature.geometry.coordinates[1],
                    properties: selectedFeature.properties
                });
            }
            return;
        }

        // For individual unclustered stations
        setSelectedStation({
            longitude: event.lngLat.lng,
            latitude: event.lngLat.lat,
            properties: feature.properties
        });
    }, [stationsGeoJSON]); // Added stationsGeoJSON to dependencies so it has fresh state

    // Escoltar event del buscador (FloatingHeader.jsx)
    useEffect(() => {
        const handleFlyToStation = (e) => {
            const { station } = e.detail;

            // 1. Moure la càmera
            if (mapRef.current) {
                mapRef.current.flyTo({
                    center: [station.longitude, station.latitude],
                    zoom: 14,
                    duration: 1500
                });
            }

            // 2. Seleccionar l'estació per mostrar el Popup en comptes dels detalls complerts
            setTimeout(() => {
                setSelectedStation({
                    longitude: station.longitude,
                    latitude: station.latitude,
                    properties: station
                });
            }, 500);
        };

        const handleClearSelection = () => {
            setSelectedStation(null);
            setDetailsStationCode(null);
        };

        window.addEventListener('flyToStation', handleFlyToStation);
        window.addEventListener('clearMapSelection', handleClearSelection);

        return () => {
            window.removeEventListener('flyToStation', handleFlyToStation);
            window.removeEventListener('clearMapSelection', handleClearSelection);
        };
    }, []);

    // Configuració del nou mapStyle utilitzant PMTiles completament local i un estil visual professional
    const pmtilesUrl = `${window.location.origin}/spain.pmtiles`;

    const mapStyle = useMemo(() => {
        // Obtenim l'estil bàsic tipus "positron" que OpenMapTiles requereix
        // i canviem dinàmicament la font d'internet per al teu PMTiles local!
        return {
            version: 8,
            name: "Local Light Map",
            sprite: "https://openmaptiles.github.io/positron-gl-style/sprite",
            glyphs: "https://fonts.openmaptiles.org/{fontstack}/{range}.pbf",
            sources: {
                openmaptiles: {
                    type: "vector",
                    url: `pmtiles://${pmtilesUrl}`,
                    maxzoom: 14,
                    attribution: '<a href="https://openstreetmap.org">OpenStreetMap</a>'
                }
            },
            transition: {
                duration: 0,
                delay: 0
            },
            layers: [
                {
                    "id": "background",
                    "type": "background",
                    "paint": { "background-color": "#f8f9fa" }
                },
                {
                    "id": "landuse_residential",
                    "type": "fill",
                    "source": "openmaptiles",
                    "source-layer": "landuse",
                    "minzoom": 10,
                    "filter": ["==", "class", "residential"],
                    "paint": { "fill-color": "#f1f5f9" }
                },
                {
                    "id": "landcover_wood",
                    "type": "fill",
                    "source": "openmaptiles",
                    "source-layer": "landcover",
                    "minzoom": 6,
                    "filter": ["==", "class", "wood"],
                    "paint": { "fill-color": "#dcfce7", "fill-opacity": 0.7 }
                },
                {
                    "id": "landcover_grass",
                    "type": "fill",
                    "source": "openmaptiles",
                    "source-layer": "landcover",
                    "minzoom": 8,
                    "filter": ["==", "class", "grass"],
                    "paint": { "fill-color": "#e2e8f0", "fill-opacity": 0.5 }
                },
                {
                    "id": "park",
                    "type": "fill",
                    "source": "openmaptiles",
                    "source-layer": "park",
                    "minzoom": 10,
                    "paint": { "fill-color": "#dcfce7" }
                },
                {
                    "id": "water",
                    "type": "fill",
                    "source": "openmaptiles",
                    "source-layer": "water",
                    "minzoom": 0,
                    "paint": { "fill-color": "#bae6fd" }
                },
                {
                    "id": "waterway",
                    "type": "line",
                    "source": "openmaptiles",
                    "source-layer": "waterway",
                    "minzoom": 8,
                    "paint": { "line-color": "#bae6fd", "line-width": 1.5 }
                },
                {
                    "id": "transportation_minor",
                    "type": "line",
                    "source": "openmaptiles",
                    "source-layer": "transportation",
                    "minzoom": 13,
                    "filter": ["in", "class", "minor", "tertiary", "residential"],
                    "paint": { "line-color": "#ffffff", "line-width": 1.5 }
                },
                {
                    "id": "transportation_secondary",
                    "type": "line",
                    "source": "openmaptiles",
                    "source-layer": "transportation",
                    "minzoom": 9,
                    "filter": ["in", "class", "secondary", "primary_link", "secondary_link"],
                    "paint": { "line-color": "#ffffff", "line-width": 2.5 }
                },
                {
                    "id": "transportation_primary",
                    "type": "line",
                    "source": "openmaptiles",
                    "source-layer": "transportation",
                    "minzoom": 5,
                    "filter": ["in", "class", "primary", "trunk", "trunk_link"],
                    "paint": { "line-color": "#ffffff", "line-width": 3.5 }
                },
                {
                    "id": "transportation_motorway",
                    "type": "line",
                    "source": "openmaptiles",
                    "source-layer": "transportation",
                    "minzoom": 4,
                    "filter": ["==", "class", "motorway"],
                    "paint": { "line-color": "#cbd5e1", "line-width": ["interpolate", ["linear"], ["zoom"], 5, 1, 12, 4] }
                },
                {
                    "id": "building",
                    "type": "fill",
                    "source": "openmaptiles",
                    "source-layer": "building",
                    "minzoom": 14,
                    "paint": { "fill-color": "#e2e8f0", "fill-opacity": 0.6 }
                },
                {
                    "id": "boundary_country",
                    "type": "line",
                    "source": "openmaptiles",
                    "source-layer": "boundary",
                    "minzoom": 2,
                    "filter": ["==", "admin_level", 2],
                    "paint": { "line-color": "#94a3b8", "line-width": 1.5, "line-dasharray": [3, 3] }
                },
                {
                    "id": "place_label_towns",
                    "type": "symbol",
                    "source": "openmaptiles",
                    "source-layer": "place",
                    "minzoom": 9,
                    "filter": ["in", "class", "town", "village"],
                    "layout": {
                        "text-field": "{name}",
                        "text-font": ["Open Sans Regular", "Arial Unicode MS Regular"],
                        "text-size": 12
                    },
                    "paint": {
                        "text-color": "#64748b",
                        "text-halo-color": "#ffffff",
                        "text-halo-width": 1.5
                    }
                },
                {
                    "id": "place_label_city",
                    "type": "symbol",
                    "source": "openmaptiles",
                    "source-layer": "place",
                    "minzoom": 4,
                    "maxzoom": 12,
                    "filter": ["==", "class", "city"],
                    "layout": {
                        "text-field": "{name}",
                        "text-font": ["Open Sans Bold", "Arial Unicode MS Bold"],
                        "text-size": 16
                    },
                    "paint": {
                        "text-color": "#334155",
                        "text-halo-color": "#ffffff",
                        "text-halo-width": 2
                    }
                }
            ]
        };
    }, [pmtilesUrl]);

    // Per quan vulguis tornar a la versió online:
    // const mapStyle = import.meta.env.VITE_MAPTILER_KEY
    //     ? `https://api.maptiler.com/maps/streets-v2/style.json?key=${import.meta.env.VITE_MAPTILER_KEY}`
    //     : "https://basemaps.cartocdn.com/gl/positron-gl-style/style.json";

    return (
        <div className="w-full h-full relative">
            <Map
                ref={mapRef}
                initialViewState={{
                    longitude: 2.1734,
                    latitude: 41.3851,
                    zoom: 7
                }}
                mapLib={maplibregl}
                mapStyle={mapStyle}
                fadeDuration={0}
                interactiveLayerIds={['stations-layer', 'clusters-layer', 'clusters-count-layer']}
                onMouseEnter={onMouseEnter}
                onMouseLeave={onMouseLeave}
                onClick={onClick}
                onMoveEnd={onMoveEnd}
                cursor={cursor}
                style={{ width: '100%', height: '100%' }}
            >
                {/* Floating DateTime Selector */}
                <div className="absolute top-0 left-0 h-full w-full pointer-events-none" style={{zIndex: 40}}>
                    <DateTimeFilter 
                        onDateTimeChange={(newTime) => {
                            setTargetTime(newTime);
                            if (mapRef.current) {
                                fetchStationsInBounds(mapRef.current.getBounds(), newTime);
                            }
                        }} 
                    />
                </div>

                {stationsGeoJSON && (
                    <Source 
                        id="stations" 
                        type="geojson"
                        data={stationsGeoJSON}
                        cluster={true}
                        clusterMaxZoom={14}
                        clusterRadius={50}
                        clusterProperties={clusterProperties}
                    >
                        <Layer {...clusterLayerStyle} />
                        <Layer {...clusterCountLayerStyle} />
                        <Layer {...layerStyle} />
                    </Source>
                )}

                {selectedStation && (
                    <StationPopup
                        station={selectedStation}
                        targetTime={targetTime}
                        onClose={() => setSelectedStation(null)}
                        onViewDetails={(code) => setDetailsStationCode(code)}
                    />
                )}
            </Map>

            {detailsStationCode && (
                <StationDetailsPanel
                    stationCode={detailsStationCode}
                    onClose={() => setDetailsStationCode(null)}
                />
            )}
        </div>
    );
}

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Map, Source, Layer, Popup } from 'react-map-gl/maplibre';
import maplibregl from 'maplibre-gl';
import * as pmtiles from 'pmtiles';
import 'maplibre-gl/dist/maplibre-gl.css';
import api from '../../api/axios';

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
    const [cursor, setCursor] = useState('auto');

    /**
     * Fetches stations from the API and transforms them into a GeoJSON FeatureCollection.
     */
    useEffect(() => {
        const fetchStations = async () => {
            try {
                const response = await api.get('/api/v1/stations');
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
                            pollutant: station.pollutant
                        }
                    }))
                };
                
                setStationsGeoJSON(geojsonData);
            } catch (error) {
                console.error("Error fetching stations:", error);
            }
        };

        fetchStations();
    }, []);

    /**
     * Style layer for the points (circles).
     * The circle color is assigned dynamically based on the AQI value using Mapbox GL expressions.
     */
    const layerStyle = useMemo(() => ({
        id: 'stations-layer',
        type: 'circle',
        source: 'stations',
        paint: {
            'circle-radius': 8,
            'circle-color': [
                'step',
                ['get', 'aqi'],
                '#22c55e', 
                50,
                '#eab308', 
                100,
                '#ef4444'  
            ],
            'circle-stroke-width': 2,
            'circle-stroke-color': '#ffffff'
        }
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
        if (feature) {
            setSelectedStation({
                longitude: event.lngLat.lng,
                latitude: event.lngLat.lat,
                properties: feature.properties
            });
        }
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
                    attribution: '<a href="https://openstreetmap.org">OpenStreetMap</a>'
                }
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
                    "filter": ["==", "class", "residential"],
                    "paint": { "fill-color": "#f1f5f9" }
                },
                {
                    "id": "landcover_wood",
                    "type": "fill",
                    "source": "openmaptiles",
                    "source-layer": "landcover",
                    "filter": ["==", "class", "wood"],
                    "paint": { "fill-color": "#dcfce7", "fill-opacity": 0.7 }
                },
                {
                    "id": "landcover_grass",
                    "type": "fill",
                    "source": "openmaptiles",
                    "source-layer": "landcover",
                    "filter": ["==", "class", "grass"],
                    "paint": { "fill-color": "#e2e8f0", "fill-opacity": 0.5 }
                },
                {
                    "id": "park",
                    "type": "fill",
                    "source": "openmaptiles",
                    "source-layer": "park",
                    "paint": { "fill-color": "#dcfce7" }
                },
                {
                    "id": "water",
                    "type": "fill",
                    "source": "openmaptiles",
                    "source-layer": "water",
                    "paint": { "fill-color": "#bae6fd" }
                },
                {
                    "id": "waterway",
                    "type": "line",
                    "source": "openmaptiles",
                    "source-layer": "waterway",
                    "paint": { "line-color": "#bae6fd", "line-width": 1.5 }
                },
                {
                    "id": "transportation_minor",
                    "type": "line",
                    "source": "openmaptiles",
                    "source-layer": "transportation",
                    "filter": ["in", "class", "minor", "tertiary", "residential"],
                    "paint": { "line-color": "#ffffff", "line-width": 1.5 }
                },
                {
                    "id": "transportation_secondary",
                    "type": "line",
                    "source": "openmaptiles",
                    "source-layer": "transportation",
                    "filter": ["in", "class", "secondary", "primary_link", "secondary_link"],
                    "paint": { "line-color": "#ffffff", "line-width": 2.5 }
                },
                {
                    "id": "transportation_primary",
                    "type": "line",
                    "source": "openmaptiles",
                    "source-layer": "transportation",
                    "filter": ["in", "class", "primary", "trunk", "trunk_link"],
                    "paint": { "line-color": "#ffffff", "line-width": 3.5 }
                },
                {
                    "id": "transportation_motorway",
                    "type": "line",
                    "source": "openmaptiles",
                    "source-layer": "transportation",
                    "filter": ["==", "class", "motorway"],
                    // Utilitza interpolació per fer les autopistes més gruixudes quan fas zoom a prop
                    "paint": { "line-color": "#cbd5e1", "line-width": ["interpolate", ["linear"], ["zoom"], 5, 1, 12, 4] }
                },
                {
                    "id": "building",
                    "type": "fill",
                    "source": "openmaptiles",
                    "source-layer": "building",
                    "paint": { "fill-color": "#e2e8f0", "fill-opacity": 0.6 }
                },
                {
                    "id": "boundary_country",
                    "type": "line",
                    "source": "openmaptiles",
                    "source-layer": "boundary",
                    "filter": ["==", "admin_level", 2],
                    "paint": { "line-color": "#94a3b8", "line-width": 1.5, "line-dasharray": [3, 3] }
                },
                {
                    "id": "place_label_towns",
                    "type": "symbol",
                    "source": "openmaptiles",
                    "source-layer": "place",
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
        <div className="w-full h-full">
            <Map
                initialViewState={{
                    longitude: 2.1734,
                    latitude: 41.3851,
                    zoom: 7
                }}
                mapLib={maplibregl}
                mapStyle={mapStyle}
                interactiveLayerIds={['stations-layer']}
                onMouseEnter={onMouseEnter}
                onMouseLeave={onMouseLeave}
                onClick={onClick}
                cursor={cursor}
                style={{ width: '100%', height: '100%' }}
            >
                {stationsGeoJSON && (
                    <Source id="stations" type="geojson" data={stationsGeoJSON}>
                        <Layer {...layerStyle} />
                    </Source>
                )}

                {selectedStation && (
                    <Popup
                        longitude={selectedStation.longitude}
                        latitude={selectedStation.latitude}
                        anchor="bottom"
                        onClose={() => setSelectedStation(null)}
                        closeOnClick={false}
                        className="rounded-lg shadow-lg"
                    >
                        <div className="p-2 text-sm text-gray-800">
                            <h3 className="font-bold text-lg mb-1">{selectedStation.properties.name}</h3>
                            <p className="mb-1"><span className="font-semibold">Code:</span> {selectedStation.properties.code}</p>
                            <p className="mb-3"><span className="font-semibold">AQI:</span> {selectedStation.properties.aqi}</p>
                            <button 
                                disabled 
                                className="w-full px-4 py-2 bg-blue-500 text-white rounded cursor-not-allowed opacity-50 font-medium transition-colors"
                            >
                                View details
                            </button>
                        </div>
                    </Popup>
                )}
            </Map>
        </div>
    );
}

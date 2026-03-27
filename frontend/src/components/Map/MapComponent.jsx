import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Map, Source, Layer, Popup } from 'react-map-gl/maplibre';
import maplibregl from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import api from '../../api/axios';

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

    const mapStyle = import.meta.env.VITE_MAPTILER_KEY 
        ? `https://api.maptiler.com/maps/streets-v2/style.json?key=${import.meta.env.VITE_MAPTILER_KEY}`
        : "https://basemaps.cartocdn.com/gl/positron-gl-style/style.json";

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

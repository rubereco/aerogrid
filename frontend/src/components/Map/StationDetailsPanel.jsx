import React, { useEffect, useState } from 'react';
import { X, Activity } from 'lucide-react';
import api from '../../api/axios';
import StationChart from './StationChart';

export default function StationDetailsPanel({ stationCode, onClose }) {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [stationInfo, setStationInfo] = useState(null);

    useEffect(() => {
        if (!stationCode) return;

        const fetchData = async () => {
            setLoading(true);
            setError(null);
            try {
                const res = await api.get(`/api/v1/stations/${stationCode}`);
                setStationInfo(res.data);
            } catch (err) {
                console.error("Error fetching station details:", err);
                setError('No s\'ha pogut carregar la informació de l\'estació.');
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, [stationCode]);

    return (
        <div className="absolute inset-0 bg-white/95 backdrop-blur-md shadow-2xl p-6 flex flex-col z-50 transform transition-transform duration-300">
            <button
                onClick={onClose}
                className="absolute top-4 right-4 p-2 text-gray-500 hover:bg-gray-100 rounded-full transition-colors"
                aria-label="Tanca"
            >
                <X className="w-5 h-5" />
            </button>

            {loading ? (
                <div className="flex-1 flex items-center justify-center">
                    <Activity className="w-8 h-8 text-blue-500 animate-pulse" />
                </div>
            ) : error ? (
                <div className="flex-1 text-red-500 flex items-center justify-center text-center p-4">
                    {error}
                </div>
            ) : (
                <div className="flex-col h-full flex pt-8">
                    <h2 className="text-2xl font-bold text-gray-800 mb-1">{stationInfo?.name}</h2>
                    <p className="text-sm text-gray-500 mb-6 font-mono">{stationInfo?.code}</p>

                    <div className="bg-blue-50 rounded-xl p-4 mb-6">
                        <div className="text-sm text-blue-600 font-semibold mb-1">Estat Actual</div>
                        <div className="flex items-end gap-2">
                            <span className="text-3xl font-bold text-blue-900">
                                {stationInfo?.aqi || '--'}
                            </span>
                            <span className="text-sm text-blue-700 mb-1 font-medium">AQI</span>
                        </div>
                    </div>

                    <div className="flex-1 min-h-0 flex flex-col">
                        <h3 className="text-lg font-semibold text-gray-700 mb-4">Històric Agrupat</h3>
                        <div className="flex-1 w-full min-h-0 bg-white rounded-xl shadow-inner border border-gray-100 p-4">
                            <StationChart stationCode={stationCode} />
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

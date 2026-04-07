import React, { useEffect, useState } from 'react';
import { X, Activity } from 'lucide-react';
import ReactECharts from 'echarts-for-react';
import api from '../../api/axios';

export default function StationDetailsPanel({ stationCode, onClose }) {
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [stationInfo, setStationInfo] = useState(null);

    useEffect(() => {
        if (!stationCode) return;

        const fetchData = async () => {
            setLoading(true);
            setError(null);
            try {
                const toDate = new Date();
                const fromDate = new Date();
                fromDate.setMonth(fromDate.getMonth() - 1);

                // Formata la data en format ISO compatible amb el LocalDateTime de Spring (ex: 2026-04-02T12:30:00)
                const formatDateTime = (date) => date.toISOString().split('.')[0];

                const [infoRes, measureRes] = await Promise.all([
                    api.get(`/api/v1/stations/${stationCode}`),
                    api.get(`/api/v1/measurements`, {
                        params: {
                            stationCode,
                            from: formatDateTime(fromDate),
                            to: formatDateTime(toDate)
                        }
                    })
                ]);
                setStationInfo(infoRes.data);

                const timestampsSet = new Set();
                const pollutantsObj = {};

                measureRes.data.forEach(item => {
                    const t = item.timestamp;
                    timestampsSet.add(t);
                    if (!pollutantsObj[item.pollutant]) {
                        pollutantsObj[item.pollutant] = {};
                    }
                    pollutantsObj[item.pollutant][t] = item.value;
                });

                const sortedTimestamps = Array.from(timestampsSet).sort((a,b) => new Date(a).getTime() - new Date(b).getTime());

                const timeLabels = sortedTimestamps.map(t => new Date(t).toLocaleDateString([], {
                    day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit'
                }));

                const seriesData = Object.keys(pollutantsObj).map(pollutant => {
                    return {
                        name: pollutant,
                        type: 'line',
                        smooth: true,
                        data: sortedTimestamps.map(t => pollutantsObj[pollutant][t] || null)
                    };
                });

                setHistory({
                    timeLabels,
                    seriesData,
                    pollutants: Object.keys(pollutantsObj)
                });
            } catch (err) {
                console.error("Error fetching station details:", err);
                setError('No s\'ha pogut carregar la informació de l\'estació.');
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, [stationCode]);

    const getChartOptions = () => {
        if (!history || !history.timeLabels) return {};

        const POLLUTANT_THRESHOLDS = {
            NO2: [40, 90, 120, 230, 340],
            PM10: [20, 40, 50, 100, 150],
            PM25: [10, 20, 25, 50, 75],
            'PM2.5': [10, 20, 25, 50, 75],
            PM2_5: [10, 20, 25, 50, 75],
            O3: [50, 100, 130, 240, 380],
            SO2: [100, 200, 350, 500, 750],
            CO: [5, 10, 15, 25, 50]
        };

        const visualMaps = history.pollutants
            .map((pollutant, index) => {
                const thresholds = POLLUTANT_THRESHOLDS[pollutant];
                if (!thresholds) return null;
                
                return {
                    show: false,
                    seriesIndex: index,
                    pieces: [
                        { lte: thresholds[0], color: '#10b981' },               // Good (Green)
                        { gt: thresholds[0], lte: thresholds[1], color: '#facc15' }, // Fair (Yellow)
                        { gt: thresholds[1], lte: thresholds[2], color: '#fb923c' }, // Moderate (Orange)
                        { gt: thresholds[2], lte: thresholds[3], color: '#ef4444' }, // Poor (Red)
                        { gt: thresholds[3], lte: thresholds[4], color: '#9f1239' }, // Very Poor (Dark Red)
                        { gt: thresholds[4], color: '#7e22ce' }                 // Extremely Poor (Purple)
                    ],
                    outOfRange: { color: '#9ca3af' }
                };
            })
            .filter(Boolean);

        return {
            tooltip: {
                trigger: 'axis'
            },
            legend: {
                data: history.pollutants,
                top: 0
            },
            visualMap: visualMaps,
            grid: {
                top: 40,
                bottom: 20,
                left: 40,
                right: 20,
                containLabel: true
            },
            xAxis: {
                type: 'category',
                boundaryGap: false,
                data: history.timeLabels,
            },
            yAxis: {
                type: 'value'
            },
            series: history.seriesData
        };
    };

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
                                {stationInfo?.aqi || (history.length > 0 ? history[history.length-1].aqi : '--')}
                            </span>
                            <span className="text-sm text-blue-700 mb-1 font-medium">AQI</span>
                        </div>
                    </div>

                    <div className="flex-1 min-h-0 flex flex-col">
                        <h3 className="text-lg font-semibold text-gray-700 mb-4">Històric (Últim mes)</h3>
                        {history.timeLabels && history.timeLabels.length > 0 ? (
                            <div className="flex-1 w-full">
                                <ReactECharts option={getChartOptions()} style={{ height: '100%', width: '100%' }} />
                            </div>
                        ) : (
                            <div className="text-sm text-gray-400 italic">No hi ha dades recents.</div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}

import React, { useEffect, useState, useMemo } from 'react';
import ReactECharts from 'echarts-for-react';
import api from '../../api/axios';
import { Activity } from 'lucide-react';

export default function StationChart({ stationCode }) {
    const [historyData, setHistoryData] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [resolution, setResolution] = useState('1D'); // 1D, 1W, 1M, 1Y

    useEffect(() => {
        if (!stationCode) return;

        const fetchAggregatedData = async () => {
            setLoading(true);
            setError(null);
            try {
                const endDate = new Date();
                const startDate = new Date();

                switch (resolution) {
                    case '1D':
                        startDate.setDate(endDate.getDate() - 1);
                        break;
                    case '1W':
                        startDate.setDate(endDate.getDate() - 7);
                        break;
                    case '1M':
                        startDate.setMonth(endDate.getMonth() - 1);
                        break;
                    case '1Y':
                        startDate.setFullYear(endDate.getFullYear() - 1);
                        break;
                    default:
                        break;
                }

                // Format per com.aerogrid.backend (Spring ISO util)
                const formatDateTime = (date) => date.toISOString().split('.')[0];

                const res = await api.get(`/api/v1/stations/${stationCode}/measurements`, {
                    params: {
                        startDate: formatDateTime(startDate),
                        endDate: formatDateTime(endDate),
                        resolution: resolution
                    }
                });

                // Result format: [{ timestamp, pollutant, avgValue, avgAqi }, ...]
                setHistoryData(res.data);
            } catch (err) {
                console.error("Error fetching aggregated data: ", err);
                setError('No s\'ha pogut carregar l\'històric per aquesta resolució.');
            } finally {
                setLoading(false);
            }
        };

        fetchAggregatedData();
    }, [stationCode, resolution]);

    const getChartOptions = useMemo(() => {
        if (!historyData || historyData.length === 0) return {};

        const timestampsSet = new Set();
        const pollutantsObj = {};
        const aqiObj = {}; // For overall AQI chart

        historyData.forEach(item => {
            const t = item.timestamp;
            timestampsSet.add(t);
            if (!pollutantsObj[item.pollutant]) {
                pollutantsObj[item.pollutant] = {};
            }
            pollutantsObj[item.pollutant][t] = item.avgValue;

            if (!aqiObj[t] || item.avgAqi > aqiObj[t]) {
                aqiObj[t] = item.avgAqi || 0;
            }
        });

        const sortedTimestamps = Array.from(timestampsSet).sort((a,b) => new Date(a).getTime() - new Date(b).getTime());

        const timeLabels = sortedTimestamps.map(t => {
            const d = new Date(t);
            if (resolution === '1D' || resolution === '1W') {
                return d.toLocaleDateString([], { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
            } else {
                return d.toLocaleDateString([], { year: 'numeric', month: '2-digit', day: '2-digit' });
            }
        });

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

        const pollutants = Object.keys(pollutantsObj);

        const seriesData = pollutants.map(pollutant => {
            return {
                name: pollutant,
                type: 'line',
                smooth: true,
                data: sortedTimestamps.map(t => pollutantsObj[pollutant][t] != null ? Math.round(pollutantsObj[pollutant][t]) : null)
            };
        });

        const visualMaps = pollutants
            .map((pollutant, index) => {
                const thresholds = POLLUTANT_THRESHOLDS[pollutant];
                if (!thresholds) return null;

                return {
                    show: false,
                    seriesIndex: index,
                    pieces: [
                        { lte: thresholds[0], color: '#10b981' },
                        { gt: thresholds[0], lte: thresholds[1], color: '#facc15' },
                        { gt: thresholds[1], lte: thresholds[2], color: '#fb923c' },
                        { gt: thresholds[2], lte: thresholds[3], color: '#ef4444' },
                        { gt: thresholds[3], lte: thresholds[4], color: '#9f1239' },
                        { gt: thresholds[4], color: '#7e22ce' }
                    ],
                    outOfRange: { color: '#9ca3af' }
                };
            })
            .filter(Boolean);

        const mainOptions = {
            tooltip: {
                trigger: 'axis',
                valueFormatter: (value) => value != null ? Math.round(value) : '-'
            },
            legend: {
                data: pollutants,
                top: 0
            },
            visualMap: visualMaps,
            grid: {
                top: 40,
                bottom: 50,
                left: 40,
                right: 20,
                containLabel: true
            },
            xAxis: {
                type: 'category',
                boundaryGap: false,
                data: timeLabels,
            },
            yAxis: {
                type: 'value',
                name: 'Concentració',
                axisLabel: {
                    formatter: (value) => Math.round(value)
                }
            },
            dataZoom: [
                {
                    type: 'slider',
                    show: true,
                    bottom: 0, // Make room for second chart if needed? No, let's use a dual grid or two charts
                    start: 0,
                    end: 100
                }
            ],
            series: seriesData
        };

        const aqiSeriesData = {
            name: 'AQI General',
            type: 'line',
            smooth: true,
            data: sortedTimestamps.map(t => aqiObj[t] != null ? Math.round(aqiObj[t]) : null),
            areaStyle: {
                color: {
                    type: 'linear',
                    x: 0, y: 0, x2: 0, y2: 1,
                    colorStops: [{
                        offset: 0, color: 'rgba(59, 130, 246, 0.5)'
                    }, {
                        offset: 1, color: 'rgba(59, 130, 246, 0.1)'
                    }]
                }
            },
            itemStyle: { color: '#3b82f6' },
            lineStyle: { width: 3, color: '#3b82f6' }
        };

        const aqiChartOptions = {
            tooltip: { 
                trigger: 'axis',
                valueFormatter: (value) => value != null ? Math.round(value) : '-'
            },
            grid: {
                top: 20,
                bottom: 20,
                left: 40,
                right: 20,
                containLabel: true
            },
            xAxis: {
                type: 'category',
                boundaryGap: false,
                data: timeLabels,
                show: false // Hide x-axis labels on the second chart to keep it clean
            },
            yAxis: {
                type: 'value',
                name: 'AQI',
                min: 1,
                max: 6,
                splitNumber: 5,
                axisLabel: {
                    formatter: (value) => Math.round(value)
                }
            },
            visualMap: {
                show: false,
                pieces: [
                    { lte: 1, color: '#10b981' },
                    { gt: 1, lte: 2, color: '#facc15' },
                    { gt: 2, lte: 3, color: '#fb923c' },
                    { gt: 3, lte: 4, color: '#ef4444' },
                    { gt: 4, lte: 5, color: '#9f1239' },
                    { gt: 5, color: '#7e22ce' }
                ],
                outOfRange: { color: '#9ca3af' }
            },
            series: [aqiSeriesData]
        };

        return { mainOptions, aqiOptions: aqiChartOptions };
    }, [historyData, resolution]);

    return (
        <div className="flex flex-col h-full w-full">
            <div className="flex gap-2 justify-center mb-4">
                {['1D', '1W', '1M', '1Y'].map(res => (
                    <button
                        key={res}
                        onClick={() => setResolution(res)}
                        className={`px-4 py-1 rounded-full text-sm font-medium transition-colors ${
                            resolution === res 
                            ? 'bg-blue-600 text-white shadow-md' 
                            : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                        }`}
                    >
                        {res}
                    </button>
                ))}
            </div>

            <div className="flex-1 w-full relative min-h-0">
                {loading ? (
                    <div className="absolute inset-0 flex flex-col items-center justify-center">
                        <Activity className="w-8 h-8 text-blue-500 animate-pulse mb-2" />
                        <span className="text-sm text-gray-500">Agrupant dades...</span>
                    </div>
                ) : error ? (
                    <div className="absolute inset-0 flex items-center justify-center text-center p-4 text-red-500">
                        {error}
                    </div>
                ) : historyData.length === 0 ? (
                    <div className="absolute inset-0 flex items-center justify-center text-sm text-gray-400 italic">
                        No hi ha dades per aquesta resolució.
                    </div>
                ) : (
                    <div className="flex flex-col h-full">
                        <div className="flex-1 min-h-0">
                            <ReactECharts 
                                option={getChartOptions.mainOptions} 
                                style={{ height: '100%', width: '100%' }} 
                                notMerge={true} 
                            />
                        </div>
                        <div className="h-1/3 min-h-[150px] border-t border-gray-100 pt-2 mt-2">
                            <div className="text-xs font-semibold text-gray-600 text-center mb-1">Evolució AQI Índex General (1-6)</div>
                            <ReactECharts 
                                option={getChartOptions.aqiOptions} 
                                style={{ height: 'calc(100% - 20px)', width: '100%' }} 
                                notMerge={true} 
                            />
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}

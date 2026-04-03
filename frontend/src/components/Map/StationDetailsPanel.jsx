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

                const formattedData = measureRes.data.map(item => ({
                    time: new Date(item.timestamp).toLocaleDateString([], { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }),
                    aqi: item.aqi
                }));

                setHistory(formattedData);
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
        return {
            tooltip: {
                trigger: 'axis'
            },
            grid: {
                top: 10,
                bottom: 20,
                left: 30,
                right: 10
            },
            xAxis: {
                type: 'category',
                boundaryGap: false,
                data: history.map(item => item.time),
            },
            yAxis: {
                type: 'value'
            },
            series: [
                {
                    data: history.map(item => item.aqi),
                    type: 'line',
                    smooth: true,
                    lineStyle: {
                        color: '#3b82f6',
                        width: 3
                    },
                    itemStyle: {
                        color: '#2563eb'
                    },
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
                    }
                }
            ]
        };
    };

    return (
        <div className="absolute top-0 right-0 h-full w-96 bg-white/95 backdrop-blur-md shadow-2xl p-6 flex flex-col z-50 transform transition-transform duration-300">
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

                    <div className="flex-1 min-h-0">
                        <h3 className="text-lg font-semibold text-gray-700 mb-4">Històric (Últim mes)</h3>
                        {history.length > 0 ? (
                            <div className="h-64 w-full">
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


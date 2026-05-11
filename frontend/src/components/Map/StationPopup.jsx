import React, { useEffect, useState } from 'react';
import { Popup } from 'react-map-gl/maplibre';
import { X, ThumbsUp, ThumbsDown, Loader2, ShieldCheck } from 'lucide-react';
import api from '../../api/axios';
const getAqiDetails = (aqi) => {
    switch (aqi) {
        case 1: return { label: 'Bona', bg: 'bg-green-500', text: 'text-white' };
        case 2: return { label: 'Raonablement bona', bg: 'bg-yellow-400', text: 'text-yellow-900' };
        case 3: return { label: 'Regular', bg: 'bg-orange-500', text: 'text-white' };
        case 4: return { label: 'Desfavorable', bg: 'bg-red-500', text: 'text-white' };
        case 5: return { label: 'Molt desfavorable', bg: 'bg-rose-700', text: 'text-white' };
        case 6: return { label: 'Extremadament desfavorable', bg: 'bg-purple-700', text: 'text-white' };
        default: return { label: '--', bg: 'bg-gray-200', text: 'text-gray-600' };
    }
};
const getPollutantName = (code) => {
    if(!code) return '--';
    const maps = {
        'O3': 'O3',
        'NO2': 'NO2',
        'PM10': 'PM10',
        'PM25': 'PM2.5',
        'PM2.5': 'PM2.5',
        'SO2': 'SO2',
        'CO': 'CO',
        'H2S': 'H2S',
        'C6H6': 'C6H6',
        'PM1': 'PM1'
    };
    return maps[code] || code;
}
export default function StationPopup({ station, targetTime, onClose, onViewDetails }) {
    const [measurements, setMeasurements] = useState([]);
    const [loading, setLoading] = useState(true);
    const [metrics, setMetrics] = useState({ upvotes: 0, downvotes: 0 });
    const [userVote, setUserVote] = useState(null);
    const rawAqi = station.properties.aqi;
    const aqi = rawAqi !== null && rawAqi !== undefined ? Number(rawAqi) : 0;
    const rawTrustScore = station.properties.trustScore ?? station.trustScore;
    const [trustScore, setTrustScore] = useState(rawTrustScore !== null && rawTrustScore !== undefined ? Number(rawTrustScore) : 100);
    const aqiTheme = getAqiDetails(aqi);

    useEffect(() => {
        const fetchDetails = async () => {
            setLoading(true);
            try {
                // Try to get station details first to ensure we have the trustScore
                try {
                    const statsRes = await api.get(`/api/v1/stations/${station.properties.code || station.code}`);
                    setMetrics({
                        upvotes: statsRes.data.upvotes || 0,
                        downvotes: statsRes.data.downvotes || 0
                    });
                    if (statsRes.data.trustScore !== undefined && statsRes.data.trustScore !== null) {
                        setTrustScore(Number(statsRes.data.trustScore));
                    }
                    const voteRes = await api.get(`/api/v1/stations/${station.properties.id || station.id}/votes/me`).catch(() => null);
                    if (voteRes && voteRes.data) {
                        setUserVote(voteRes.data.type);
                    }
                } catch (e) {
                    console.error("Error fetching station stats", e);
                }

                const params = { stationCode: station.properties.code || station.code };
                if (targetTime) params.targetTime = targetTime;
                const res = await api.get('/api/v1/measurements', { params });
                const mData = res.data;
                if (mData && mData.length > 0) {
                    mData.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
                    const latestTs = mData[0].timestamp;
                    const currentMeasurements = mData.filter(m => m.timestamp === latestTs);
                    const mainPollutants = currentMeasurements
                        .filter(m => ['NO2', 'O3', 'PM10', 'PM2.5', 'PM25'].includes(m.pollutant))
                        .slice(0, 4); 
                    if (mainPollutants.length === 0) {
                        setMeasurements(currentMeasurements.slice(0, 4));
                    } else {
                        setMeasurements(mainPollutants);
                    }
                } else {
                    setMeasurements([]);
                }
            } catch (err) {
                console.error("Error fetching popup details", err);
                setMeasurements([]);
            } finally {
                setLoading(false);
            }
        };
        fetchDetails();
    }, [station.properties.code, station.properties.id, targetTime]);
    const handleVote = async (type) => {
        try {
            if (userVote === type) {
                await api.delete(`/api/v1/stations/${station.properties.id}/votes/me`);
                setUserVote(null);
                setMetrics(prev => ({
                    ...prev,
                    [type === 'POSITIVE' ? 'upvotes' : 'downvotes']: prev[type === 'POSITIVE' ? 'upvotes' : 'downvotes'] - 1
                }));
            } else {
                await api.put(`/api/v1/stations/${station.properties.id}/votes/me`, { type });
                setMetrics(prev => {
                    let newUp = prev.upvotes; let newDown = prev.downvotes;
                    if (userVote === 'POSITIVE') newUp--; if (userVote === 'NEGATIVE') newDown--;
                    if (type === 'POSITIVE') newUp++; if (type === 'NEGATIVE') newDown++;
                    return { upvotes: newUp, downvotes: newDown };
                });
                setUserVote(type);
            }
        } catch (e) {
            console.error("Error voting:", e);
            if (e.response?.status === 401 || e.response?.status === 403) {
                alert("Has d'iniciar sessió per votar.");
            }
        }
    };
    return (
        <Popup longitude={station.longitude} latitude={station.latitude} anchor="bottom" onClose={onClose} closeOnClick={false} closeButton={false} className="rounded-xl overflow-hidden shadow-2xl z-10 custom-map-popup" maxWidth="320px">
            <div className="w-[280px] bg-white rounded-xl overflow-hidden flex flex-col relative">
                <div className={`px-4 py-3 flex justify-between items-start ${aqiTheme.bg} ${aqiTheme.text}`}>
                    <div className="pr-6">
                        <div className="text-xs opacity-90 font-medium uppercase tracking-wider mb-1">
                            AQI: {aqi} • {aqiTheme.label}
                        </div>
                        <h3 className="font-bold text-base leading-tight">
                            {station.properties.name}
                        </h3>
                    </div>
                </div>
                <button onClick={onClose} className="absolute top-2 right-2 p-1.5 bg-black/10 hover:bg-black/20 rounded-full text-white transition-colors">
                    <X size={16} strokeWidth={2.5} />
                </button>
                <div className="p-4 flex flex-col gap-4">
                    <div>
                        <h4 className="text-xs font-semibold text-gray-500 uppercase mb-2">Resum de Contaminants</h4>
                        {loading ? (
                            <div className="flex justify-center py-4">
                                <Loader2 className="animate-spin text-gray-400" size={20} />
                            </div>
                        ) : measurements.length > 0 ? (
                            <div className="grid grid-cols-2 gap-2">
                                {measurements.map((m, idx) => (
                                    <div key={idx} className="bg-gray-50 rounded px-2 py-1.5 border border-gray-100 flex justify-between items-center">
                                        <span className="text-xs font-medium text-gray-600">{getPollutantName(m.pollutant)}</span>
                                        <span className="text-xs font-bold text-gray-900">{parseFloat(m.value).toFixed(2)}</span>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <p className="text-sm text-gray-500 italic text-center py-2">Sense dades per aquest moment.</p>
                        )}
                    </div>

                    {(() => {
                        const totalVotes = metrics.upvotes + metrics.downvotes;
                        const isUnrated = totalVotes < 5;
                        let trustStatus = { label: 'Molt baixa', color: 'text-red-700', bg: 'bg-red-50', border: 'border-red-200' };

                        if (isUnrated) {
                            trustStatus = { label: 'Sense valorar', color: 'text-gray-500', bg: 'bg-gray-50', border: 'border-gray-200' };
                        } else if (trustScore >= 80) {
                            trustStatus = { label: 'Excel·lent', color: 'text-green-700', bg: 'bg-green-50', border: 'border-green-200' };
                        } else if (trustScore > 30) {
                            trustStatus = { label: 'Bona', color: 'text-blue-700', bg: 'bg-blue-50', border: 'border-blue-200' };
                        } else if (trustScore > 15) {
                            trustStatus = { label: 'Baixa', color: 'text-orange-700', bg: 'bg-orange-50', border: 'border-orange-200' };
                        }

                        return (
                            <>
                                <div className="flex justify-center mb-1">
                                    <div className={`flex items-center gap-1.5 px-2.5 py-1 ${trustStatus.bg} border ${trustStatus.border} rounded-full text-xs font-medium shadow-sm transition-colors`}>
                                        <ShieldCheck size={14} className={trustStatus.color} />
                                        <span className={trustStatus.color}>
                                            Fiabilitat: <span className="font-bold">{isUnrated ? trustStatus.label : `${trustScore}% (${trustStatus.label})`}</span>
                                        </span>
                                    </div>
                                </div>
                            </>
                        );
                    })()}

                    <div className="flex justify-between items-center pt-3 border-t border-gray-100">
                        <div className="flex gap-2">
                            <button onClick={() => handleVote('POSITIVE')} className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-md text-xs font-medium transition-colors ${userVote === 'POSITIVE' ? 'bg-green-100 text-green-700' : 'bg-gray-50 text-gray-600 hover:bg-gray-100'}`}>
                                <ThumbsUp size={14} className={userVote === 'POSITIVE' ? 'fill-green-600' : ''} />
                                <span>{metrics.upvotes}</span>
                            </button>
                            <button onClick={() => handleVote('NEGATIVE')} className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-md text-xs font-medium transition-colors ${userVote === 'NEGATIVE' ? 'bg-red-100 text-red-700' : 'bg-gray-50 text-gray-600 hover:bg-gray-100'}`}>
                                <ThumbsDown size={14} className={userVote === 'NEGATIVE' ? 'fill-red-600' : ''} />
                                <span>{metrics.downvotes}</span>
                            </button>
                        </div>
                        <button onClick={() => onViewDetails(station.properties.code)} className="px-3 py-1.5 bg-blue-600 text-white text-xs font-semibold rounded-md hover:bg-blue-700 transition-colors shadow-sm">
                            Veure detalls
                        </button>
                    </div>
                </div>
            </div>
        </Popup>
    );
}

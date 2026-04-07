import React, { useEffect, useState } from 'react';
import { X, Activity } from 'lucide-react';
import api from '../../api/axios';
import StationChart from './StationChart';

const getAqiLabel = (aqi) => {
    switch (aqi) {
        case 1: return 'Bona';
        case 2: return 'Raonablement bona';
        case 3: return 'Regular';
        case 4: return 'Desfavorable';
        case 5: return 'Molt desfavorable';
        case 6: return 'Extremadament desfavorable';
        default: return '--';
    }
};

const getAqiColor = (aqi) => {
    switch (aqi) {
        case 1: return { bg: 'bg-green-500', text: 'text-green-900', lightBg: 'bg-green-50', border: 'border-green-500', badgeTheme: 'text-white' };
        case 2: return { bg: 'bg-yellow-400', text: 'text-yellow-900', lightBg: 'bg-yellow-50', border: 'border-yellow-400', badgeTheme: 'text-yellow-900' };
        case 3: return { bg: 'bg-orange-500', text: 'text-orange-900', lightBg: 'bg-orange-50', border: 'border-orange-500', badgeTheme: 'text-white' };
        case 4: return { bg: 'bg-red-500', text: 'text-red-900', lightBg: 'bg-red-50', border: 'border-red-500', badgeTheme: 'text-white' };
        case 5: return { bg: 'bg-rose-700', text: 'text-rose-900', lightBg: 'bg-rose-50', border: 'border-rose-700', badgeTheme: 'text-white' };
        case 6: return { bg: 'bg-purple-700', text: 'text-purple-900', lightBg: 'bg-purple-50', border: 'border-purple-700', badgeTheme: 'text-white' };
        default: return { bg: 'bg-gray-400', text: 'text-gray-900', lightBg: 'bg-gray-50', border: 'border-gray-400', badgeTheme: 'text-white' };
    }
};

const getPollutantName = (code) => {
    if(!code) return '--';
    const maps = {
        'O3': 'O3 (Ozó)',
        'NO2': 'NO2 (Diòxid de nitrogen)',
        'PM10': 'PM10 (Partícules <10µm)',
        'PM25': 'PM2.5 (Partícules <2.5µm)',
        'PM2.5': 'PM2.5 (Partícules <2.5µm)',
        'SO2': 'SO2 (Diòxid de sofre)',
        'CO': 'CO (Monòxid de carboni)',
        'H2S': 'H2S (Àcid sulfhídric)',
        'C6H6': 'C6H6 (Benzè)',
        'PM1': 'PM1 (Partícules <1µm)'
    };
    return maps[code] || code;
}

const timeAgo = (dateStr) => {
    if (!dateStr) return '';
    const diff = Math.floor((new Date() - new Date(dateStr)) / 60000);
    if (diff < 0) return `recentment`;
    if (diff < 60) return `fa ${diff} minuts`;
    if (diff < 1440) return `fa ${Math.floor(diff/60)} hores`;
    return `fa ${Math.floor(diff/1440)} dies`;
};

export default function StationDetailsPanel({ stationCode, onClose }) {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [stationInfo, setStationInfo] = useState(null);
    const [detectedPollutants, setDetectedPollutants] = useState([]);
    const [latestData, setLatestData] = useState({
        measurements: [],
        aqi: null,
        worstPollutant: null,
        timestamp: null
    });

    useEffect(() => {
        // Prepare global callback per capturar els contaminants de StationChart
        window.__pollutantsCallback = (pollutants) => {
            setDetectedPollutants(pollutants);
        };

        return () => {
            delete window.__pollutantsCallback;
        };
    }, []);

    useEffect(() => {
        if (!stationCode) return;

        const fetchData = async () => {
            setLoading(true);
            setError(null);
            try {
                const res = await api.get(`/api/v1/stations/${stationCode}`);
                setStationInfo(res.data);

                // Fetch recent measurements directly to reliably display instant top metrics
                try {
                    const mRes = await api.get('/api/v1/measurements', { params: { stationCode } });
                    const mData = mRes.data;

                    if (mData && mData.length > 0) {
                        mData.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
                        const latestTs = mData[0].timestamp;
                        const currentMeasurements = mData.filter(m => m.timestamp === latestTs);

                        let maxAqi = null;
                        let worstPollutant = currentMeasurements[0]?.pollutant || null;

                        currentMeasurements.forEach(m => {
                            if (m.aqi) {
                                if (maxAqi === null || m.aqi > maxAqi) {
                                    maxAqi = m.aqi;
                                    worstPollutant = m.pollutant;
                                }
                            }
                        });

                        setLatestData({
                            measurements: currentMeasurements,
                            aqi: maxAqi,
                            worstPollutant: worstPollutant,
                            timestamp: latestTs
                        });
                    } else {
                        setLatestData({ measurements: [], aqi: null, worstPollutant: null, timestamp: null });
                    }
                } catch (merr) {
                    console.error("No s'han pogut obtenir les mesures recents per la capçalera", merr);
                }
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

                    {(() => {
                        const aqiTheme = getAqiColor(latestData.aqi);
                        return (
                            <div className={`rounded-xl p-4 mb-6 border-l-4 shadow-sm transition-colors ${aqiTheme.lightBg} ${aqiTheme.border}`}>
                                {/* 1. L'Índex Principal & 2. Contaminant Crític */}
                                <div className="flex items-center gap-4 mb-4">
                                    <div className={`w-14 h-14 flex items-center justify-center rounded-xl shadow-sm text-3xl font-black ${aqiTheme.bg} ${aqiTheme.badgeTheme}`}>
                                        {latestData.aqi || '-'}
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <div className={`text-xl font-extrabold ${aqiTheme.text} leading-tight`}>
                                            {latestData.aqi ? `${latestData.aqi} - ${getAqiLabel(latestData.aqi)}` : 'Sense dades de moment'}
                                        </div>
                                        <div className="text-sm font-medium text-gray-600 mt-0.5 truncate">
                                            Contaminant principal ara mateix: <span className="font-bold text-gray-800">{getPollutantName(latestData.worstPollutant)}</span>
                                        </div>
                                    </div>
                                </div>

                                {/* 3. Mini-Targetes dels Contaminants */}
                                {latestData.measurements.length > 0 && (
                                    <div className="flex flex-wrap gap-2 mb-4">
                                        {latestData.measurements.map(m => {
                                            const isWorst = m.pollutant === latestData.worstPollutant;
                                            return (
                                                <div key={m.pollutant} className={`px-2.5 py-1.5 rounded-lg shadow-sm border text-sm font-medium flex items-center gap-1.5 ${isWorst ? 'bg-white border-gray-300 ring-1 ring-gray-200' : 'bg-white/60 border-white/40'}`}>
                                                    <span className={`font-bold ${isWorst ? aqiTheme.text : 'text-gray-500'}`}>{m.pollutant}:</span>
                                                    <span className="text-gray-800">{Number(Number(m.value).toFixed(2))}</span>
                                                    <span className="text-gray-500 text-xs">{m.pollutant === 'CO' ? 'mg/m³' : 'µg/m³'}</span>
                                                </div>
                                            );
                                        })}
                                    </div>
                                )}

                                {/* 4. Metadades del Sensor */}
                                <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-2 text-xs font-medium text-gray-500 pt-3 border-t border-gray-200/50">
                                    <span className="flex items-center gap-1.5 bg-white px-2.5 py-1 rounded-md border border-gray-200 shadow-sm text-gray-700">
                                        {stationInfo?.sourceType === 'OFFICIAL' ? '🏛️ Estació Oficial (Generalitat)' : '👤 Sensor Ciutadà'}
                                    </span>
                                    <span className="flex items-center gap-1">
                                        Dades actualitzades {latestData.timestamp ? timeAgo(latestData.timestamp) : '--'}
                                    </span>
                                </div>
                            </div>
                        );
                    })()}

                    {/* Container dels Gràfics */}
                    <div className="flex-1 overflow-y-auto min-h-0 relative bg-gray-50/50">
                        <StationChart stationCode={stationCode} />
                        
                        {/* Informació addicional i llegendes dinamiques */}
                        <div className="p-4 mx-4 mb-4 bg-white rounded-xl shadow-sm border border-gray-100 dark:bg-gray-800 dark:border-gray-700">
                          <div className="flex items-center gap-2 mb-4">
                            <svg className="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                            <span className="font-medium text-gray-700 dark:text-gray-300">Entendre les Mesures d'aquesta Estació</span>
                          </div>

                          <div className="space-y-4 text-sm text-gray-600 dark:text-gray-400">
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                              <div>
                                <h4 className="font-semibold text-gray-800 dark:text-gray-200 mb-2">Contaminants Detectats</h4>
                                <ul className="space-y-2">
                                  {detectedPollutants.length === 0 && (
                                      <li className="text-gray-500 italic">Carregant contaminants...</li>
                                  )}
                                  {(detectedPollutants.includes('PM25') || detectedPollutants.includes('PM2.5') || detectedPollutants.includes('PM10')) && (
                                    <li><strong className="text-gray-700 dark:text-gray-300">PM2.5 / PM10:</strong> Partícules fines en suspensió. Poden penetrar profundament als pulmons.</li>
                                  )}
                                  {detectedPollutants.includes('NO2') && (
                                    <li><strong className="text-gray-700 dark:text-gray-300">NO2:</strong> Diòxid de nitrogen. Principalment del trànsit, afecta les vies respiratòries.</li>
                                  )}
                                  {detectedPollutants.includes('O3') && (
                                    <li><strong className="text-gray-700 dark:text-gray-300">O3:</strong> Ozó troposfèric. Irritant respiratori format per reaccions químiques al sol.</li>
                                  )}
                                  {detectedPollutants.includes('SO2') && (
                                    <li><strong className="text-gray-700 dark:text-gray-300">SO2:</strong> Diòxid de sofre, provinent de la crema de combustibles fòssils.</li>
                                  )}
                                  {detectedPollutants.includes('CO') && (
                                    <li><strong className="text-gray-700 dark:text-gray-300">CO:</strong> Monòxid de carboni, gas tòxic de la combustió incompleta de vehicles.</li>
                                  )}

                                  {/* Contaminants especials sense AQI establert segons el servei */}
                                  {detectedPollutants.includes('H2S') && (
                                    <li><strong className="text-gray-700 dark:text-gray-300">H2S:</strong> Àcid sulfhídric. Olor molt forta a "ous podrits", típic de processos industrials. <em>(No té llindars AQI globals estandarditzats per a la població general)</em>.</li>
                                  )}
                                  {detectedPollutants.includes('PM1') && (
                                    <li><strong className="text-gray-700 dark:text-gray-300">PM1:</strong> Partícules ultrafines. <em>(Encara no s'inclouen als reglaments d'AQI per manca de dades històriques, tot i ser més perilloses que les PM2.5)</em>.</li>
                                  )}
                                  {detectedPollutants.includes('C6H6') && (
                                    <li><strong className="text-gray-700 dark:text-gray-300">C6H6:</strong> Benzè. És un COV cancerigen de la indústria i trànsit. <em>(Es regula amb mitjanes anuals per la seva toxicitat crònica, no amb índexs AQI horaris)</em>.</li>
                                  )}
                                </ul>
                              </div>

                              <div>
                                <h4 className="font-semibold text-gray-800 dark:text-gray-200 mb-2">Categories AQI (General)</h4>
                                <ul className="space-y-2">
                                  <li><span className="inline-block w-3 h-3 rounded-full mr-2" style={{ backgroundColor: '#10b981' }}></span><strong>Bona (1):</strong> Qualitat de l'aire satisfactòria, sense risc per a la salut.</li>
                                  <li><span className="inline-block w-3 h-3 rounded-full mr-2" style={{ backgroundColor: '#facc15' }}></span><strong>Raonablement bona (2):</strong> Qualitat acceptable, els contaminants estan dins dels límits legals.</li>
                                  <li><span className="inline-block w-3 h-3 rounded-full mr-2" style={{ backgroundColor: '#fb923c' }}></span><strong>Regular (3):</strong> Pot afectar lleugerament grups de risc.</li>
                                  <li><span className="inline-block w-3 h-3 rounded-full mr-2" style={{ backgroundColor: '#ef4444' }}></span><strong>Desfavorable (4):</strong> Possibles efectes sobre la salut en grups sensibles.</li>
                                  <li><span className="inline-block w-3 h-3 rounded-full mr-2" style={{ backgroundColor: '#9f1239' }}></span><strong>Molt desfavorable (5):</strong> Condicions d'emergència, tota la població pot notar efectes.</li>
                                  <li><span className="inline-block w-3 h-3 rounded-full mr-2" style={{ backgroundColor: '#7e22ce' }}></span><strong>Extremadament desfavorable (6):</strong> Risc molt greu per a la salut de tothom.</li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

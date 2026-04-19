import { useState, useRef, useEffect } from 'react';
import maplibregl from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { X, Loader2 } from 'lucide-react';
import api from '../../api/axios';

export default function CreateStationModal({ onClose, onSuccess }) {
    const mapContainer = useRef(null);
    const map = useRef(null);
    const marker = useRef(null);

    const [formData, setFormData] = useState({
        name: '',
        municipality: '',
        latitude: '',
        longitude: ''
    });

    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (map.current) return;

        map.current = new maplibregl.Map({
            container: mapContainer.current,
            style: 'https://basemaps.cartocdn.com/gl/positron-gl-style/style.json',
            center: [2.1734, 41.3851], // Default a BCN
            zoom: 10,
            maxBounds: [[-9.3, 36.0], [4.3, 43.8]] // Límits d'Espanya aprox
        });

        map.current.addControl(new maplibregl.NavigationControl(), 'top-right');

        marker.current = new maplibregl.Marker({ color: '#2563eb', draggable: true })
            .setLngLat([2.1734, 41.3851])
            .addTo(map.current);

        marker.current.on('dragend', () => {
            const lngLat = marker.current.getLngLat();
            setFormData(prev => ({
                ...prev,
                latitude: lngLat.lat.toFixed(6),
                longitude: lngLat.lng.toFixed(6)
            }));
        });

        map.current.on('click', (e) => {
            marker.current.setLngLat(e.lngLat);
            setFormData(prev => ({
                ...prev,
                latitude: e.lngLat.lat.toFixed(6),
                longitude: e.lngLat.lng.toFixed(6)
            }));
        });

    }, []);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (!formData.name || !formData.municipality || !formData.latitude || !formData.longitude) {
            setError('Si us plau, omple tots els camps o marca al mapa.');
            return;
        }

        setSubmitting(true);
        try {
            const dto = {
                name: formData.name,
                municipality: formData.municipality,
                latitude: parseFloat(formData.latitude),
                longitude: parseFloat(formData.longitude)
            };

            await api.post('/api/v1/stations', dto);
            onSuccess();
        } catch (err) {
            console.error(err);
            setError("Hi ha hagut un error intern en crear l'estació.");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
            <div className="bg-white rounded-xl shadow-2xl w-full max-w-lg overflow-hidden flex flex-col max-h-[90vh]">

                <div className="flex justify-between items-center px-6 py-4 border-b border-gray-100 bg-gray-50/50">
                    <h3 className="text-xl font-bold text-gray-800">Vincular nova estació</h3>
                    <button onClick={onClose} className="p-1.5 text-gray-400 hover:text-gray-700 hover:bg-gray-200 rounded-full transition-colors">
                        <X size={20} />
                    </button>
                </div>

                <div className="overflow-y-auto p-6 flex flex-col gap-6">
                    {error && (
                        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm font-medium">
                            {error}
                        </div>
                    )}

                    <form id="stationForm" onSubmit={handleSubmit} className="flex flex-col gap-4">
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div className="flex flex-col gap-1.5">
                                <label className="text-sm font-semibold text-gray-700">Nom de l'estació</label>
                                <input
                                    type="text"
                                    name="name"
                                    value={formData.name}
                                    onChange={handleChange}
                                    placeholder="Ex: Balcó Poble Sec"
                                    className="px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none transition-all"
                                />
                            </div>
                            <div className="flex flex-col gap-1.5">
                                <label className="text-sm font-semibold text-gray-700">Municipi</label>
                                <input
                                    type="text"
                                    name="municipality"
                                    value={formData.municipality}
                                    onChange={handleChange}
                                    placeholder="Ex: Barcelona"
                                    className="px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none transition-all"
                                />
                            </div>
                        </div>

                        <div className="flex flex-col gap-2 mt-2">
                            <label className="text-sm font-semibold text-gray-700">Ubicació exacte (Fes clic al mapa)</label>
                            <div className="w-full h-[250px] relative rounded-lg border border-gray-200 overflow-hidden shadow-inner">
                                <div ref={mapContainer} className="absolute inset-0" />
                            </div>
                            <div className="flex gap-4 text-xs text-gray-500 font-mono mt-1 px-1">
                                <span>Lat: {formData.latitude || '--'}</span>
                                <span>Lng: {formData.longitude || '--'}</span>
                            </div>
                        </div>
                    </form>
                </div>

                <div className="border-t border-gray-100 p-4 bg-gray-50/50 flex justify-end gap-3">
                    <button
                        onClick={onClose}
                        type="button"
                        disabled={submitting}
                        className="px-4 py-2 font-medium text-gray-600 bg-white border border-gray-200 shadow-sm rounded-lg hover:bg-gray-50 transition-colors disabled:opacity-50"
                    >
                        Cancel·lar
                    </button>
                    <button
                        form="stationForm"
                        type="submit"
                        disabled={submitting}
                        className="px-5 py-2 font-semibold text-white bg-blue-600 shadow rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-70 flex items-center justify-center min-w-[140px]"
                    >
                        {submitting ? <Loader2 size={18} className="animate-spin" /> : "Crear estació"}
                    </button>
                </div>

            </div>
        </div>
    );
}

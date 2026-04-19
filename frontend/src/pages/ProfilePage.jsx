import { useState, useEffect } from 'react';
import { Mail, User, Link, Loader2, Eye, EyeOff, Copy, CheckCircle2 } from 'lucide-react';
import api from '../api/axios';
import FloatingHeader from '../components/UI/FloatingHeader';
import CreateStationModal from '../components/Map/CreateStationModal';

export default function ProfilePage() {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    // Station management states
    const [myStations, setMyStations] = useState([]);
    const [stationsLoading, setStationsLoading] = useState(true);
    const [isShowModal, setIsShowModal] = useState(false);
    const [visibleKeys, setVisibleKeys] = useState({});
    const [copiedKey, setCopiedKey] = useState(null);
    const [successMessage, setSuccessMessage] = useState('');

    const fetchMyStations = async () => {
        setStationsLoading(true);
        try {
            const res = await api.get('/api/v1/stations/me');
            setMyStations(res.data);
        } catch (e) {
            console.error("Error fetching my stations:", e);
        } finally {
            setStationsLoading(false);
        }
    };

    useEffect(() => {
        const fetchUser = async () => {
            try {
                const response = await api.get('/api/v1/users/me');
                setUser(response.data);
            } catch (error) {
                console.error("Error fetching user profile:", error);
            } finally {
                setLoading(false);
            }
        };

        fetchUser();
        fetchMyStations();
    }, []);

    const toggleKeyVisibility = (id) => {
        setVisibleKeys(prev => ({
            ...prev,
            [id]: !prev[id]
        }));
    };

    const copyToClipboard = (text, id) => {
        if (!text) return;
        navigator.clipboard.writeText(text).then(() => {
            setCopiedKey(id);
            setTimeout(() => setCopiedKey(null), 2000);
        });
    };

    const handleCreateSuccess = () => {
        setIsShowModal(false);
        setSuccessMessage("Estació creada amb èxit! Ja la pots veure a la teva llista.");
        setTimeout(() => setSuccessMessage(''), 5000);
        fetchMyStations(); // Refresca llista màgicament
    };

    if (loading) {
        return (
            <div className="flex flex-col min-h-screen bg-gray-50">
                <FloatingHeader />
                <div className="flex flex-1 justify-center items-center">
                    <Loader2 className="animate-spin text-blue-600" size={48} />
                </div>
            </div>
        );
    }

    if (!user) {
        return (
            <div className="flex flex-col min-h-screen bg-gray-50 pt-20">
                <FloatingHeader />
                <div className="flex flex-1 justify-center items-center text-gray-500">
                    No s'ha pogut carregar la informació de l'usuari.
                </div>
            </div>
        );
    }

    return (
        <div className="flex flex-col min-h-screen bg-gray-50">
            <FloatingHeader />

            <main className="flex-1 max-w-4xl w-full mx-auto px-4 py-8 mt-20">
                <h1 className="text-3xl font-bold text-gray-900 mb-8">El meu perfil</h1>

                <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 mb-8 flex flex-col gap-6">
                    <div>
                        <h2 className="text-lg font-semibold text-gray-800 mb-4 border-b pb-2">Dades de l'usuari</h2>
                        <div className="flex flex-col gap-4">
                            <div className="flex items-center gap-3">
                                <div className="p-2 bg-blue-50 rounded-lg text-blue-600">
                                    <User size={20} />
                                </div>
                                <div>
                                    <p className="text-sm text-gray-500 font-medium">Nom d'usuari</p>
                                    <p className="text-gray-900 font-semibold">{user.username}</p>
                                </div>
                            </div>

                            <div className="flex items-center gap-3">
                                <div className="p-2 bg-blue-50 rounded-lg text-blue-600">
                                    <Mail size={20} />
                                </div>
                                <div>
                                    <p className="text-sm text-gray-500 font-medium">Correu electrònic</p>
                                    <p className="text-gray-900 font-semibold">{user.email}</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 flex flex-col pt-0 pb-0">
                    <div className="flex justify-between items-center py-6 border-b border-gray-100">
                        <div className="flex items-center gap-3">
                            <div className="p-2 bg-blue-50 rounded-lg text-blue-600">
                                <Link size={20} />
                            </div>
                            <h2 className="text-lg font-semibold text-gray-800">Les Meves Estacions</h2>
                        </div>
                        <button
                            onClick={() => setIsShowModal(true)}
                            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold rounded-lg shadow-sm transition-colors"
                        >
                            + Vincular nova estació
                        </button>
                    </div>

                    {successMessage && (
                        <div className="bg-green-50 border border-green-200 text-green-800 px-4 py-3 rounded-lg text-sm font-medium mt-6 mb-2 flex items-center gap-2 transition-all">
                            <CheckCircle2 size={18} className="text-green-600" />
                            {successMessage}
                        </div>
                    )}

                    <div className="py-6">
                        {stationsLoading ? (
                            <div className="flex justify-center p-8 text-gray-400">
                                <Loader2 size={32} className="animate-spin" />
                            </div>
                        ) : myStations.length === 0 ? (
                            <div className="bg-gray-50 rounded-lg border border-dashed border-gray-300 p-8 text-center flex flex-col items-center">
                                <p className="text-gray-500 text-sm italic mb-4">
                                    Encara no tens cap estació vinculada a la teva xarxa.
                                </p>
                                <button
                                    onClick={() => setIsShowModal(true)}
                                    className="px-5 py-2.5 bg-white border border-gray-300 shadow-sm text-gray-700 text-sm font-semibold rounded-lg hover:bg-gray-50 transition-colors"
                                >
                                    Vincular la primera estació
                                </button>
                            </div>
                        ) : (
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                {myStations.map(station => (
                                    <div key={station.id} className="border border-gray-200 rounded-xl overflow-hidden hover:border-gray-300 transition-colors shadow-sm bg-white">
                                        <div className="p-4 flex justify-between items-start border-b border-gray-100 bg-gray-50/50">
                                            <div>
                                                <h3 className="font-bold text-gray-900">{station.name}</h3>
                                                <p className="text-xs text-gray-500">{station.municipality} • Codi: <span className="font-mono">{station.code}</span></p>
                                            </div>
                                            <span className={`px-2 py-0.5 text-[10px] font-bold rounded-full ${station.isActive ? 'bg-green-100 text-green-700' : 'bg-gray-200 text-gray-600'}`}>
                                                {station.isActive ? 'Activa' : 'Inactiva'}
                                            </span>
                                        </div>
                                        <div className="p-4 flex flex-col gap-2">
                                            <span className="text-xs font-semibold uppercase text-gray-500 tracking-wider">
                                                Private API Key
                                            </span>
                                            {station.apiKey ? (
                                                <div className="flex items-center gap-1.5">
                                                    <div className="flex-1 bg-gray-100 border border-gray-200 px-3 py-2 rounded-lg text-sm font-mono text-gray-800 tracking-wide break-all">
                                                        {visibleKeys[station.id] ? station.apiKey : 'sk_live_' + '•'.repeat(24)}
                                                    </div>
                                                    <button
                                                        onClick={() => toggleKeyVisibility(station.id)}
                                                        className="p-2 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors border border-transparent shadow-sm"
                                                        title="Mostrar API Key"
                                                    >
                                                        {visibleKeys[station.id] ? <EyeOff size={18} /> : <Eye size={18} />}
                                                    </button>
                                                    <button
                                                        onClick={() => copyToClipboard(station.apiKey, station.id)}
                                                        className={`p-2 rounded-lg transition-colors border shadow-sm flex items-center justify-center min-w-[38px] ${
                                                            copiedKey === station.id 
                                                                ? 'text-white bg-green-500 border-green-500 hover:bg-green-600' 
                                                                : 'text-gray-500 hover:text-blue-600 hover:bg-blue-50 border-transparent bg-white'
                                                        }`}
                                                        title="Copiar al porta-retalls"
                                                    >
                                                        {copiedKey === station.id ? <CheckCircle2 size={18} /> : <Copy size={18} />}
                                                    </button>
                                                </div>
                                            ) : (
                                                <div className="text-sm italic text-gray-500 bg-gray-50 px-3 py-2 rounded border border-gray-100">
                                                    No s'ha trobat cap clau d'API associada.
                                                </div>
                                            )}

                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </main>

            {isShowModal && (
                <CreateStationModal
                    onClose={() => setIsShowModal(false)}
                    onSuccess={handleCreateSuccess}
                />
            )}
        </div>
    );
}





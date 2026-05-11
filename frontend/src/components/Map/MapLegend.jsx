import React, { useState } from 'react';
import { Layers, X } from 'lucide-react';

export default function MapLegend() {
    const [isOpen, setIsOpen] = useState(false);

    if (!isOpen) {
        return (
            <button
                onClick={() => setIsOpen(true)}
                className="absolute bottom-8 left-4 bg-white/95 backdrop-blur shadow-lg p-2.5 rounded-xl text-gray-700 hover:text-blue-600 hover:bg-gray-50 transition-all z-40 border border-gray-200 pointer-events-auto"
                title="Llegenda del mapa"
            >
                <Layers className="w-6 h-6" />
            </button>
        );
    }

    return (
        <div className="absolute bottom-8 left-4 bg-white/95 backdrop-blur-md shadow-2xl rounded-xl p-5 w-72 z-40 transition-all border border-gray-200 flex flex-col max-h-[80vh] overflow-y-auto pointer-events-auto">
            <div className="flex justify-between items-center mb-4 sticky top-0 bg-white/95 py-1 z-10 w-full">
                <h3 className="font-bold text-gray-800 flex items-center gap-2">
                    <Layers className="w-5 h-5 text-blue-500" />
                    Llegenda
                </h3>
                <button onClick={() => setIsOpen(false)} className="text-gray-500 hover:bg-gray-100 p-1.5 rounded-full transition-colors">
                    <X className="w-4 h-4" />
                </button>
            </div>

            <div className="space-y-6 text-sm text-gray-700">
                {/* 1. Forma (Oficial vs Ciutadana) */}
                <section>
                    <h4 className="font-semibold text-gray-900 mb-2 border-b border-gray-100 pb-1">Orígens de dades</h4>
                    <div className="flex items-center gap-3 mb-2">
                        <div className="w-4 h-4 rounded-full bg-blue-500 border border-white shadow-sm shrink-0"></div>
                        <span><strong>Cercle:</strong> Estació Oficial</span>
                    </div>
                    <div className="flex items-center gap-3">
                        <div className="text-blue-500 font-bold text-lg leading-none w-4 text-center drop-shadow-sm shrink-0" style={{ textShadow: '0 0 2px white' }}>▲</div>
                        <span><strong>Triangle:</strong> Sensor Ciutadà</span>
                    </div>
                </section>

                {/* 2. Mida i Opacitat (Fiabilitat) */}
                <section>
                    <h4 className="font-semibold text-gray-900 mb-2 border-b border-gray-100 pb-1">Fiabilitat (Trust Score)</h4>
                    <p className="text-xs text-gray-500 mb-3 leading-relaxed">
                        Es mesura per la transparència i la mida del símbol. A més fiabilitat, més destaca al mapa.
                    </p>
                    <div className="flex items-end justify-between px-2 h-10">
                        <div className="flex flex-col items-center opacity-50 gap-1.5">
                            <div className="w-1.5 h-1.5 bg-gray-600 rounded-full"></div>
                            <span className="text-[10px] font-medium uppercase">Baixa</span>
                        </div>
                        <div className="flex flex-col items-center opacity-80 gap-1.5">
                            <div className="w-3 h-3 bg-gray-600 rounded-full"></div>
                            <span className="text-[10px] font-medium uppercase">Mitja</span>
                        </div>
                        <div className="flex flex-col items-center opacity-100 gap-1.5">
                            <div className="w-5 h-5 bg-gray-600 rounded-full"></div>
                            <span className="text-[10px] font-medium uppercase">Alta</span>
                        </div>
                    </div>
                </section>

                {/* 3. Agrupacions (Clusters) */}
                <section>
                    <h4 className="font-semibold text-gray-900 mb-2 border-b border-gray-100 pb-1">Agrupacions</h4>
                    <div className="flex items-center gap-3 mt-2">
                        <div className="w-8 h-8 rounded-full bg-rose-500 text-white flex items-center justify-center font-bold text-xs ring-2 ring-white shadow-sm shrink-0">5</div>
                        <p className="text-xs leading-relaxed text-gray-600">
                            Els cercles grossos amb <strong>números</strong> representen zones on hi ha vàries estacions juntes d'ambdós tipus. El color reflecteix la <strong>pitjor prevenció AQI</strong> del grup.
                        </p>
                    </div>
                </section>
            </div>
        </div>
    );
}


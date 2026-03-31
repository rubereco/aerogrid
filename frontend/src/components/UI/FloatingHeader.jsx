import { Search, Menu, UserCircle, X } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';

export default function FloatingHeader() {
    const [isMenuOpen, setIsMenuOpen] = useState(false);

    return (
        <div className="absolute top-0 left-0 right-0 z-10 pointer-events-none w-full p-4 md:p-6">
            <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-4 pointer-events-none">
                
                {/* 
                  MOBILE ROW: Logo + Search + Hamburger inside a single Island 
                  DESKTOP: Just the Logo as Left Island
                */}
                <div className="flex items-center justify-between w-full md:w-auto bg-white/90 backdrop-blur-md shadow-lg rounded-full md:rounded-2xl px-4 py-3 pointer-events-auto gap-3">
                    <Link to="/" className="flex items-center gap-2 flex-shrink-0 group">
                        <div className="w-8 h-8 md:w-10 md:h-10 bg-blue-600 rounded-lg flex items-center justify-center shadow-inner group-hover:bg-blue-700 transition-colors">
                            <span className="text-white font-bold text-sm md:text-lg tracking-tighter">AG</span>
                        </div>
                        <span className="font-bold text-gray-800 text-lg md:text-xl hidden md:block">AeroGrid</span>
                    </Link>

                    {/* Mobile Search Bar inline */}
                    <div className="flex-1 md:hidden relative">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 w-4 h-4 pointer-events-none" />
                        <input
                            type="text"
                            placeholder="Cerca..."
                            className="w-full pl-9 pr-3 py-1.5 bg-gray-100/50 hover:bg-gray-100 focus:bg-white border-transparent focus:ring-2 focus:ring-blue-200 rounded-xl outline-none transition-all text-sm text-gray-700 placeholder-gray-500 shadow-inner"
                        />
                    </div>

                    {/* Mobile Hamburger */}
                    <button 
                        onClick={() => setIsMenuOpen(!isMenuOpen)}
                        className="md:hidden p-1.5 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors cursor-pointer flex-shrink-0"
                    >
                        {isMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
                    </button>
                </div>

                {/* DESKTOP SEARCH - Center Island */}
                <div className="hidden md:flex flex-1 max-w-xl bg-white/90 backdrop-blur-md shadow-lg rounded-2xl px-4 py-2 pointer-events-auto items-center">
                    <div className="relative flex items-center w-full">
                        <Search className="absolute left-3 text-gray-400 w-5 h-5 pointer-events-none" />
                        <input
                            type="text"
                            placeholder="Cerca estacions per nom o codi..."
                            className="w-full pl-10 pr-4 py-2.5 bg-gray-100/50 hover:bg-gray-100 focus:bg-white border-transparent focus:ring-2 focus:ring-blue-200 rounded-xl outline-none transition-all text-base text-gray-700 placeholder-gray-500 shadow-inner"
                        />
                    </div>
                </div>

                {/* DESKTOP ACTIONS - Right Island */}
                <div className="hidden md:flex items-center gap-3 bg-white/90 backdrop-blur-md shadow-lg rounded-2xl px-4 py-3 flex-shrink-0 pointer-events-auto">
                    <Link to="/login" className="px-4 py-2 text-sm font-semibold text-gray-700 hover:text-blue-600 hover:bg-gray-50 rounded-xl transition-colors">
                        Inicia sessió
                    </Link>
                    <Link to="/register" className="px-5 py-2 text-sm font-semibold text-white bg-blue-600 hover:bg-blue-700 rounded-xl shadow-md transition-all focus:ring-2 focus:ring-offset-2 focus:ring-blue-500">
                        Registra't
                    </Link>
                </div>

                {/* MOBILE DROPDOWN ACTIONS - Underneath */}
                {isMenuOpen && (
                    <div className="md:hidden flex flex-col w-full gap-2 bg-white/90 backdrop-blur-md shadow-lg rounded-2xl p-4 pointer-events-auto animate-in fade-in slide-in-from-top-4 duration-200 mt-2">
                        <Link to="/login" onClick={() => setIsMenuOpen(false)} className="flex items-center gap-3 px-4 py-3 text-gray-700 hover:bg-gray-50 rounded-xl font-medium transition-colors">
                            <UserCircle className="w-5 h-5 flex-shrink-0" />
                            Inicia sessió
                        </Link>
                        <Link to="/register" onClick={() => setIsMenuOpen(false)} className="flex items-center justify-center px-4 py-3 text-white bg-blue-600 hover:bg-blue-700 rounded-xl font-medium shadow-sm transition-colors">
                            Registra't gratis
                        </Link>
                    </div>
                )}
            </div>
        </div>
    );
}

import { Search, Menu, UserCircle, X, ChevronDown, List, LogOut, LayoutDashboard } from 'lucide-react';
import { useState, useContext, useEffect, useRef } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';
import api from '../../api/axios';

export default function FloatingHeader() {
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const [isDesktopDropdownOpen, setIsDesktopDropdownOpen] = useState(false);

    // Nous estats pel cercador
    const [searchQuery, setSearchQuery] = useState('');
    const [searchData, setSearchData] = useState(null);
    const [isLoadingSearch, setIsLoadingSearch] = useState(false);
    const [searchResults, setSearchResults] = useState([]);
    const [isSearchFocused, setIsSearchFocused] = useState(false);

    const { isAuthenticated, logout } = useContext(AuthContext);
    const dropdownRef = useRef(null);
    const searchRef = useRef(null);
    const searchRefMobile = useRef(null);
    const location = useLocation();

    const isProfilePage = location.pathname === '/profile';

    // Refresh search data when route changes so it fetches the correct stations
    useEffect(() => {
        setSearchData(null);
        setSearchQuery('');
    }, [location.pathname]);

    // Close dropdowns on click outside
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setIsDesktopDropdownOpen(false);
            }
            if (searchRef.current && !searchRef.current.contains(event.target)) {
                setIsSearchFocused(false);
            }
            if (searchRefMobile.current && !searchRefMobile.current.contains(event.target)) {
                setIsSearchFocused(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const loadSearchData = async () => {
        if (searchData || isLoadingSearch) return; // Ja està carregat o carregant
        setIsLoadingSearch(true);
        try {
            const endpoint = isProfilePage && isAuthenticated ? '/api/v1/stations/me' : '/api/v1/stations';
            const response = await api.get(endpoint);
            setSearchData(response.data);
        } catch (err) {
            console.error('Search data setup failed', err);
        } finally {
            setIsLoadingSearch(false);
            setIsSearchFocused(true); // Mantenir obert
        }
    };

    // Filtrar resultats automàticament quan canvia la query o les dades
    useEffect(() => {
        if (!searchQuery.trim() || !searchData) {
            setSearchResults([]);
            return;
        }

        const lowerQuery = searchQuery.toLowerCase();
        const filtered = searchData.filter(station =>
            (station.name && station.name.toLowerCase().includes(lowerQuery)) ||
            (station.code && station.code.toLowerCase().includes(lowerQuery))
        ).slice(0, 8); // Màxim 8 resultats

        setSearchResults(filtered);
    }, [searchQuery, searchData]);

    const handleSearchInput = (e) => {
        setSearchQuery(e.target.value);
    };

    const handleStationSelect = (station) => {
        setSearchQuery('');
        setSearchResults([]);
        setIsSearchFocused(false);
        // Llençar un event personalitzat
        if (isProfilePage) {
            window.dispatchEvent(new CustomEvent('focusProfileStation', {
                detail: { station }
            }));
        } else {
            window.dispatchEvent(new CustomEvent('flyToStation', {
                detail: { station }
            }));
        }
    };

    const handleLogout = () => {
        setIsMenuOpen(false);
        setIsDesktopDropdownOpen(false);
        logout();
    };

    return (
        <div className="absolute top-0 left-0 right-0 z-10 pointer-events-none w-full p-4 md:p-6">
            <div className="w-full flex flex-col md:flex-row items-center justify-between gap-4 pointer-events-none">

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
                <div
                    ref={searchRef}
                    className="hidden md:flex flex-1 max-w-xl bg-white/90 backdrop-blur-md shadow-lg rounded-2xl px-4 py-2 pointer-events-auto items-center relative"
                >
                    <div className="relative flex items-center w-full">
                        <Search className="absolute left-3 text-gray-400 w-5 h-5 pointer-events-none" />
                        <input
                            type="text"
                            value={searchQuery}
                            onFocus={() => {
                                loadSearchData();
                                setIsSearchFocused(true);
                                // Esborrar seleccions del mapa
                                window.dispatchEvent(new CustomEvent('clearMapSelection'));
                            }}
                            onChange={handleSearchInput}
                            placeholder={isProfilePage ? "Cerca les teves estacions..." : "Cerca estacions per nom o codi..."}
                            className="w-full pl-10 pr-4 py-2.5 bg-gray-100/50 hover:bg-gray-100 focus:bg-white border-transparent focus:ring-2 focus:ring-blue-200 rounded-xl outline-none transition-all text-base text-gray-700 placeholder-gray-500 shadow-inner"
                        />
                        {isLoadingSearch && (
                            <div className="absolute right-3 w-4 h-4 rounded-full border-2 border-blue-600 border-t-transparent animate-spin"></div>
                        )}
                    </div>

                    {/* Resultats Desk */}
                    {isSearchFocused && searchQuery && (
                        <div className="absolute top-full left-0 right-0 mt-2 bg-white rounded-2xl shadow-xl border border-gray-100 overflow-hidden z-50">
                            {searchResults.length > 0 ? (
                                searchResults.map(station => (
                                    <button
                                        key={station.code}
                                        onClick={() => handleStationSelect(station)}
                                        className="w-full flex items-center justify-between px-4 py-3 hover:bg-blue-50 border-b border-gray-50 last:border-0 transition-colors text-left"
                                    >
                                        <div>
                                            <div className="text-sm font-semibold text-gray-800">{station.name}</div>
                                            <div className="text-xs text-gray-500">Codi: {station.code}</div>
                                        </div>
                                        {station.aqi > 0 && (
                                            <span className="text-xs font-bold px-2 py-1 bg-gray-100 rounded-lg text-gray-600">
                                                AQI {station.aqi}
                                            </span>
                                        )}
                                    </button>
                                ))
                            ) : (
                                <div className="px-4 py-4 text-sm text-gray-500 text-center">No hi ha resultats per "{searchQuery}"</div>
                            )}
                        </div>
                    )}
                </div>

                {/* DESKTOP ACTIONS - Right Island */}
                <div className="hidden md:flex items-center gap-3 bg-white/90 backdrop-blur-md shadow-lg rounded-2xl px-4 py-3 flex-shrink-0 pointer-events-auto relative" ref={dropdownRef}>
                    {!isAuthenticated ? (
                        <>
                            <Link to="/login" className="px-4 py-2 text-sm font-semibold text-gray-700 hover:text-blue-600 hover:bg-gray-50 rounded-xl transition-colors">
                                Inicia sessió
                            </Link>
                            <Link to="/register" className="px-5 py-2 text-sm font-semibold text-white bg-blue-600 hover:bg-blue-700 rounded-xl shadow-md transition-all focus:ring-2 focus:ring-offset-2 focus:ring-blue-500">
                                Registra't
                            </Link>
                        </>
                    ) : (
                        <div className="relative">
                            <button 
                                onClick={() => setIsDesktopDropdownOpen(!isDesktopDropdownOpen)}
                                className="flex items-center gap-2 px-3 py-1.5 text-sm font-semibold text-gray-700 hover:bg-gray-100 rounded-xl transition-colors"
                            >
                                <Menu className="w-5 h-5" />
                                <span>Menú</span>
                                <ChevronDown className="w-4 h-4 ml-1" />
                            </button>

                            {/* Dropdown content */}
                            {isDesktopDropdownOpen && (
                                <div className="absolute right-0 mt-3 w-56 bg-white rounded-2xl shadow-xl border border-gray-100 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200">
                                    <div className="p-2 flex flex-col">
                                        <Link to="/profile" onClick={() => setIsDesktopDropdownOpen(false)} className="flex items-center gap-3 px-4 py-3 text-gray-700 hover:bg-gray-50 rounded-xl font-medium transition-colors">
                                            <UserCircle className="w-5 h-5" />
                                            Perfil
                                        </Link>
                                        <Link to="/my-stations" onClick={() => setIsDesktopDropdownOpen(false)} className="flex items-center gap-3 px-4 py-3 text-gray-700 hover:bg-gray-50 rounded-xl font-medium transition-colors">
                                            <LayoutDashboard className="w-5 h-5" />
                                            Les meves estacions
                                        </Link>
                                        <div className="h-px bg-gray-100 my-1 mx-2"></div>
                                        <button onClick={handleLogout} className="flex items-center gap-3 px-4 py-3 text-red-600 hover:bg-red-50 rounded-xl font-medium transition-colors text-left">
                                            <LogOut className="w-5 h-5" />
                                            Tancar sessió
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>
                    )}
                </div>

                {/* MOBILE DROPDOWN ACTIONS - Underneath */}
                {isMenuOpen && (
                    <div className="md:hidden flex flex-col w-full gap-2 bg-white/90 backdrop-blur-md shadow-lg rounded-2xl p-4 pointer-events-auto animate-in fade-in slide-in-from-top-4 duration-200 mt-2">
                        {!isAuthenticated ? (
                            <>
                                <Link to="/login" onClick={() => setIsMenuOpen(false)} className="flex items-center gap-3 px-4 py-3 text-gray-700 hover:bg-gray-50 rounded-xl font-medium transition-colors">
                                    <UserCircle className="w-5 h-5 flex-shrink-0" />
                                    Inicia sessió
                                </Link>
                                <Link to="/register" onClick={() => setIsMenuOpen(false)} className="flex items-center justify-center px-4 py-3 text-white bg-blue-600 hover:bg-blue-700 rounded-xl font-medium shadow-sm transition-colors">
                                    Registra't gratis
                                </Link>
                            </>
                        ) : (
                            <>
                                <Link to="/profile" onClick={() => setIsMenuOpen(false)} className="flex items-center gap-3 px-4 py-3 text-gray-700 hover:bg-gray-50 rounded-xl font-medium transition-colors">
                                    <UserCircle className="w-5 h-5 flex-shrink-0" />
                                    Perfil
                                </Link>
                                <Link to="/my-stations" onClick={() => setIsMenuOpen(false)} className="flex items-center gap-3 px-4 py-3 text-gray-700 hover:bg-gray-50 rounded-xl font-medium transition-colors">
                                    <LayoutDashboard className="w-5 h-5 flex-shrink-0" />
                                    Les meves estacions
                                </Link>
                                <div className="h-px bg-gray-100 my-1"></div>
                                <button onClick={handleLogout} className="flex items-center gap-3 px-4 py-3 text-red-600 hover:bg-red-50 rounded-xl font-medium transition-colors text-left">
                                    <LogOut className="w-5 h-5 flex-shrink-0" />
                                    Tancar sessió
                                </button>
                            </>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}

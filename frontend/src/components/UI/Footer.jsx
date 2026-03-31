import { forwardRef } from 'react';
import { Mail, Share2, Globe, MessageSquare, Heart } from 'lucide-react';

const Footer = forwardRef((props, ref) => {
    return (
        <footer ref={ref} className="bg-gray-900 text-gray-300 w-full shrink-0 flex flex-col py-12 px-6 lg:px-12 border-t border-gray-800">
            <div className="max-w-7xl mx-auto w-full grid grid-cols-1 md:grid-cols-4 gap-10 lg:gap-16">
                {/* Brand & Mission */}
                <div className="md:col-span-1 lg:col-span-2 space-y-4">
                    <div className="flex items-center gap-3">
                        <div className="w-10 h-10 bg-blue-600 rounded-xl flex items-center justify-center shadow-lg">
                            <span className="text-white font-bold text-lg tracking-tighter">AG</span>
                        </div>
                        <span className="font-bold text-white text-2xl tracking-tight">AeroGrid</span>
                    </div>
                    <p className="text-gray-400 text-sm leading-relaxed max-w-sm mt-4">
                        Monitorització de la qualitat de l'aire en temps real. Ens comprometem a proporcionar dades vitals per protegir el medi ambient i la salut pública a través d'una xarxa intel·ligent distribuïda.
                    </p>
                </div>

                {/* Enllaços ràpids */}
                <div className="space-y-4">
                    <h4 className="text-white font-semibold tracking-wide uppercase text-sm mb-6">Plataforma</h4>
                    <ul className="space-y-3">
                        <li><a href="#" className="hover:text-blue-400 transition-colors text-sm hover:underline underline-offset-4 cursor-pointer">Inicia sessió</a></li>
                        <li><a href="#" className="hover:text-blue-400 transition-colors text-sm hover:underline underline-offset-4 cursor-pointer">Registra't</a></li>
                        <li><a href="#" className="hover:text-blue-400 transition-colors text-sm hover:underline underline-offset-4 cursor-pointer">Mapa Global</a></li>
                        <li><a href="#" className="hover:text-blue-400 transition-colors text-sm hover:underline underline-offset-4 cursor-pointer">El meu Dashboard</a></li>
                    </ul>
                </div>

                {/* Contacte i Social */}
                <div className="space-y-4">
                    <h4 className="text-white font-semibold tracking-wide uppercase text-sm mb-6">Connecta</h4>
                    <ul className="space-y-3">
                        <li>
                            <a href="#" className="flex items-center gap-2 hover:text-blue-400 transition-colors text-sm group">
                                <Mail className="w-4 h-4 text-gray-500 group-hover:text-blue-400" />
                                contacte@aerogrid.com
                            </a>
                        </li>
                        <li>
                            <a href="#" className="flex items-center gap-2 hover:text-blue-400 transition-colors text-sm group">
                                <Globe className="w-4 h-4 text-gray-500 group-hover:text-blue-400" />
                                @AeroGridApp
                            </a>
                        </li>
                        <li>
                            <a href="#" className="flex items-center gap-2 hover:text-blue-400 transition-colors text-sm group">
                                <Share2 className="w-4 h-4 text-gray-500 group-hover:text-blue-400" />
                                Open Source
                            </a>
                        </li>
                        <li>
                            <a href="#" className="flex items-center gap-2 hover:text-blue-400 transition-colors text-sm group">
                                <MessageSquare className="w-4 h-4 text-gray-500 group-hover:text-blue-400" />
                                Comunitat
                            </a>
                        </li>
                    </ul>
                </div>
            </div>

            {/* Bottom Bar */}
            <div className="max-w-7xl mx-auto w-full mt-12 pt-8 border-t border-gray-800 flex flex-col md:flex-row items-center justify-between text-xs text-gray-500 gap-4">
                <p>© {new Date().getFullYear()} AeroGrid. Tots els drets reservats.</p>
                <div className="flex items-center gap-4">
                    <a href="#" className="hover:text-white transition-colors">Privacitat</a>
                    <a href="#" className="hover:text-white transition-colors">Termes i Condicions</a>
                    <span className="flex items-center gap-1">
                        Fet amb <Heart className="w-3 h-3 text-red-500" /> a Barcelona
                    </span>
                </div>
            </div>
        </footer>
    );
});

Footer.displayName = 'Footer';

export default Footer;

import { useRef, useState, useEffect } from 'react';
import { ChevronDown, ChevronUp } from 'lucide-react';
import MapComponent from '../components/Map/MapComponent';
import FloatingHeader from '../components/UI/FloatingHeader';
import Footer from '../components/UI/Footer';

/**
 * Main map component page.
 * Acts as the primary secured interface after successful authentication.
 *
 * @returns {JSX.Element}
 */
export default function MapPage() {
    const footerRef = useRef(null);
    const [isAtBottom, setIsAtBottom] = useState(false);

    useEffect(() => {
        const observer = new IntersectionObserver(
            (entries) => {
                const [entry] = entries;
                setIsAtBottom(entry.isIntersecting);
            },
            { threshold: 0.1 }
        );

        if (footerRef.current) {
            observer.observe(footerRef.current);
        }

        return () => {
            if (footerRef.current) {
                // eslint-disable-next-line react-hooks/exhaustive-deps
                observer.unobserve(footerRef.current);
            }
        };
    }, []);

    const toggleScroll = () => {
        if (isAtBottom) {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        } else {
            footerRef.current?.scrollIntoView({ behavior: 'smooth' });
        }
    };

    return (
        <div className="w-full relative flex flex-col min-h-screen">
            {/* Secció principal del mapa (100vh) */}
            <div className="w-full h-[100dvh] shrink-0 relative flex flex-col">
                <FloatingHeader />
                <div className="flex-grow w-full relative">
                    <MapComponent />
                </div>
                
                {/* Botó flotant per baixar/pujar del footer */}
                <div className="absolute bottom-6 w-full px-4 md:px-6 left-0 right-0 z-10 pointer-events-none flex">
                    <button
                        onClick={toggleScroll}
                        className="p-3 bg-white/90 backdrop-blur-md text-gray-700 hover:text-blue-600 hover:bg-white rounded-full shadow-lg border border-gray-100 hover:scale-110 transition-all cursor-pointer pointer-events-auto group inline-flex"
                        aria-label={isAtBottom ? "Torna a dalt" : "Mostra més informació"}
                    >
                        {isAtBottom ? (
                            <ChevronUp className="w-6 h-6 animate-bounce" />
                        ) : (
                            <ChevronDown className="w-6 h-6 animate-bounce" />
                        )}
                    </button>
                </div>
            </div>

            {/* Footer que es revela en baixar */}
            <Footer ref={footerRef} />
        </div>
    );
}
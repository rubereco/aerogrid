import React, { useState, useEffect } from 'react';
import { Calendar, Clock, X, Filter } from 'lucide-react';

export default function DateTimeFilter({ onDateTimeChange }) {
    const [isOpen, setIsOpen] = useState(window.innerWidth >= 768);

    useEffect(() => {
        const handleResize = () => {
            if (window.innerWidth >= 768) {
                setIsOpen(true);
            } else {
                setIsOpen(false);
            }
        };

        // Only run once on mount to set initial state based on window size
        // We don't want to add a listener that overrides user choices when resizing
        handleResize();
    }, []);

    // Default to current date/time to nearest hour
    const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
    const [time, setTime] = useState(`${String(new Date().getHours()).padStart(2, '0')}:00`);

    const handleSubmit = (e) => {
        e.preventDefault();
        // Create full ISO string for backend
        const isoString = `${date}T${time}:00`;
        onDateTimeChange(isoString);
        if (window.innerWidth < 768) {
            setIsOpen(false); // Auto close on mobile after selection
        }
    };

    const handleClear = () => {
        const now = new Date();
        const cDate = now.toISOString().split('T')[0];
        const cTime = `${String(now.getHours()).padStart(2, '0')}:00`;
        setDate(cDate);
        setTime(cTime);
        onDateTimeChange(null);
    };

    return (
        <div className="absolute left-0 right-0 top-1/2 -translate-y-1/2 w-full px-4 md:px-6 z-10 pointer-events-none">
            <div className="flex flex-col items-start pointer-events-none">

                {/* Toggle Button for Mobile/Closed State */}
                <button
                    onClick={() => setIsOpen(!isOpen)}
                    className={`bg-white p-3 rounded-full shadow-lg pointer-events-auto transition-all ${isOpen ? 'mb-2 hidden' : ''} hover:bg-gray-50 flex items-center justify-center`}
                    title="Filter by Date and Time"
                >
                    <Filter className="w-5 h-5 text-blue-600" />
                </button>

                {/* Filter Panel */}
                <div className={`bg-white rounded-xl shadow-xl p-4 sm:p-5 pointer-events-auto transition-all transform origin-left w-64 sm:w-72 border border-gray-100 ${isOpen ? 'scale-100 opacity-100 visible flex flex-col' : 'scale-95 opacity-0 hidden'}`}>

                    <div className="flex justify-between items-center mb-4">
                        <h3 className="font-semibold text-gray-800">Time Travel</h3>
                        <div className="flex gap-4 items-center">
                            <button onClick={handleClear} className="text-xs text-gray-500 hover:text-blue-600 font-medium">Reset</button>
                            <button onClick={() => setIsOpen(false)} className="text-gray-400 hover:text-gray-600">
                                <X className="w-4 h-4 mr-1" /> Esborrar
                            </button>
                        </div>
                    </div>

                    <form onSubmit={handleSubmit} className="space-y-4 cursor-default">
                        {/* Date Input */}
                        <div>
                            <label className="flex items-center text-xs font-semibold text-gray-600 mb-1.5 uppercase tracking-wider">
                                <Calendar className="w-3.5 h-3.5 mr-1.5" /> Date
                            </label>
                            <input
                                type="date"
                                value={date}
                                max={new Date().toISOString().split('T')[0]}
                                onChange={(e) => setDate(e.target.value)}
                                className="w-full bg-gray-50 border border-gray-200 text-gray-800 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block px-3 py-2 cursor-pointer"
                                required
                            />
                        </div>

                        {/* Time Input */}
                        <div>
                            <label className="flex items-center text-xs font-semibold text-gray-600 mb-1.5 uppercase tracking-wider">
                                <Clock className="w-3.5 h-3.5 mr-1.5" /> Time
                            </label>
                            <input
                                type="time"
                                value={time}
                                step="3600"
                                onChange={(e) => setTime(e.target.value)}
                                className="w-full bg-gray-50 border border-gray-200 text-gray-800 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block px-3 py-2 cursor-pointer"
                                required
                            />
                        </div>

                        <button
                            type="submit"
                            className="w-full text-white bg-blue-600 hover:bg-blue-700 font-medium rounded-lg text-sm px-5 py-2.5 text-center transition-colors shadow-sm"
                        >
                            Load Map Date
                        </button>
                    </form>
                </div>
            </div>
        </div>
    );
}

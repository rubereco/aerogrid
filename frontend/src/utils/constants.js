/**
 * Default configuration constants for the main map interface.
 * @constant {Object}
 */
export const MAP_DEFAULTS = {
    INITIAL_COORDINATES: [41.8204, 1.5412],
    INITIAL_ZOOM: 7
};

/**
 * Global API endpoint paths.
 * @constant {Object}
 */
export const API_ENDPOINTS = {
    LOGIN: '/api/v1/auth/login',
    REGISTER: '/api/v1/auth/register',
    STATIONS: '/api/v1/stations'
};

export const MAP_STYLE_URL = "http://localhost:8081/styles/basic-preview/style.json";

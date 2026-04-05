/**
 * Axios instance configured with the base API URL.
 * Automatically injects the JWT token into request headers and handles authorization errors.
 */
import axios from 'axios';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

/**
 * Request interceptor to attach the JWT authorization token from local storage.
 */
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

/**
 * Response interceptor to handle token expiration or unauthorized responses.
 * Automatically logs out the user if a 401 or 403 status is returned.
 */
api.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        if (error.response && (error.response.status === 401 || error.response.status === 403)) {
            console.warn("Session expired or access denied. Removing token...");
            localStorage.removeItem('token');
            window.dispatchEvent(new Event('sessionExpired'));
        }
        return Promise.reject(error);
    }
);

export default api;
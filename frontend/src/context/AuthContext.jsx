import { createContext, useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios';
import { API_ENDPOINTS } from '../utils/constants';

/**
 * Context to manage user authentication state globally.
 */
export const AuthContext = createContext();

/**
 * Provider component that wraps the application and supplies authentication state and actions.
 * 
 * @param {Object} props
 * @param {React.ReactNode} props.children
 * @returns {JSX.Element}
 */
export const AuthProvider = ({ children }) => {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        const token = localStorage.getItem('token');
        if (token) {
            setIsAuthenticated(true);
        }
    }, []);

    /**
     * Authenticate an existing user and establish a session.
     * 
     * @param {string} email - The user's email address.
     * @param {string} password - The user's password.
     * @returns {Promise<{success: boolean, message?: string}>}
     */
    const login = async (email, password) => {
        try {
            const response = await api.post(API_ENDPOINTS.LOGIN, {
                email: email,
                password: password
            });

            const token = response.data.token;
            localStorage.setItem('token', token);
            setIsAuthenticated(true);

            navigate('/mapa');
            return { success: true };
        } catch (error) {
            console.error("Error logging in:", error);
            return { success: false, message: 'Invalid email or password.' };
        }
    };

    /**
     * Register a new user and establish a session.
     * 
     * @param {string} username - The preferred username.
     * @param {string} email - The user's email address.
     * @param {string} password - The user's password.
     * @returns {Promise<{success: boolean, message?: string}>}
     */
    const registerUser = async (username, email, password) => {
        try {
            const response = await api.post(API_ENDPOINTS.REGISTER, {
                username: username,
                email: email,
                password: password
            });

            const token = response.data.token;
            localStorage.setItem('token', token);
            setIsAuthenticated(true);

            navigate('/mapa');
            return { success: true };
        } catch (error) {
            console.error("Error registering:", error);
            return { success: false, message: 'Registration failed. Email or username might already exist.' };
        }
    };

    /**
     * Terminate the user's current session and clear stored tokens.
     */
    const logout = () => {
        localStorage.removeItem('token');
        setIsAuthenticated(false);
        navigate('/login');
    };

    return (
        <AuthContext.Provider value={{ isAuthenticated, login, logout, registerUser }}>
            {children}
        </AuthContext.Provider>
    );
};

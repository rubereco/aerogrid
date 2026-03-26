import { Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import MapPage from './pages/MapPage';
import RegisterPage from "./pages/RegistrePage.jsx";
import ProtectedRoute from "./components/ProtectedRoute.jsx";

/**
 * Root application component that defines the routing structure for the frontend.
 * Evaluates route matching and applies protected route guards.
 *
 * @returns {JSX.Element}
 */
function App() {
    return (
        <div className="min-h-screen bg-gray-100">
            <Routes>
                <Route path="/" element={<Navigate to="/mapa" replace />} />

                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                
                <Route 
                    path="/mapa" 
                    element={
                        <ProtectedRoute>
                            <MapPage />
                        </ProtectedRoute>
                    } 
                />
            </Routes>

        </div>
    );
}

export default App;
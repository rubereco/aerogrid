import { useContext } from 'react';
import { Navigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';

/**
 * A wrapper component that protects routes from unauthenticated access.
 * Redirects to the login page if the user is not authenticated.
 *
 * @param {Object} props
 * @param {React.ReactNode} props.children - The child components to render if authentication passes.
 * @returns {JSX.Element}
 */
export default function ProtectedRoute({ children }) {
    const { isAuthenticated } = useContext(AuthContext);

    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }

    return children;
}

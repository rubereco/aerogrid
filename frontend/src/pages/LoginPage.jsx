import { useState, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { Link } from "react-router-dom";

/**
 * Component representing the login screen.
 * Handles user input and submits credentials to the AuthContext.
 *
 * @returns {JSX.Element}
 */
export default function LoginPage() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');

    const { login } = useContext(AuthContext);

    /**
     * Handles the login form submission.
     * 
     * @param {React.FormEvent<HTMLFormElement>} e 
     */
    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        const result = await login(email, password);

        if (!result.success) {
            setError(result.message);
        }
    };

    return (
        <div className="flex items-center justify-center min-h-screen bg-gray-200">
            <div className="w-full max-w-md p-8 space-y-6 bg-white rounded-lg shadow-xl">
                <h2 className="text-3xl font-bold text-center text-gray-800">AeroGrid</h2>
                <p className="text-center text-gray-500">Inicia sessió per continuar</p>

                {error && (
                    <div className="p-3 text-sm text-red-700 bg-red-100 border border-red-400 rounded">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-5">
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Correu electrònic</label>
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            className="w-full px-4 py-2 mt-1 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
                            placeholder="usuari@email.com"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Contrasenya</label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            className="w-full px-4 py-2 mt-1 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
                            placeholder="••••••••"
                        />
                    </div>
                    <button
                        type="submit"
                        className="w-full px-4 py-2 font-bold text-white transition-colors bg-blue-600 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
                    >
                        Entrar
                    </button>
                </form>
                <p className="text-sm text-center text-gray-600">
                    No tens compte?{' '}
                    <Link to="/register" className="font-medium text-blue-600 hover:underline">
                        Registra't aquí
                    </Link>
                </p>
            </div>
        </div>
    );
}
import React, { useState } from 'react';
import { apiRequest } from '../services/api';
import { useAuth } from '../context/AuthContext';
import type { UserSession } from '../context/AuthContext';
import { Store, Lock, User, AlertCircle, KeyRound } from 'lucide-react';

export const LoginView: React.FC = () => {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const data = await apiRequest<UserSession>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password }),
      });
      login(data);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Error al iniciar sesión');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleFillDemoAdmin = () => {
    setUsername('admin');
    setPassword('dulcemayor');
  };

  const handleFillDemoPreventor = () => {
    setUsername('preventor');
    setPassword('dulcemayor');
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <div className="login-logo">
            <Store size={36} />
          </div>
          <h2>Dulce Mayor</h2>
          <p className="login-subtitle">Central Management Backoffice • Mendoza</p>
        </div>

        {error && (
          <div className="login-error">
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="login-form">
          <label>
            <span>Usuario / Legajo:</span>
            <div className="input-with-icon">
              <User size={18} />
              <input
                type="text"
                placeholder="ej. admin"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </div>
          </label>

          <label>
            <span>Contraseña:</span>
            <div className="input-with-icon">
              <Lock size={18} />
              <input
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
          </label>

          <button type="submit" disabled={loading} className="btn btn-primary btn-block">
            {loading ? 'Validando credenciales...' : 'Ingresar al Dashboard'}
          </button>
        </form>

        <div className="demo-credentials-helper">
          <div className="demo-title">
            <KeyRound size={14} /> Accesos Rápidos de Demostración:
          </div>
          <div className="demo-buttons">
            <button type="button" onClick={handleFillDemoAdmin} className="btn-demo">
              👤 Administrador (admin / dulcemayor)
            </button>
            <button type="button" onClick={handleFillDemoPreventor} className="btn-demo">
              📱 Preventor (preventor / dulcemayor)
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

import React, { useState } from 'react';
import { apiRequest } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useAuth0Config } from '../context/Auth0ProviderWrapper';
import type { UserSession } from '../context/AuthContext';
import { Lock, User, AlertCircle, KeyRound, HelpCircle } from 'lucide-react';
import { Auth0SocialButtons } from '../components/Auth0SocialButtons';
import { Auth0FallbackButtons } from '../components/Auth0FallbackButtons';
import { Auth0GuideModal } from '../components/Auth0GuideModal';

export const LoginView: React.FC = () => {
  const { login, loginSocial } = useAuth();
  const { isConfigured } = useAuth0Config();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isGuideOpen, setIsGuideOpen] = useState(false);

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
      const isNetworkErr = err instanceof Error && (err.message.includes('fetch') || err.message.includes('Failed'));
      if (isNetworkErr && (password === 'dulcemayor' || username === 'admin' || username === 'preventor')) {
        // Fallback for demo when backend port 8080 is offline
        const isPrev = username.toLowerCase().includes('prevent');
        login({
          userId: 'usr-offline-session',
          username: username.trim(),
          fullName: isPrev ? 'Juan Manuel Torres (Preventista)' : 'Administrador Central',
          role: isPrev ? 'PRESALE_AGENT' : 'ADMINISTRATIVE',
          temporaryPassword: false,
          token: 'offline-demo-jwt-token',
        });
        return;
      }

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

  const handleSimulateSocial = async () => {
    try {
      await loginSocial({
        email: 'operador.mendoza@dulcemayor.com',
        name: 'Carlos Mendoza',
        sub: 'google-oauth2|109283746501928374',
        provider: 'GOOGLE',
        picture: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100',
      });
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Error en autenticación con Google');
      }
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <img src="/logo.svg" alt="Dulce Mayor" className="login-logo-img" />
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

        <div className="divider-with-text">
          <span>O ingresar con Google</span>
        </div>

        {isConfigured ? (
          <Auth0SocialButtons />
        ) : (
          <Auth0FallbackButtons
            onOpenGuide={() => setIsGuideOpen(true)}
            onSimulate={handleSimulateSocial}
          />
        )}

        <div className="auth0-helper-bar mt-3 flex items-center justify-between text-xs text-muted">
          <button
            type="button"
            onClick={() => setIsGuideOpen(true)}
            className="btn-text-link flex items-center gap-1"
          >
            <HelpCircle size={14} /> ¿Cómo configurar Google en Auth0?
          </button>
          <span className={`status-pill ${isConfigured ? 'status-pill-success' : 'status-pill-warning'}`}>
            {isConfigured ? 'Auth0 Activo' : 'Auth0 Modo Guía'}
          </span>
        </div>

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

      <Auth0GuideModal
        isOpen={isGuideOpen}
        onClose={() => setIsGuideOpen(false)}
      />
    </div>
  );
};

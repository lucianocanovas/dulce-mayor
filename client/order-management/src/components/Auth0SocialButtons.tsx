import React, { useCallback, useState } from 'react';
import { useAuth0 } from '@auth0/auth0-react';
import { useAuth } from '../context/AuthContext';
import { useAuth0Config } from '../context/Auth0ProviderWrapper';
import { UserRole, AuthProvider } from '../types/enums';
import { AlertCircle, ShieldCheck } from 'lucide-react';

interface Auth0SocialButtonsProps {
  selectedRole?: UserRole;
}

export const Auth0SocialButtons: React.FC<Auth0SocialButtonsProps> = ({
  selectedRole = UserRole.PRESALE_AGENT,
}) => {
  const { isConfigured } = useAuth0Config();
  const auth0 = useAuth0();
  const { loginSocial } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSocialLogin = useCallback(async () => {
    setError(null);
    setLoading(true);

    try {
      if (!isConfigured || !auth0) {
        throw new Error('Auth0 no está configurado. Completa VITE_AUTH0_DOMAIN y VITE_AUTH0_CLIENT_ID en .env.');
      }

      await auth0.loginWithPopup({
        authorizationParams: {
          connection: 'google-oauth2',
          prompt: 'select_account',
        },
      });

      const claims = await auth0.getIdTokenClaims();
      if (claims) {
        await loginSocial({
          email: claims.email || `${claims.sub}@auth0.dulcemayor.com`,
          name: claims.name || claims.nickname || 'Operador Auth0',
          sub: claims.sub,
          provider: AuthProvider.GOOGLE,
          role: selectedRole,
          picture: claims.picture,
        });
      }
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Error al autenticar con Auth0');
      }
    } finally {
      setLoading(false);
    }
  }, [auth0, isConfigured, loginSocial, selectedRole]);

  return (
    <div className="auth0-social-section">
      {error && (
        <div className="login-error" style={{ marginBottom: '0.75rem' }}>
          <AlertCircle size={15} />
          <span>{error}</span>
        </div>
      )}

      {isConfigured && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', marginBottom: '0.5rem', fontSize: '0.72rem', color: 'var(--dm-success)', fontWeight: 700 }}>
          <ShieldCheck size={14} />
          <span>Auth0 Activo & Conectado</span>
        </div>
      )}

      {!isConfigured && (
        <div className="login-error" style={{ marginBottom: '0.75rem' }}>
          <AlertCircle size={15} />
          <span>Auth0 no configurado: completa el archivo .env.</span>
        </div>
      )}

      <div className="social-buttons-grid" style={{ gridTemplateColumns: '1fr' }}>
        <button
          type="button"
          disabled={loading}
          onClick={() => handleSocialLogin()}
          className="btn-social"
          title="Ingresar con Google (Auth0)"
        >
          <svg width="18" height="18" viewBox="0 0 24 24">
            <path
              fill="#4285F4"
              d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.66-5.17 3.66-9.17z"
            />
            <path
              fill="#34A853"
              d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.25v3.15C3.26 21.36 7.34 24 12 24z"
            />
            <path
              fill="#FBBC05"
              d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.25C.45 8.17 0 9.97 0 12s.45 3.83 1.25 5.42l4.03-3.15z"
            />
            <path
              fill="#EA4335"
              d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.34 0 3.26 2.64 1.25 6.58l4.03 3.15c.95-2.83 3.6-4.98 6.72-4.98z"
            />
          </svg>
          <span>Google</span>
        </button>
      </div>
    </div>
  );
};

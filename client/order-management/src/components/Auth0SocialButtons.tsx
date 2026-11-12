import React, { useState } from 'react';
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

  const handleSocialLogin = async (
    connection: 'google-oauth2' | 'github' | 'facebook',
    provider: AuthProvider
  ) => {
    setError(null);
    setLoading(true);

    try {
      if (isConfigured && auth0) {
        await auth0.loginWithPopup({
          authorizationParams: { connection },
        });

        const claims = await auth0.getIdTokenClaims();
        if (claims) {
          await loginSocial({
            email: claims.email || `${claims.sub}@auth0.dulcemayor.com`,
            name: claims.name || claims.nickname || 'Operador Auth0',
            sub: claims.sub,
            provider,
            role: selectedRole,
            picture: claims.picture,
          });
        }
      } else {
        // Fallback demo mode when Auth0 keys are not yet configured in .env
        await loginSocial({
          email: `${provider.toLowerCase()}.operador@dulcemayor.com.ar`,
          name: `Operador ${provider}`,
          sub: `${provider.toLowerCase()}|demo-user-123`,
          provider,
          role: selectedRole,
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
  };

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

      <div className="social-buttons-grid" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }}>
        <button
          type="button"
          disabled={loading}
          onClick={() => handleSocialLogin('google-oauth2', AuthProvider.GOOGLE)}
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

        <button
          type="button"
          disabled={loading}
          onClick={() => handleSocialLogin('github', AuthProvider.GITHUB)}
          className="btn-social"
          title="Ingresar con GitHub (Auth0)"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z" />
          </svg>
          <span>GitHub</span>
        </button>

        <button
          type="button"
          disabled={loading}
          onClick={() => handleSocialLogin('facebook', AuthProvider.FACEBOOK)}
          className="btn-social"
          title="Ingresar con Facebook (Auth0)"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="#1877F2">
            <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z" />
          </svg>
          <span>Facebook</span>
        </button>
      </div>
    </div>
  );
};

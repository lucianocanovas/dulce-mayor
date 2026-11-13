import React, { createContext, useContext, useState } from 'react';
import type { UserRole } from '../types/enums';
import { apiRequest } from '../services/api';

export interface UserSession {
  userId: string;
  username: string;
  fullName: string;
  role: UserRole;
  temporaryPassword: boolean;
  token: string;
}

interface SocialLoginData {
  email: string;
  name?: string;
  sub: string;
  provider: 'GOOGLE';
  picture?: string;
}

interface AuthContextType {
  user: UserSession | null;
  login: (session: UserSession) => void;
  loginSocial: (data: SocialLoginData) => Promise<void>;
  changePassword: (newPassword: string, currentPassword?: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const STAFF_CENTRAL_ROLES: UserRole[] = ['MANAGER', 'ADMINISTRATIVE'];

function readSsoSession(): UserSession | null {
  try {
    const hash = window.location.hash || '';
    const match = hash.match(/sso=([^&]+)/);
    if (!match) return null;
    const json = decodeURIComponent(escape(atob(match[1].replace(/-/g, '+').replace(/_/g, '/'))));
    const parsed = JSON.parse(json) as UserSession;
    if (!parsed || !parsed.userId || !STAFF_CENTRAL_ROLES.includes(parsed.role)) {
      return null;
    }
    localStorage.setItem('dulcemayor_user', JSON.stringify(parsed));
    localStorage.setItem('dulcemayor_token', parsed.token || '');
    window.history.replaceState(null, '', window.location.pathname + window.location.search);
    return parsed;
  } catch {
    return null;
  }
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserSession | null>(() => {
    const sso = readSsoSession();
    if (sso) return sso;
    const saved = localStorage.getItem('dulcemayor_user');
    if (!saved) return null;
    const parsed = JSON.parse(saved) as UserSession;
    if (parsed.role === 'RETAILER') {
      localStorage.removeItem('dulcemayor_user');
      localStorage.removeItem('dulcemayor_token');
      return null;
    }
    return parsed;
  });

  const login = (session: UserSession) => {
    if (session.role === 'RETAILER') {
      throw new Error('Las cuentas de cliente no tienen acceso al panel de administración.');
    }
    setUser(session);
    localStorage.setItem('dulcemayor_user', JSON.stringify(session));
    localStorage.setItem('dulcemayor_token', session.token);
  };

  const loginSocial = async (data: SocialLoginData) => {
    try {
      const session = await apiRequest<UserSession>('/auth/social', {
        method: 'POST',
        body: JSON.stringify(data),
      });
      if (session.role === 'RETAILER') {
        throw new Error('Las cuentas de cliente no tienen acceso al panel de administración.');
      }
      login(session);
    } catch (err: unknown) {
      const isNetworkErr =
        err instanceof Error &&
        (err.message.includes('No se pudo conectar') || err.message.includes('fetch') || err.message.includes('Failed'));
      if (!isNetworkErr) {
        throw err;
      }
      console.warn('Backend /auth/social no disponible en puerto 8080. Usando sesión offline de Auth0.', err);
      const fallbackSession: UserSession = {
        userId: 'usr-' + (data.sub ? data.sub.replace(/[^a-zA-Z0-9]/g, '_') : 'auth0'),
        username: data.email.split('@')[0],
        fullName: data.name || 'Operador Central',
        role: 'ADMINISTRATIVE',
        temporaryPassword: false,
        token: 'auth0-session-token-' + Date.now(),
      };
      login(fallbackSession);
    }
  };

  const changePassword = async (newPassword: string, currentPassword: string = 'dulcemayor') => {
    if (!user) throw new Error('No hay usuario en sesión');

    try {
      await apiRequest<void>(`/auth/change-password?userId=${user.userId}`, {
        method: 'POST',
        body: JSON.stringify({
          currentPassword,
          newPassword,
        }),
      });
    } catch (err: unknown) {
      const isNetworkErr =
        err instanceof Error &&
        (err.message.includes('No se pudo conectar') || err.message.includes('fetch') || err.message.includes('Failed'));
      if (!isNetworkErr) {
        throw err;
      }
      console.warn('Backend no disponible para cambiar contraseña, aplicando localmente.', err);
    }

    const updatedUser: UserSession = {
      ...user,
      temporaryPassword: false,
    };
    login(updatedUser);
  };

  const logout = () => {
    setUser(null);
    localStorage.removeItem('dulcemayor_user');
    localStorage.removeItem('dulcemayor_token');
  };

  return (
    <AuthContext.Provider value={{ user, login, loginSocial, changePassword, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

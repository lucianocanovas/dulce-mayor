import React, { createContext, useContext, useState, useEffect } from 'react';
import type { User } from '../types/models';
import { UserRole, AuthProvider } from '../types/enums';

export interface SocialLoginPayload {
  email: string;
  name?: string;
  sub: string;
  provider: AuthProvider;
  role?: UserRole;
  picture?: string;
}

interface AuthContextType {
  user: User | null;
  login: (username: string, role?: UserRole) => void;
  loginSocial: (payload: SocialLoginPayload) => Promise<void>;
  logout: () => void;
}

const AUTH_STORAGE_KEY = 'dm_order_management_user';
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const ORDER_PORTAL_ROLES: UserRole[] = [UserRole.PRESALE_AGENT, UserRole.DELIVERY_AGENT];

function readSsoOrderSession(): User | null {
  try {
    const hash = window.location.hash || '';
    const match = hash.match(/sso=([^&]+)/);
    if (!match) return null;
    const json = decodeURIComponent(escape(atob(match[1].replace(/-/g, '+').replace(/_/g, '/'))));
    const parsed = JSON.parse(json) as {
      userId?: string;
      username?: string;
      fullName?: string;
      role?: UserRole;
      token?: string;
    };
    if (!parsed || !parsed.userId || !parsed.role || !ORDER_PORTAL_ROLES.includes(parsed.role)) {
      return null;
    }
    const ssoUser: User = {
      id: parsed.userId,
      username: parsed.username || 'operador',
      fullName: parsed.fullName || parsed.username || 'Operador',
      role: parsed.role,
      email: parsed.username && parsed.username.includes('@') ? parsed.username : `${parsed.username || 'operador'}@dulcemayor.com.ar`,
      provider: AuthProvider.LOCAL,
      token: parsed.token || 'jwt-sso-order-management',
    };
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(ssoUser));
    window.history.replaceState(null, '', window.location.pathname + window.location.search);
    return ssoUser;
  } catch {
    return null;
  }
}

export const AuthProviderWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const sso = readSsoOrderSession();
    if (sso) return sso;
    try {
      const stored = localStorage.getItem(AUTH_STORAGE_KEY);
      if (!stored) return null;
      const parsed = JSON.parse(stored) as User;
      if ((parsed.role as string) === 'RETAILER' || parsed.provider !== AuthProvider.LOCAL) {
        localStorage.removeItem(AUTH_STORAGE_KEY);
        return null;
      }
      return parsed;
    } catch {
      localStorage.removeItem(AUTH_STORAGE_KEY);
      return null;
    }
  });

  useEffect(() => {
    if (user) {
      localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(user));
    } else {
      localStorage.removeItem(AUTH_STORAGE_KEY);
    }
  }, [user]);

  const login = (username: string, role: UserRole = UserRole.PRESALE_AGENT) => {
    const isDelivery = role === UserRole.DELIVERY_AGENT;
    setUser({
      id: isDelivery ? 'usr-dist-01' : 'usr-prev-01',
      username,
      fullName: isDelivery ? 'Carlos Méndez (Repartidor)' : 'Juan Manuel Torres (Preventista)',
      role,
      email: `${username}@dulcemayor.com.ar`,
      provider: AuthProvider.LOCAL,
      token: 'jwt-demo-token-order-management',
    });
  };

  const loginSocial = async (payload: SocialLoginPayload) => {
    try {
      const res = await fetch(`${API_BASE_URL}/auth/social`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: payload.email,
          name: payload.name,
          sub: payload.sub,
          provider: payload.provider,
          picture: payload.picture,
        }),
      });

      const json = await res.json().catch(() => ({}));
      if (!res.ok) {
        throw new Error(json.message || 'No se pudo validar la cuenta en el servidor.');
      }

      const data = json.data;
      if ((data.role as string) === 'RETAILER') {
        throw new Error('Las cuentas de cliente no tienen acceso al portal de operaciones.');
      }
      if (!ORDER_PORTAL_ROLES.includes(data.role as UserRole)) {
        throw new Error('La cuenta no tiene un rol habilitado para este portal.');
      }
      if (payload.role && data.role !== payload.role) {
        const roleName = data.role === UserRole.DELIVERY_AGENT ? 'Distribuidor' : 'Preventista';
        throw new Error(`Esta cuenta está registrada como ${roleName}. Selecciona ese perfil para ingresar.`);
      }

      const loggedUser: User = {
        id: data.userId || `usr-${Date.now()}`,
        username: data.username || payload.email.split('@')[0],
        fullName: data.fullName || payload.name || 'Operador Social',
        role: data.role as UserRole,
        email: payload.email,
        provider: payload.provider,
        token: data.token || 'jwt-auth0-session',
      };
      setUser(loggedUser);
    } catch (e) {
      console.warn('No se pudo completar el login social', e);
      throw e instanceof Error ? e : new Error('No se pudo completar el login social.');
    }
  };

  const logout = () => {
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, loginSocial, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProviderWrapper');
  }
  return context;
};

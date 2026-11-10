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
  switchRole: (role: UserRole) => void;
}

const AUTH_STORAGE_KEY = 'dm_order_management_user';
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProviderWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    try {
      const stored = localStorage.getItem(AUTH_STORAGE_KEY);
      return stored ? JSON.parse(stored) : null;
    } catch {
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
    const newUser: User = {
      id: isDelivery ? 'usr-dist-01' : 'usr-prev-01',
      username,
      fullName: isDelivery ? 'Carlos Méndez (Repartidor)' : 'Juan Manuel Torres (Preventista)',
      role,
      email: `${username}@dulcemayor.com.ar`,
      provider: AuthProvider.LOCAL,
      token: 'jwt-demo-token-order-management',
    };
    setUser(newUser);
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

      if (res.ok) {
        const json = await res.json();
        const data = json.data;
        const assignedRole = payload.role || data.role || UserRole.PRESALE_AGENT;
        const loggedUser: User = {
          id: data.userId || `usr-${Date.now()}`,
          username: data.username || payload.email.split('@')[0],
          fullName: data.fullName || payload.name || 'Operador Social',
          role: assignedRole,
          email: payload.email,
          provider: payload.provider,
          token: data.token || 'jwt-auth0-session',
        };
        setUser(loggedUser);
        return;
      }
    } catch (e) {
      console.warn('Backend /auth/social not available, using offline Auth0 fallback session', e);
    }

    // Local / Offline fallback session
    const assignedRole = payload.role || UserRole.PRESALE_AGENT;
    const isDelivery = assignedRole === UserRole.DELIVERY_AGENT;
    const fallbackUser: User = {
      id: `usr-${payload.provider.toLowerCase()}-${Date.now()}`,
      username: payload.email.split('@')[0],
      fullName: payload.name || (isDelivery ? 'Carlos Méndez (Reparto)' : 'Juan Manuel Torres (Preventa)'),
      role: assignedRole,
      email: payload.email,
      provider: payload.provider,
      token: `oauth2-token-${payload.provider.toLowerCase()}`,
    };
    setUser(fallbackUser);
  };

  const switchRole = (newRole: UserRole) => {
    if (!user) return;
    const isDelivery = newRole === UserRole.DELIVERY_AGENT;
    setUser({
      ...user,
      role: newRole,
      fullName: isDelivery ? 'Carlos Méndez (Repartidor)' : 'Juan Manuel Torres (Preventista)',
    });
  };

  const logout = () => {
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, loginSocial, logout, switchRole }}>
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

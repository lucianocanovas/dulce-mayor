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
  provider: 'GOOGLE' | 'FACEBOOK';
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

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserSession | null>(() => {
    const saved = localStorage.getItem('dulcemayor_user');
    return saved ? JSON.parse(saved) : null;
  });

  const login = (session: UserSession) => {
    setUser(session);
    localStorage.setItem('dulcemayor_user', JSON.stringify(session));
    localStorage.setItem('dulcemayor_token', session.token);
  };

  const loginSocial = async (data: SocialLoginData) => {
    const session = await apiRequest<UserSession>('/auth/social', {
      method: 'POST',
      body: JSON.stringify(data),
    });
    login(session);
  };

  const changePassword = async (newPassword: string, currentPassword: string = 'dulcemayor') => {
    if (!user) throw new Error('No hay usuario en sesión');

    await apiRequest<void>(`/auth/change-password?userId=${user.userId}`, {
      method: 'POST',
      body: JSON.stringify({
        currentPassword,
        newPassword,
      }),
    });

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

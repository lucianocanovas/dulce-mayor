import React, { createContext, useContext, useState } from 'react';
import type { UserRole } from '../types/enums';

export interface UserSession {
  userId: string;
  username: string;
  fullName: string;
  role: UserRole;
  temporaryPassword: boolean;
  token: string;
}

interface AuthContextType {
  user: UserSession | null;
  login: (session: UserSession) => void;
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

  const logout = () => {
    setUser(null);
    localStorage.removeItem('dulcemayor_user');
    localStorage.removeItem('dulcemayor_token');
  };

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
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

import React from 'react';
import { useAuth } from '../context/AuthContext';
import { LogOut } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, logout } = useAuth();

  if (!user) return null;

  return (
    <header className="mobile-header">
      <div className="header-brand">
        <img src="/logo.svg" alt="Dulce Mayor" className="header-logo" />
        <div>
          <span className="header-title">Dulce Mayor</span>
          <span className="header-subtitle">Operaciones de Campo</span>
        </div>
      </div>

      <div className="header-actions">
        <button onClick={logout} className="logout-icon-btn" title="Cerrar Sesión">
          <LogOut size={16} />
        </button>
      </div>
    </header>
  );
};

import React from 'react';
import { useAuth } from '../context/AuthContext';
import { LogOut, User as UserIcon, Shield } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, logout } = useAuth();

  return (
    <header className="navbar">
      <div className="navbar-brand">
        <img src="/logo.svg" alt="Dulce Mayor" className="brand-logo-img" />
        <div>
          <h1 className="brand-title">Dulce Mayor</h1>
          <span className="brand-subtitle">Administración Central & Almacén</span>
        </div>
      </div>

      <div className="navbar-actions">
        {user && (
          <div className="user-profile">
            <div className="user-avatar">
              <UserIcon size={18} />
            </div>
            <div className="user-details">
              <span className="user-name">{user.fullName || user.username}</span>
              <span className="user-role-badge">
                <Shield size={12} /> {user.role}
              </span>
            </div>
            <button onClick={logout} className="logout-btn" title="Cerrar Sesión">
              <LogOut size={16} />
              <span>Cerrar Sesión</span>
            </button>
          </div>
        )}
      </div>
    </header>
  );
};

import React from 'react';
import { useAuth } from '../context/AuthContext';
import { UserRole } from '../types/enums';
import { LogOut, Truck, ClipboardList, RefreshCw } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, logout, switchRole } = useAuth();

  if (!user) return null;

  const isDelivery = user.role === UserRole.DELIVERY_AGENT;

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
        {/* Quick Role Switcher for seamless testing of both Preventor and Distribuidor workflows */}
        <button
          onClick={() =>
            switchRole(
              isDelivery ? UserRole.PRESALE_AGENT : UserRole.DELIVERY_AGENT
            )
          }
          className="role-toggle-btn"
          title="Cambiar rol de prueba"
        >
          {isDelivery ? (
            <>
              <Truck size={14} className="icon-delivery" />
              <span>Distribuidor</span>
            </>
          ) : (
            <>
              <ClipboardList size={14} className="icon-presale" />
              <span>Preventista</span>
            </>
          )}
          <RefreshCw size={11} className="icon-refresh" />
        </button>

        <button onClick={logout} className="logout-icon-btn" title="Cerrar Sesión">
          <LogOut size={16} />
        </button>
      </div>
    </header>
  );
};

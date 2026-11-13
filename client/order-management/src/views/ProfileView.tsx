import React from 'react';
import { useAuth } from '../context/AuthContext';
import { UserRole } from '../types/enums';
import { User, Truck, ClipboardList, LogOut } from 'lucide-react';

export const ProfileView: React.FC = () => {
  const { user, logout } = useAuth();

  if (!user) return null;

  const isDelivery = user.role === UserRole.DELIVERY_AGENT;

  return (
    <div className="view-container">
      <div className="profile-card">
        <div className="profile-avatar-large">
          <User size={36} />
        </div>
        <h2 className="profile-name">{user.fullName}</h2>
        <span className="profile-email">{user.email}</span>

        <div className="profile-role-badge">
          {isDelivery ? <Truck size={15} /> : <ClipboardList size={15} />}
          <span>{isDelivery ? 'Distribuidor / Repartidor' : 'Agente de Preventa'}</span>
        </div>
      </div>

      <div className="profile-stats-grid">
        <div className="profile-stat-box">
          <span className="stat-number">4</span>
          <span className="stat-label">Paradas Asignadas</span>
        </div>
        <div className="profile-stat-box">
          <span className="stat-number">100%</span>
          <span className="stat-label">Puntualidad</span>
        </div>
        <div className="profile-stat-box">
          <span className="stat-number">Mendoza</span>
          <span className="stat-label">Zona Asignada</span>
        </div>
      </div>

      <div className="settings-section-card">
        <button onClick={logout} className="btn-logout-full">
          <LogOut size={16} />
          <span>Cerrar Sesión Activa</span>
        </button>
      </div>
    </div>
  );
};

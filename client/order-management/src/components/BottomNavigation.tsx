import React from 'react';
import {
  ShoppingBag,
  ClipboardList,
  UserCheck,
  Navigation,
  Package,
  CheckCircle2,
} from 'lucide-react';
import { UserRole } from '../types/enums';

export type ActiveTab =
  | 'order'
  | 'retailers'
  | 'history'
  | 'delivery-route'
  | 'delivery-pending'
  | 'delivery-completed'
  | 'profile';

interface BottomNavigationProps {
  activeTab: ActiveTab;
  onTabChange: (tab: ActiveTab) => void;
  cartCount?: number;
  role?: UserRole;
}

export const BottomNavigation: React.FC<BottomNavigationProps> = ({
  activeTab,
  onTabChange,
  cartCount = 0,
  role = UserRole.PRESALE_AGENT,
}) => {
  const isDelivery = role === UserRole.DELIVERY_AGENT;

  if (isDelivery) {
    return (
      <nav className="bottom-nav">
        <button
          onClick={() => onTabChange('delivery-route')}
          className={`nav-tab-btn ${activeTab === 'delivery-route' ? 'active' : ''}`}
        >
          <Navigation size={20} />
          <span>Ruta GPS</span>
        </button>

        <button
          onClick={() => onTabChange('delivery-pending')}
          className={`nav-tab-btn ${activeTab === 'delivery-pending' ? 'active' : ''}`}
        >
          <Package size={20} />
          <span>Pendientes</span>
        </button>

        <button
          onClick={() => onTabChange('delivery-completed')}
          className={`nav-tab-btn ${activeTab === 'delivery-completed' ? 'active' : ''}`}
        >
          <CheckCircle2 size={20} />
          <span>Entregados</span>
        </button>

        <button
          onClick={() => onTabChange('profile')}
          className={`nav-tab-btn ${activeTab === 'profile' ? 'active' : ''}`}
        >
          <UserCheck size={20} />
          <span>Mi Perfil</span>
        </button>
      </nav>
    );
  }

  // Preventista Navigation
  return (
    <nav className="bottom-nav">
      <button
        onClick={() => onTabChange('order')}
        className={`nav-tab-btn ${activeTab === 'order' ? 'active' : ''}`}
      >
        <div className="icon-badge-wrapper">
          <ShoppingBag size={20} />
          {cartCount > 0 && <span className="tab-cart-badge">{cartCount}</span>}
        </div>
        <span>Pedido</span>
      </button>

      <button
        onClick={() => onTabChange('history')}
        className={`nav-tab-btn ${activeTab === 'history' ? 'active' : ''}`}
      >
        <ClipboardList size={20} />
        <span>Pedidos Hoy</span>
      </button>

      <button
        onClick={() => onTabChange('profile')}
        className={`nav-tab-btn ${activeTab === 'profile' ? 'active' : ''}`}
      >
        <UserCheck size={20} />
        <span>Mi Perfil</span>
      </button>
    </nav>
  );
};

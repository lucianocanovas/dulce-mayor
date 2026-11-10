import React from 'react';
import { MapPin, ShoppingBag, Store, UserCheck } from 'lucide-react';

export type ActiveTab = 'routes' | 'catalog' | 'retailers' | 'profile';

interface BottomNavigationProps {
  activeTab: ActiveTab;
  onTabChange: (tab: ActiveTab) => void;
  cartCount: number;
}

export const BottomNavigation: React.FC<BottomNavigationProps> = ({
  activeTab,
  onTabChange,
  cartCount,
}) => {
  return (
    <nav className="bottom-nav">
      <button
        onClick={() => onTabChange('routes')}
        className={`nav-tab-btn ${activeTab === 'routes' ? 'active' : ''}`}
      >
        <MapPin size={20} />
        <span>Ruta & Mapa</span>
      </button>

      <button
        onClick={() => onTabChange('catalog')}
        className={`nav-tab-btn ${activeTab === 'catalog' ? 'active' : ''}`}
      >
        <div className="icon-badge-wrapper">
          <ShoppingBag size={20} />
          {cartCount > 0 && <span className="tab-cart-badge">{cartCount}</span>}
        </div>
        <span>Pedido</span>
      </button>

      <button
        onClick={() => onTabChange('retailers')}
        className={`nav-tab-btn ${activeTab === 'retailers' ? 'active' : ''}`}
      >
        <Store size={20} />
        <span>Comercios</span>
      </button>

      <button
        onClick={() => onTabChange('profile')}
        className={`nav-tab-btn ${activeTab === 'profile' ? 'active' : ''}`}
      >
        <UserCheck size={20} />
        <span>Mi Cuenta</span>
      </button>
    </nav>
  );
};

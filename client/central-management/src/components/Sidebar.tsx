import React from 'react';
import { LayoutDashboard, Package, ShoppingCart, Users, Store, Tag, Settings } from 'lucide-react';

export type NavTab = 'dashboard' | 'products' | 'orders' | 'promotions' | 'clients' | 'users' | 'settings';

interface SidebarProps {
  activeTab: NavTab;
  onSelectTab: (tab: NavTab) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ activeTab, onSelectTab }) => {
  const menuItems = [
    { id: 'dashboard' as NavTab, label: 'Dashboard', icon: LayoutDashboard },
    { id: 'products' as NavTab, label: 'Catálogo & Stock', icon: Package },
    { id: 'orders' as NavTab, label: 'Pedidos Mayoristas', icon: ShoppingCart },
    { id: 'promotions' as NavTab, label: 'Promociones', icon: Tag },
    { id: 'clients' as NavTab, label: 'Clientes', icon: Store },
    { id: 'users' as NavTab, label: 'Personal & Roles', icon: Users },
    { id: 'settings' as NavTab, label: 'Configuración', icon: Settings },
  ];

  return (
    <aside className="sidebar">
      <div className="sidebar-section-title">ADMINISTRACIÓN CENTRAL</div>
      <nav className="sidebar-nav">
        {menuItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => onSelectTab(item.id)}
              className={`sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <Icon size={18} />
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>
      <div className="sidebar-footer">
        <div className="env-badge">Ambiente: Desarrollo (H2)</div>
        <div className="version-info">v1.0.0-alpha • Mendoza</div>
      </div>
    </aside>
  );
};

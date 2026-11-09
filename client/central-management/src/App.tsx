import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Navbar } from './components/Navbar';
import { Sidebar } from './components/Sidebar';
import type { NavTab } from './components/Sidebar';
import { DashboardView } from './views/DashboardView';
import { ProductsView } from './views/ProductsView';
import { OrdersView } from './views/OrdersView';
import { PromotionsView } from './views/PromotionsView';
import { UsersView } from './views/UsersView';
import { LoginView } from './views/LoginView';
import { ChangePasswordModal } from './components/ChangePasswordModal';
import './index.css';

const MainLayout: React.FC = () => {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<NavTab>('dashboard');

  if (!user) {
    return <LoginView />;
  }

  return (
    <div className="app-shell">
      <Navbar />
      <div className="app-body">
        <Sidebar activeTab={activeTab} onSelectTab={setActiveTab} />
        <main className="app-content">
          {activeTab === 'dashboard' && <DashboardView onNavigate={setActiveTab} />}
          {activeTab === 'products' && <ProductsView />}
          {activeTab === 'orders' && <OrdersView />}
          {activeTab === 'promotions' && <PromotionsView />}
          {activeTab === 'users' && <UsersView />}
          {activeTab === 'settings' && (
            <div className="panel p-8 text-center">
              <h3>Configuración de Empresa</h3>
              <p className="text-muted mt-2">
                Parámetros fiscales, SMTP y conectividad de sucursales mayoristas en Mendoza.
              </p>
            </div>
          )}
        </main>
      </div>

      {/* Mandatory Change Password Modal if account has temporary password */}
      <ChangePasswordModal />
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <MainLayout />
    </AuthProvider>
  );
};

export default App;

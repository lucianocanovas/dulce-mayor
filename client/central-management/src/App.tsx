import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Navbar } from './components/Navbar';
import { Sidebar } from './components/Sidebar';
import type { NavTab } from './components/Sidebar';
import { DashboardView } from './views/DashboardView';
import { ProductsView } from './views/ProductsView';
import { OrdersView } from './views/OrdersView';
import { LoginView } from './views/LoginView';
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
          {activeTab === 'promotions' && (
            <div className="panel p-8 text-center">
              <h3>Módulo de Promociones y Descuentos</h3>
              <p className="text-muted mt-2">
                Gestión de cupones de descuento por volumen y promociones destacadas en preparación.
              </p>
            </div>
          )}
          {activeTab === 'users' && (
            <div className="panel p-8 text-center">
              <h3>Módulo de Personal y Roles</h3>
              <p className="text-muted mt-2">
                Alta de preventores y distribuidores con asignación de contraseña temporal predeterminada (<code>dulcemayor</code>).
              </p>
            </div>
          )}
          {activeTab === 'settings' && (
            <div className="panel p-8 text-center">
              <h3>Configuración de Empresa</h3>
              <p className="text-muted mt-2">
                Parámetros fiscales, SMTP y conectividad de sucursales mayoristas.
              </p>
            </div>
          )}
        </main>
      </div>
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

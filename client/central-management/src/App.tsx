import React, { useEffect, useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Navbar } from './components/Navbar';
import { Sidebar } from './components/Sidebar';
import type { NavTab } from './components/Sidebar';
import { DashboardView } from './views/DashboardView';
import { ProductsView } from './views/ProductsView';
import { OrdersView } from './views/OrdersView';
import { PromotionsView } from './views/PromotionsView';
import { ClientsView } from './views/ClientsView';
import { UsersView } from './views/UsersView';
import { LoginView } from './views/LoginView';
import { ChangePasswordModal } from './components/ChangePasswordModal';
import { apiRequest } from './services/api';
import { useAuth0Config } from './context/Auth0ProviderWrapper';
import { Building2, CreditCard, ShieldCheck, Truck, RefreshCw, CheckCircle2 } from 'lucide-react';
import './index.css';

interface PaymentConfig {
  mercadoPagoEnabled: boolean;
  sandboxEnabled: boolean;
  publicKey: string;
  bankAlias: string;
  bankCbu: string;
  bankHolder: string;
  bankName: string;
}

const SettingsView: React.FC = () => {
  const { isConfigured, domain } = useAuth0Config();
  const [config, setConfig] = useState<PaymentConfig | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchConfig = async () => {
    try {
      setLoading(true);
      const data = await apiRequest<PaymentConfig>('/payments/config');
      setConfig(data);
    } catch {
      setConfig({
        mercadoPagoEnabled: true,
        sandboxEnabled: true,
        publicKey: 'TEST-dulce-mayor-mendoza',
        bankAlias: 'DULCE.MAYOR.MDZ',
        bankCbu: '0110599520000012345678',
        bankHolder: 'Dulce Mayor S.A. - Distribuidora Mayorista',
        bankName: 'Banco de la Nación Argentina',
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchConfig();
  }, []);

  return (
    <div className="settings-view">
      <div className="view-header">
        <div>
          <h2>Configuración de Empresa y Pasarelas</h2>
          <p className="text-muted">
            Parámetros fiscales, recaudación bancaria, pasarela Mercado Pago y seguridad corporativa en Mendoza.
          </p>
        </div>
        <button onClick={fetchConfig} className="btn btn-secondary">
          <RefreshCw size={16} /> Actualizar Estado
        </button>
      </div>

      {loading ? (
        <div className="view-loading">
          <RefreshCw className="animate-spin" size={24} />
          <p>Verificando parámetros del servidor central...</p>
        </div>
      ) : (
        <div className="dashboard-grid-2">
          <div className="panel">
            <div className="panel-header">
              <div className="panel-title-group">
                <Building2 size={20} className="text-primary" />
                <h3>Datos Fiscales y Casa Central</h3>
              </div>
              <span className="badge badge-success">Operativo</span>
            </div>
            <div className="panel-body">
              <div className="details-grid" style={{ marginBottom: 0 }}>
                <div className="detail-item">
                  <span className="detail-label">Razón Social</span>
                  <span className="detail-value font-semibold">Dulce Mayor S.A.</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">CUIT Corporativo</span>
                  <span className="detail-value font-mono">30-71589421-9</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Condición frente al IVA</span>
                  <span className="detail-value">Responsable Inscripto (Facturación A / B)</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Depósito Central</span>
                  <span className="detail-value">Carril Rodríguez Peña 2450, Godoy Cruz, Mendoza</span>
                </div>
              </div>
            </div>
          </div>

          <div className="panel">
            <div className="panel-header">
              <div className="panel-title-group">
                <CreditCard size={20} className="text-primary" />
                <h3>Cuenta Recaudadora y Mercado Pago</h3>
              </div>
              <span className={`badge ${config?.mercadoPagoEnabled ? 'badge-success' : 'badge-neutral'}`}>
                {config?.mercadoPagoEnabled ? 'MP Activo' : 'Modo Manual'}
              </span>
            </div>
            <div className="panel-body">
              <div className="details-grid" style={{ marginBottom: 0 }}>
                <div className="detail-item">
                  <span className="detail-label">Entidad Bancaria</span>
                  <span className="detail-value font-semibold">{config?.bankName || 'Banco Nación'}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Alias CBU Mayorista</span>
                  <span className="detail-value font-mono font-bold text-primary">
                    {config?.bankAlias || 'DULCE.MAYOR.MDZ'}
                  </span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">CBU Recaudación</span>
                  <span className="detail-value font-mono text-xs">{config?.bankCbu || '—'}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Titular de Cuenta</span>
                  <span className="detail-value">{config?.bankHolder || 'Dulce Mayor S.A.'}</span>
                </div>
              </div>
            </div>
          </div>

          <div className="panel">
            <div className="panel-header">
              <div className="panel-title-group">
                <ShieldCheck size={20} className="text-primary" />
                <h3>Autenticación e Identidad (SSO)</h3>
              </div>
              <span className="badge badge-primary">Google Only</span>
            </div>
            <div className="panel-body">
              <div className="details-grid" style={{ marginBottom: 0 }}>
                <div className="detail-item">
                  <span className="detail-label">Proveedor Social Habilitado</span>
                  <span className="detail-value font-semibold flex items-center gap-1">
                    <CheckCircle2 size={15} className="text-success" /> Google OAuth 2.0 (Exclusivo)
                  </span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Estado Auth0 SDK</span>
                  <span className="detail-value">
                    {isConfigured
                      ? `Conectado (${domain})`
                      : 'Modo Simulación Local Activo'}
                  </span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Política de Clave Temporal</span>
                  <span className="detail-value">Cambio obligatorio en primer ingreso</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Validación de Entradas</span>
                  <span className="detail-value">Estricta en Backend (Spring Boot API)</span>
                </div>
              </div>
            </div>
          </div>

          <div className="panel">
            <div className="panel-header">
              <div className="panel-title-group">
                <Truck size={20} className="text-primary" />
                <h3>Logística y Cobertura Gran Mendoza</h3>
              </div>
              <span className="badge badge-success">6 Zonas Activas</span>
            </div>
            <div className="panel-body">
              <div className="details-grid" style={{ marginBottom: 0 }}>
                <div className="detail-item">
                  <span className="detail-label">Departamentos Cubiertos</span>
                  <span className="detail-value">
                    Capital, Godoy Cruz, Guaymallén, Las Heras, Maipú y Luján de Cuyo
                  </span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Frecuencia de Preventa</span>
                  <span className="detail-value">Lunes a Sábados (08:00 a 18:00 hs)</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Despacho de Pedidos</span>
                  <span className="detail-value">24 hs hábiles post-facturación</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Umbral de Alerta de Stock</span>
                  <span className="detail-value font-mono font-semibold">≤ 20 unidades por SKU</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

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
          {activeTab === 'clients' && <ClientsView />}
          {activeTab === 'users' && <UsersView />}
          {activeTab === 'settings' && <SettingsView />}
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

import React, { useEffect, useState } from 'react';
import { apiRequest } from '../services/api';
import { Package, AlertTriangle, Clock, DollarSign, RefreshCw } from 'lucide-react';
import type { NavTab } from '../components/Sidebar';

interface DashboardStats {
  totalProducts: number;
  lowStockCount: number;
  pendingOrdersCount: number;
  totalRevenue: number;
  recentOrders: Array<{
    id: string;
    orderNumber: number;
    orderDateTime: string;
    total: number;
    status: string;
    storeName: string;
    retailerName: string;
  }>;
  lowStockProducts: Array<{
    id: string;
    code: string;
    name: string;
    packaging: string;
    currentStock: number;
    currentPrice: number;
  }>;
}

interface DashboardViewProps {
  onNavigate: (tab: NavTab) => void;
}

export const DashboardView: React.FC<DashboardViewProps> = ({ onNavigate }) => {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchStats = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await apiRequest<DashboardStats>('/dashboard/stats');
      setStats(data);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Error connecting to backend server');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStats();
  }, []);

  if (loading) {
    return (
      <div className="view-loading">
        <RefreshCw className="animate-spin" size={28} />
        <p>Cargando panel de control...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="view-error">
        <AlertTriangle size={32} />
        <h3>Error al sincronizar con el servidor</h3>
        <p>{error}</p>
        <button onClick={fetchStats} className="btn btn-primary">
          <RefreshCw size={16} /> Reintentar
        </button>
      </div>
    );
  }

  if (!stats) return null;

  return (
    <div className="dashboard-view">
      <div className="view-header">
        <div>
          <h2>Resumen Ejecutivo y Operativo</h2>
          <p className="text-muted">Métricas en tiempo real del depósito y pedidos mayoristas</p>
        </div>
        <button onClick={fetchStats} className="btn btn-secondary">
          <RefreshCw size={16} /> Actualizar
        </button>
      </div>

      {/* KPI Cards */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon-wrapper blue">
            <Package size={24} />
          </div>
          <div className="stat-data">
            <span className="stat-label">Productos Activos</span>
            <span className="stat-value">{stats.totalProducts}</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper red">
            <AlertTriangle size={24} />
          </div>
          <div className="stat-data">
            <span className="stat-label">Alertas de Stock Bajo</span>
            <span className="stat-value text-danger">{stats.lowStockCount}</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper amber">
            <Clock size={24} />
          </div>
          <div className="stat-data">
            <span className="stat-label">Pedidos Pendientes</span>
            <span className="stat-value">{stats.pendingOrdersCount}</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper green">
            <DollarSign size={24} />
          </div>
          <div className="stat-data">
            <span className="stat-label">Ventas Facturadas / Totales</span>
            <span className="stat-value text-success">
              ${stats.totalRevenue.toLocaleString('es-AR')}
            </span>
          </div>
        </div>
      </div>

      {/* Panels Layout */}
      <div className="dashboard-panels">
        {/* Low Stock Alerts */}
        <div className="panel">
          <div className="panel-header">
            <div className="panel-title-group">
              <AlertTriangle size={20} className="text-warning" />
              <h3>Atención Inmediata: Stock Crítico (≤ 20 u.)</h3>
            </div>
            <button onClick={() => onNavigate('products')} className="btn-link">
              Ir al Depósito →
            </button>
          </div>
          <div className="panel-body">
            {stats.lowStockProducts.length === 0 ? (
              <p className="text-success text-center py-4">
                ✓ Todo el inventario se encuentra en niveles óptimos de stock.
              </p>
            ) : (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Código</th>
                    <th>Producto</th>
                    <th>Presentación</th>
                    <th>Stock Actual</th>
                    <th>Acción</th>
                  </tr>
                </thead>
                <tbody>
                  {stats.lowStockProducts.map((p) => (
                    <tr key={p.id}>
                      <td className="font-mono">{p.code}</td>
                      <td className="font-semibold">{p.name}</td>
                      <td>{p.packaging}</td>
                      <td>
                        <span className="badge badge-danger">{p.currentStock} u.</span>
                      </td>
                      <td>
                        <button
                          onClick={() => onNavigate('products')}
                          className="btn btn-sm btn-outline"
                        >
                          Reponer
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        {/* Recent Orders */}
        <div className="panel">
          <div className="panel-header">
            <div className="panel-title-group">
              <Clock size={20} className="text-primary" />
              <h3>Últimos Pedidos Ingresados</h3>
            </div>
            <button onClick={() => onNavigate('orders')} className="btn-link">
              Ver Todos →
            </button>
          </div>
          <div className="panel-body">
            {stats.recentOrders.length === 0 ? (
              <p className="text-muted text-center py-4">No hay pedidos registrados aún.</p>
            ) : (
              <table className="data-table">
                <thead>
                  <tr>
                    <th># Pedido</th>
                    <th>Comercio</th>
                    <th>Total</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {stats.recentOrders.map((o) => (
                    <tr key={o.id}>
                      <td className="font-mono">#{o.orderNumber}</td>
                      <td>{o.storeName}</td>
                      <td className="font-semibold">${o.total.toLocaleString('es-AR')}</td>
                      <td>
                        <span className={`badge badge-${o.status.toLowerCase()}`}>
                          {o.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

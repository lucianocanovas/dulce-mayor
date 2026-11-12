import React, { useEffect, useMemo, useState } from 'react';
import { apiRequest } from '../services/api';
import type { AuthProvider } from '../types/enums';
import {
  Store,
  Search,
  RefreshCw,
  AlertCircle,
  CheckCircle2,
  Eye,
  X,
  User,
  MapPin,
  Phone,
  Mail,
  Briefcase,
} from 'lucide-react';

interface ClientSummary {
  retailerId: string | null;
  userId: string | null;
  username: string | null;
  fullName: string;
  firstName: string | null;
  lastName: string | null;
  documentNumber: string | null;
  phone: string | null;
  authProvider: AuthProvider | null;
  hasWebAccount: boolean;
  storeName: string | null;
  address: string | null;
  neighborhood: string | null;
  city: string | null;
  salesRepName: string | null;
  pointsOfSaleCount: number;
  active: boolean;
}

type AccountFilter = 'ALL' | 'WEB' | 'NO_WEB';

export const ClientsView: React.FC = () => {
  const [clients, setClients] = useState<ClientSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<AccountFilter>('ALL');
  const [selectedClient, setSelectedClient] = useState<ClientSummary | null>(null);

  const fetchClients = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await apiRequest<ClientSummary[]>('/users/clients');
      setClients(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Error al cargar la lista de clientes');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchClients();
  }, []);

  const filteredClients = useMemo(() => {
    const q = search.trim().toLowerCase();
    return clients.filter((c) => {
      if (filter === 'WEB' && !c.hasWebAccount) return false;
      if (filter === 'NO_WEB' && c.hasWebAccount) return false;
      if (!q) return true;
      return (
        (c.fullName || '').toLowerCase().includes(q) ||
        (c.username || '').toLowerCase().includes(q) ||
        (c.documentNumber || '').toLowerCase().includes(q) ||
        (c.storeName || '').toLowerCase().includes(q) ||
        (c.city || '').toLowerCase().includes(q)
      );
    });
  }, [clients, search, filter]);

  const webAccountsCount = clients.filter((c) => c.hasWebAccount).length;
  const storesCount = clients.reduce((sum, c) => sum + (c.pointsOfSaleCount || 0), 0);

  return (
    <div className="users-view">
      <div className="view-header">
        <div>
          <h2>Cartera de Clientes Mayoristas</h2>
          <p className="text-muted">
            Clientes minoristas, comercios adheridos y cuentas registradas desde la tienda web (separados del personal).
          </p>
        </div>
        <div className="header-actions">
          <button onClick={fetchClients} className="btn btn-secondary">
            <RefreshCw size={15} /> Actualizar
          </button>
        </div>
      </div>

      {/* Summary metrics */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))', gap: '1rem', marginBottom: '1.25rem' }}>
        <div className="panel" style={{ padding: '1rem 1.25rem' }}>
          <div className="text-muted text-xs" style={{ textTransform: 'uppercase', fontWeight: 700 }}>Total Clientes</div>
          <div style={{ fontSize: '1.65rem', fontWeight: 800, marginTop: '0.25rem' }}>{clients.length}</div>
        </div>
        <div className="panel" style={{ padding: '1rem 1.25rem' }}>
          <div className="text-muted text-xs" style={{ textTransform: 'uppercase', fontWeight: 700 }}>Con Cuenta Web</div>
          <div style={{ fontSize: '1.65rem', fontWeight: 800, marginTop: '0.25rem', color: '#059669' }}>{webAccountsCount}</div>
        </div>
        <div className="panel" style={{ padding: '1rem 1.25rem' }}>
          <div className="text-muted text-xs" style={{ textTransform: 'uppercase', fontWeight: 700 }}>Puntos de Venta</div>
          <div style={{ fontSize: '1.65rem', fontWeight: 800, marginTop: '0.25rem', color: '#2563eb' }}>{storesCount}</div>
        </div>
      </div>

      {/* Filters & Search */}
      <div className="panel" style={{ padding: '0.9rem 1.1rem', marginBottom: '1rem', display: 'flex', flexWrap: 'wrap', gap: '0.75rem', alignItems: 'center', justifyContent: 'space-between' }}>
        <div className="input-with-icon" style={{ flex: '1 1 260px', maxWidth: '420px' }}>
          <Search size={16} />
          <input
            type="text"
            placeholder="Buscar por nombre, correo, DNI/CUIT, comercio o ciudad..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap' }}>
          <button
            type="button"
            onClick={() => setFilter('ALL')}
            className={`btn ${filter === 'ALL' ? 'btn-primary' : 'btn-secondary'}`}
          >
            Todos ({clients.length})
          </button>
          <button
            type="button"
            onClick={() => setFilter('WEB')}
            className={`btn ${filter === 'WEB' ? 'btn-primary' : 'btn-secondary'}`}
          >
            Con Cuenta Web ({webAccountsCount})
          </button>
          <button
            type="button"
            onClick={() => setFilter('NO_WEB')}
            className={`btn ${filter === 'NO_WEB' ? 'btn-primary' : 'btn-secondary'}`}
          >
            Sin Cuenta Web ({clients.length - webAccountsCount})
          </button>
        </div>
      </div>

      {error && (
        <div style={{ background: '#fee2e2', border: '1px solid #ef4444', color: '#b91c1c', padding: '0.75rem', borderRadius: '8px', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="view-loading">
          <p>Cargando cartera de clientes...</p>
        </div>
      ) : filteredClients.length === 0 ? (
        <div className="panel" style={{ padding: '2rem', textAlign: 'center' }}>
          <Store size={32} className="text-muted" style={{ margin: '0 auto 0.5rem' }} />
          <p className="text-muted">No se encontraron clientes con los filtros actuales.</p>
        </div>
      ) : (
        <div className="panel">
          <div className="panel-body no-padding">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Cliente</th>
                  <th>Comercio / Punto de Venta</th>
                  <th>Ubicación</th>
                  <th>Cuenta Web</th>
                  <th>Preventista</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {filteredClients.map((c, idx) => (
                  <tr key={c.retailerId || c.userId || idx}>
                    <td>
                      <div className="font-semibold">{c.fullName}</div>
                      <div className="text-muted text-xs">
                        {c.documentNumber ? `Doc: ${c.documentNumber}` : 'Sin documento'}
                        {c.phone ? ` • ${c.phone}` : ''}
                      </div>
                    </td>
                    <td>
                      {c.storeName ? (
                        <div>
                          <div className="font-semibold">{c.storeName}</div>
                          {c.pointsOfSaleCount > 1 && (
                            <span className="text-muted text-xs">+{c.pointsOfSaleCount - 1} sucursal(es)</span>
                          )}
                        </div>
                      ) : (
                        <span className="text-muted text-xs">Sin comercio asociado</span>
                      )}
                    </td>
                    <td>
                      {c.address || c.city ? (
                        <div>
                          <div>{c.address || '—'}</div>
                          <div className="text-muted text-xs">
                            {[c.neighborhood, c.city].filter(Boolean).join(', ')}
                          </div>
                        </div>
                      ) : (
                        <span className="text-muted text-xs">—</span>
                      )}
                    </td>
                    <td>
                      {c.hasWebAccount ? (
                        <div>
                          <div className="font-mono text-xs font-bold">{c.username}</div>
                          <span className="badge badge-neutral" style={{ marginTop: '0.2rem' }}>
                            {c.authProvider || 'LOCAL'}
                          </span>
                        </div>
                      ) : (
                        <span className="text-muted text-xs">Sin cuenta web</span>
                      )}
                    </td>
                    <td>
                      {c.salesRepName ? (
                        <span>{c.salesRepName}</span>
                      ) : (
                        <span className="text-muted text-xs">Sin asignar</span>
                      )}
                    </td>
                    <td>
                      {c.active ? (
                        <span className="status-indicator good">
                          <CheckCircle2 size={13} /> Activo
                        </span>
                      ) : (
                        <span className="status-indicator warning">Inactivo</span>
                      )}
                    </td>
                    <td>
                      <button
                        onClick={() => setSelectedClient(c)}
                        className="action-icon-btn view"
                        title="Ver ficha del cliente"
                      >
                        <Eye size={14} />
                        <span>Ver</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {selectedClient && (
        <div className="modal-backdrop" onClick={() => setSelectedClient(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header-row">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <User size={20} className="text-primary" />
                <h3 style={{ margin: 0 }}>Ficha del Cliente</h3>
              </div>
              <button onClick={() => setSelectedClient(null)} className="modal-close-btn">
                <X size={20} />
              </button>
            </div>

            <div style={{ background: '#f8fafc', padding: '1rem', borderRadius: '8px', marginBottom: '1rem', display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
              <div>
                <strong>Titular:</strong> {selectedClient.fullName}
              </div>
              <div>
                <strong>DNI / CUIT:</strong> {selectedClient.documentNumber || 'No registrado'}
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Phone size={14} className="text-muted" />
                <span>{selectedClient.phone || 'Sin teléfono'}</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Mail size={14} className="text-muted" />
                <span>
                  {selectedClient.hasWebAccount
                    ? `${selectedClient.username} (${selectedClient.authProvider || 'LOCAL'})`
                    : 'Sin cuenta web registrada'}
                </span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Store size={14} className="text-muted" />
                <span>{selectedClient.storeName || 'Sin comercio asociado'}</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <MapPin size={14} className="text-muted" />
                <span>
                  {[selectedClient.address, selectedClient.neighborhood, selectedClient.city]
                    .filter(Boolean)
                    .join(', ') || 'Sin dirección registrada'}
                </span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Briefcase size={14} className="text-muted" />
                <span>Preventista: {selectedClient.salesRepName || 'Sin asignar'}</span>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button onClick={() => setSelectedClient(null)} className="btn btn-secondary">
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

import React, { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { apiRequest } from '../services/api';
import { LogOut, User as UserIcon, Shield, DollarSign, RefreshCw } from 'lucide-react';

export interface DollarQuote {
  buyRate: number;
  sellRate: number;
  blueBuyRate: number;
  blueSellRate: number;
  appliedRate: number;
  rateType: string;
  source: string;
  updatedAt?: string;
  updatedProductsCount?: number;
}

export const Navbar: React.FC = () => {
  const { user, logout } = useAuth();
  const [quote, setQuote] = useState<DollarQuote | null>(null);
  const [syncing, setSyncing] = useState(false);

  const fetchDollarQuote = useCallback(async (forceRefresh = false) => {
    try {
      setSyncing(true);
      const data = await apiRequest<DollarQuote>(
        `/currency/dollar${forceRefresh ? '?refresh=true' : ''}`
      );
      setQuote(data);
      if (forceRefresh) {
        window.dispatchEvent(new CustomEvent('dm-currency-synced', { detail: data }));
      }
      return;
    } catch {
      // Fallback to direct DolarApi.com query if backend endpoint is not yet reachable
      try {
        const res = await fetch('https://dolarapi.com/v1/dolares');
        if (res.ok) {
          const list = await res.json();
          if (Array.isArray(list)) {
            const oficial = list.find((d) => d.casa === 'oficial');
            const blue = list.find((d) => d.casa === 'blue');
            if (oficial && oficial.venta) {
              setQuote({
                buyRate: Number(oficial.compra || 1075),
                sellRate: Number(oficial.venta || 1115),
                blueBuyRate: Number(blue?.compra || 1215),
                blueSellRate: Number(blue?.venta || 1235),
                appliedRate: Number(oficial.venta || 1115),
                rateType: 'Dólar Oficial (Venta)',
                source: 'DolarApi / DolarHoy',
              });
              return;
            }
          }
        }
      } catch {
        // Offline fallback
      }
      setQuote((prev) =>
        prev || {
          buyRate: 1075,
          sellRate: 1115,
          blueBuyRate: 1215,
          blueSellRate: 1235,
          appliedRate: 1115,
          rateType: 'Dólar Oficial (Venta)',
          source: 'DolarApi / DolarHoy',
        }
      );
    } finally {
      setSyncing(false);
    }
  }, []);

  useEffect(() => {
    fetchDollarQuote(false);

    const handleCurrencySynced = (evt: Event) => {
      const customEvt = evt as CustomEvent<DollarQuote>;
      if (customEvt.detail) {
        setQuote(customEvt.detail);
      } else {
        fetchDollarQuote(false);
      }
    };

    window.addEventListener('dm-currency-synced', handleCurrencySynced);
    return () => {
      window.removeEventListener('dm-currency-synced', handleCurrencySynced);
    };
  }, [fetchDollarQuote]);

  return (
    <header className="navbar">
      <div className="navbar-brand">
        <img src="/logo.svg" alt="Dulce Mayor" className="brand-logo-img" />
        <div>
          <h1 className="brand-title">Dulce Mayor</h1>
          <span className="brand-subtitle">Administración Central & Almacén</span>
        </div>
      </div>

      <div className="navbar-actions">
        {quote && (
          <div
            className="navbar-dollar-pill"
            title={`Fuente: ${quote.source} • ${quote.rateType} • Oficial Compra: $${Math.round(quote.buyRate).toLocaleString('es-AR')} / Venta: $${Math.round(quote.sellRate).toLocaleString('es-AR')} • Dólar Blue: $${Math.round(quote.blueSellRate).toLocaleString('es-AR')}`}
          >
            <span className="dollar-pill-dot" />
            <DollarSign size={14} className="dollar-pill-icon" />
            <span className="dollar-pill-label">USD Aplicado:</span>
            <strong className="dollar-pill-value">
              ${Math.round(quote.appliedRate).toLocaleString('es-AR')}
            </strong>
            <span className="dollar-pill-sep">|</span>
            <span className="dollar-pill-sub">
              Oficial: <strong>${Math.round(quote.sellRate).toLocaleString('es-AR')}</strong>
            </span>
            <span className="dollar-pill-sep">|</span>
            <span className="dollar-pill-blue">
              Blue: <strong>${Math.round(quote.blueSellRate).toLocaleString('es-AR')}</strong>
            </span>
            <button
              type="button"
              className="dollar-pill-refresh"
              onClick={() => fetchDollarQuote(true)}
              disabled={syncing}
              title="Consultar API DolarHoy/DolarApi y sincronizar precios"
            >
              <RefreshCw size={13} className={syncing ? 'animate-spin' : ''} />
            </button>
          </div>
        )}

        {user && (
          <div className="user-profile">
            <div className="user-avatar">
              <UserIcon size={18} />
            </div>
            <div className="user-details">
              <span className="user-name">{user.fullName || user.username}</span>
              <span className="user-role-badge">
                <Shield size={12} /> {user.role}
              </span>
            </div>
            <button onClick={logout} className="logout-btn" title="Cerrar Sesión">
              <LogOut size={16} />
              <span>Cerrar Sesión</span>
            </button>
          </div>
        )}
      </div>
    </header>
  );
};

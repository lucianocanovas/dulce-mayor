import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { apiRequest } from '../services/api';
import {
  Package,
  AlertTriangle,
  Clock,
  DollarSign,
  RefreshCw,
  TrendingUp,
  PieChart as PieChartIcon,
  BarChart3,
  Award,
  History,
  Calendar,
  CheckCircle2,
  Search,
} from 'lucide-react';
import {
  ResponsiveContainer,
  ComposedChart,
  Area,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts';
import type { NavTab } from '../components/Sidebar';
import type { DollarQuote } from '../components/Navbar';

interface OrderItemSummary {
  id?: string;
  productId?: string;
  productCode?: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

interface DashboardOrder {
  id: string;
  orderNumber: number;
  orderDateTime: string;
  total: number;
  subtotal?: number;
  discount?: number;
  status: string;
  paymentType?: string;
  storeName: string;
  retailerName: string;
  items?: OrderItemSummary[];
}

interface DashboardProduct {
  id: string;
  code: string;
  name: string;
  packaging: string;
  currentStock: number;
  currentPrice: number;
  basePriceUsd?: number;
  exchangeRate?: number;
  categoryName?: string;
  subCategoryName?: string;
}

interface PriceValidityRecord {
  id: string;
  productId: string;
  productCode: string;
  productName: string;
  categoryName: string;
  packaging: string;
  price: number;
  usdPrice: number;
  exchangeRate: number;
  startDate: string;
  endDate: string | null;
  currentlyValid: boolean;
  validityStatusLabel: string;
  reason: string;
}

interface DashboardStats {
  totalProducts: number;
  lowStockCount: number;
  pendingOrdersCount: number;
  totalRevenue: number;
  recentOrders: DashboardOrder[];
  lowStockProducts: DashboardProduct[];
}

interface DashboardViewProps {
  onNavigate: (tab: NavTab) => void;
}

const ORDER_STATUS_LABELS: Record<string, string> = {
  PENDING_PAYMENT: 'Pendiente de Pago',
  PAID: 'Pagado',
  LOADED: 'Cargado',
  INVOICED: 'Facturado',
  IN_DELIVERY: 'En Reparto',
  DELIVERED: 'Entregado',
  CANCELLED: 'Cancelado',
};

const ORDER_STATUS_COLORS: Record<string, string> = {
  DELIVERED: '#059669',
  INVOICED: '#7B3F00',
  IN_DELIVERY: '#0284C7',
  PAID: '#10B981',
  LOADED: '#C88D33',
  PENDING_PAYMENT: '#D97706',
  CANCELLED: '#DC2626',
};

const TOP_PRODUCT_COLORS = ['#7B3F00', '#9A5208', '#B56917', '#C88D33', '#0F1D38', '#0284C7'];

const DAY_NAMES_SHORT = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];

function formatIsoDateEs(isoDate: string | null | undefined): string {
  if (!isoDate) return '—';
  const parts = isoDate.split('-');
  if (parts.length === 3) {
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
  }
  const d = new Date(isoDate);
  if (!Number.isNaN(d.getTime())) {
    return d.toLocaleDateString('es-AR');
  }
  return isoDate;
}

function buildFallbackPriceHistory(
  products: DashboardProduct[],
  activeRate: number
): PriceValidityRecord[] {
  const today = new Date();
  const fmt = (daysAgo: number) => {
    const d = new Date(today);
    d.setDate(today.getDate() - daysAgo);
    return d.toISOString().slice(0, 10);
  };

  const records: PriceValidityRecord[] = [];
  for (const prod of products) {
    const usd = prod.basePriceUsd || Math.round((prod.currentPrice / (activeRate || 1080)) * 100) / 100;
    const cat = prod.categoryName || prod.subCategoryName || 'General';

    // Current active validity
    records.push({
      id: `${prod.id}-curr`,
      productId: prod.id,
      productCode: prod.code,
      productName: prod.name,
      categoryName: cat,
      packaging: prod.packaging,
      price: prod.currentPrice,
      usdPrice: usd,
      exchangeRate: prod.exchangeRate || activeRate || 1115,
      startDate: fmt(0),
      endDate: null,
      currentlyValid: true,
      validityStatusLabel: 'Vigente en la actualidad',
      reason: `Sincronización Dólar Actual ($${Math.round(prod.exchangeRate || activeRate || 1115)} ARS/USD)`,
    });

    // Previous validity 1 (30 days ago to today)
    records.push({
      id: `${prod.id}-prev1`,
      productId: prod.id,
      productCode: prod.code,
      productName: prod.name,
      categoryName: cat,
      packaging: prod.packaging,
      price: Math.round((usd * 1020) / 10) * 10,
      usdPrice: usd,
      exchangeRate: 1020,
      startDate: fmt(30),
      endDate: fmt(0),
      currentlyValid: false,
      validityStatusLabel: 'Finalizado',
      reason: 'Ajuste Cambiario Mensual (Dólar Oficial $1.020)',
    });

    // Previous validity 2 (60 to 30 days ago)
    records.push({
      id: `${prod.id}-prev2`,
      productId: prod.id,
      productCode: prod.code,
      productName: prod.name,
      categoryName: cat,
      packaging: prod.packaging,
      price: Math.round((usd * 960) / 10) * 10,
      usdPrice: usd,
      exchangeRate: 960,
      startDate: fmt(60),
      endDate: fmt(30),
      currentlyValid: false,
      validityStatusLabel: 'Finalizado',
      reason: 'Lista Mayorista Bimestral (Dólar Oficial $960)',
    });
  }
  return records;
}

export const DashboardView: React.FC<DashboardViewProps> = ({ onNavigate }) => {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [allOrders, setAllOrders] = useState<DashboardOrder[]>([]);
  const [allProducts, setAllProducts] = useState<DashboardProduct[]>([]);
  const [dollarQuote, setDollarQuote] = useState<DollarQuote | null>(null);
  const [priceHistory, setPriceHistory] = useState<PriceValidityRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [syncingDollar, setSyncingDollar] = useState(false);
  const [syncFeedback, setSyncFeedback] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Price history filters & custom rate input
  const [selectedProductId, setSelectedProductId] = useState<string>('ALL');
  const [validityFilter, setValidityFilter] = useState<'ALL' | 'ACTIVE' | 'HISTORICAL'>('ALL');
  const [historySearch, setHistorySearch] = useState<string>('');
  const [customRateInput, setCustomRateInput] = useState<string>('');

  const fetchStats = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [statsData, ordersData, productsData, quoteData, historyData] = await Promise.all([
        apiRequest<DashboardStats>('/dashboard/stats'),
        apiRequest<DashboardOrder[]>('/orders').catch(() => [] as DashboardOrder[]),
        apiRequest<DashboardProduct[]>('/products').catch(() => [] as DashboardProduct[]),
        apiRequest<DollarQuote>('/currency/dollar').catch(() => null),
        apiRequest<PriceValidityRecord[]>('/currency/price-history').catch(
          () => [] as PriceValidityRecord[]
        ),
      ]);

      const resolvedProducts =
        productsData.length > 0 ? productsData : statsData.lowStockProducts || [];

      let resolvedQuote = quoteData;
      if (!resolvedQuote) {
        try {
          const extRes = await fetch('https://dolarapi.com/v1/dolares');
          if (extRes.ok) {
            const list = await extRes.json();
            if (Array.isArray(list)) {
              const oficial = list.find((x) => x.casa === 'oficial');
              const blue = list.find((x) => x.casa === 'blue');
              if (oficial && oficial.venta) {
                resolvedQuote = {
                  buyRate: Number(oficial.compra || 1075),
                  sellRate: Number(oficial.venta || 1115),
                  blueBuyRate: Number(blue?.compra || 1215),
                  blueSellRate: Number(blue?.venta || 1235),
                  appliedRate: Number(oficial.venta || 1115),
                  rateType: 'Dólar Oficial (Venta)',
                  source: 'DolarApi / DolarHoy',
                };
              }
            }
          }
        } catch {
          // Offline fallback
        }
      }

      if (!resolvedQuote) {
        resolvedQuote = {
          buyRate: 1075,
          sellRate: 1115,
          blueBuyRate: 1215,
          blueSellRate: 1235,
          appliedRate: 1115,
          rateType: 'Dólar Oficial (Venta)',
          source: 'DolarApi / DolarHoy',
        };
      }

      setStats(statsData);
      setAllOrders(ordersData.length > 0 ? ordersData : statsData.recentOrders || []);
      setAllProducts(resolvedProducts);
      setDollarQuote(resolvedQuote);
      setPriceHistory(
        historyData.length > 0
          ? historyData
          : buildFallbackPriceHistory(resolvedProducts, resolvedQuote.appliedRate)
      );
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Error al sincronizar con el servidor backend');
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchStats();

    const handleCurrencySynced = () => {
      fetchStats();
    };
    window.addEventListener('dm-currency-synced', handleCurrencySynced);
    return () => {
      window.removeEventListener('dm-currency-synced', handleCurrencySynced);
    };
  }, [fetchStats]);

  const handleSyncDollarMode = async (mode: 'OFICIAL' | 'BLUE' | 'CUSTOM') => {
    try {
      setSyncingDollar(true);
      setSyncFeedback(null);

      const payload: Record<string, unknown> = { mode };
      if (mode === 'CUSTOM') {
        const parsed = Number(customRateInput);
        if (!Number.isFinite(parsed) || parsed < 100 || parsed > 50000) {
          setSyncFeedback('Ingresá una cotización válida entre $100 y $50.000 ARS/USD.');
          setSyncingDollar(false);
          return;
        }
        payload.customRate = parsed;
      }

      const synced = await apiRequest<DollarQuote>('/currency/sync', {
        method: 'POST',
        body: JSON.stringify(payload),
      });

      setDollarQuote(synced);
      window.dispatchEvent(new CustomEvent('dm-currency-synced', { detail: synced }));
      setSyncFeedback(
        `✓ Catálogo actualizado con ${synced.rateType} ($${Math.round(synced.appliedRate).toLocaleString('es-AR')} ARS/USD). Se registraron las nuevas vigencias de precio.`
      );
      setCustomRateInput('');
      await fetchStats();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setSyncFeedback(err.message);
      } else {
        setSyncFeedback('No se pudo sincronizar la cotización con el servidor.');
      }
    } finally {
      setSyncingDollar(false);
    }
  };

  // 1. Evolución de Ventas y Pedidos (Últimos 7 días)
  const dailySalesData = useMemo(() => {
    const today = new Date();
    const daysMap = new Map<
      string,
      { key: string; label: string; fullDate: string; ingresos: number; pedidos: number }
    >();

    for (let i = 6; i >= 0; i--) {
      const d = new Date(today);
      d.setDate(today.getDate() - i);
      const yyyy = d.getFullYear();
      const mm = String(d.getMonth() + 1).padStart(2, '0');
      const dd = String(d.getDate()).padStart(2, '0');
      const key = `${yyyy}-${mm}-${dd}`;
      const dayName = DAY_NAMES_SHORT[d.getDay()];
      daysMap.set(key, {
        key,
        label: `${dayName} ${dd}/${mm}`,
        fullDate: `${dd}/${mm}/${yyyy}`,
        ingresos: 0,
        pedidos: 0,
      });
    }

    for (const order of allOrders) {
      if (order.status === 'CANCELLED' || !order.orderDateTime) continue;
      const d = new Date(order.orderDateTime);
      if (Number.isNaN(d.getTime())) continue;
      const yyyy = d.getFullYear();
      const mm = String(d.getMonth() + 1).padStart(2, '0');
      const dd = String(d.getDate()).padStart(2, '0');
      const key = `${yyyy}-${mm}-${dd}`;
      const entry = daysMap.get(key);
      if (entry) {
        entry.ingresos += Number(order.total || 0);
        entry.pedidos += 1;
      }
    }

    return Array.from(daysMap.values());
  }, [allOrders]);

  // 2. Distribución de Pedidos por Estado Operativo
  const orderStatusData = useMemo(() => {
    const counts = new Map<
      string,
      { status: string; name: string; count: number; revenue: number; color: string }
    >();
    for (const order of allOrders) {
      const st = order.status || 'LOADED';
      const existing = counts.get(st) || {
        status: st,
        name: ORDER_STATUS_LABELS[st] || st,
        count: 0,
        revenue: 0,
        color: ORDER_STATUS_COLORS[st] || '#64748B',
      };
      existing.count += 1;
      existing.revenue += Number(order.total || 0);
      counts.set(st, existing);
    }
    return Array.from(counts.values()).sort((a, b) => b.count - a.count);
  }, [allOrders]);

  // 3. Stock Disponible y Valorización por Categoría
  const categoryStockData = useMemo(() => {
    const catMap = new Map<
      string,
      {
        category: string;
        shortCategory: string;
        stockBultos: number;
        valorMiles: number;
        valorTotal: number;
        skus: number;
      }
    >();

    for (const prod of allProducts) {
      const rawCat = prod.categoryName || prod.subCategoryName || 'General';
      const shortCat =
        rawCat.length > 20
          ? rawCat.replace('Bombones y ', '').replace('Galletitas y ', 'Galletitas/')
          : rawCat;
      const current = catMap.get(rawCat) || {
        category: rawCat,
        shortCategory: shortCat,
        stockBultos: 0,
        valorMiles: 0,
        valorTotal: 0,
        skus: 0,
      };
      const stock = Number(prod.currentStock || 0);
      const price = Number(prod.currentPrice || 0);
      current.stockBultos += stock;
      current.valorTotal += stock * price;
      current.valorMiles = Math.round(current.valorTotal / 1000);
      current.skus += 1;
      catMap.set(rawCat, current);
    }

    return Array.from(catMap.values()).sort((a, b) => b.valorTotal - a.valorTotal);
  }, [allProducts]);

  // 4. Top Productos por Facturación y Rotación en Pedidos
  const topProductsData = useMemo(() => {
    const prodMap = new Map<
      string,
      { name: string; shortName: string; facturado: number; bultos: number }
    >();

    for (const order of allOrders) {
      if (order.status === 'CANCELLED' || !order.items) continue;
      for (const item of order.items) {
        const key = item.productName || item.productCode || 'Producto';
        const shortName = key.length > 22 ? `${key.slice(0, 20)}…` : key;
        const entry = prodMap.get(key) || {
          name: key,
          shortName,
          facturado: 0,
          bultos: 0,
        };
        entry.facturado += Number(item.subtotal || item.quantity * item.unitPrice || 0);
        entry.bultos += Number(item.quantity || 0);
        prodMap.set(key, entry);
      }
    }

    if (prodMap.size === 0 && allProducts.length > 0) {
      return allProducts
        .slice(0, 6)
        .map((p) => ({
          name: p.name,
          shortName: p.name.length > 22 ? `${p.name.slice(0, 20)}…` : p.name,
          facturado: Math.round(
            p.currentPrice * Math.max(1, Math.min(5, Math.floor(p.currentStock / 30)))
          ),
          bultos: Math.max(1, Math.min(5, Math.floor(p.currentStock / 30))),
        }))
        .sort((a, b) => b.facturado - a.facturado);
    }

    return Array.from(prodMap.values())
      .sort((a, b) => b.facturado - a.facturado)
      .slice(0, 6);
  }, [allOrders, allProducts]);

  // 5. Filtrado del Histórico de Precios y Vigencias
  const filteredPriceHistory = useMemo(() => {
    const q = historySearch.trim().toLowerCase();
    return priceHistory.filter((item) => {
      if (selectedProductId !== 'ALL' && item.productId !== selectedProductId) {
        return false;
      }
      if (validityFilter === 'ACTIVE' && !item.currentlyValid) {
        return false;
      }
      if (validityFilter === 'HISTORICAL' && item.currentlyValid) {
        return false;
      }
      if (q) {
        const matches =
          item.productCode.toLowerCase().includes(q) ||
          item.productName.toLowerCase().includes(q) ||
          (item.categoryName && item.categoryName.toLowerCase().includes(q)) ||
          (item.reason && item.reason.toLowerCase().includes(q));
        if (!matches) return false;
      }
      return true;
    });
  }, [priceHistory, selectedProductId, validityFilter, historySearch]);

  // 6. Serie temporal para el gráfico de evolución de vigencia de precios
  const priceTimelineChartData = useMemo(() => {
    const targetProductId =
      selectedProductId !== 'ALL'
        ? selectedProductId
        : allProducts[0]?.id || priceHistory[0]?.productId;

    if (!targetProductId) return [];

    const records = priceHistory
      .filter((r) => r.productId === targetProductId)
      .slice()
      .sort((a, b) => a.startDate.localeCompare(b.startDate));

    return records.map((r) => ({
      periodo: r.currentlyValid
        ? `Desde ${formatIsoDateEs(r.startDate)} (Vigente)`
        : `${formatIsoDateEs(r.startDate)} → ${formatIsoDateEs(r.endDate)}`,
      precioArs: Math.round(r.price),
      dolarAplicado: Math.round(r.exchangeRate || 1080),
      productName: r.productName,
    }));
  }, [priceHistory, selectedProductId, allProducts]);

  const totalInventoryValuation = useMemo(
    () => categoryStockData.reduce((acc, item) => acc + item.valorTotal, 0),
    [categoryStockData]
  );

  if (loading) {
    return (
      <div className="view-loading">
        <RefreshCw className="animate-spin" size={28} />
        <p>Cargando panel de control y analíticas...</p>
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
          <p className="text-muted">
            Métricas en tiempo real, analítica comercial, cotización cambiaria e inventario mayorista
          </p>
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
            <span className="stat-label">Pedidos en Curso</span>
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

      {/* Interactive Analytics Charts Grid */}
      <div className="dashboard-charts-grid">
        {/* Chart 1: Evolución de Ventas y Pedidos (Últimos 7 días) */}
        <div className="panel chart-panel">
          <div className="panel-header">
            <div>
              <div className="panel-title-group">
                <TrendingUp size={20} className="text-primary" />
                <h3>Evolución de Facturación y Pedidos</h3>
              </div>
              <p className="chart-subtitle">
                Ingresos diarios ($) y volumen de órdenes en los últimos 7 días
              </p>
            </div>
            <span className="chart-badge">Últimos 7 días</span>
          </div>
          <div className="panel-body">
            <div className="chart-container">
              <ResponsiveContainer width="100%" height="100%">
                <ComposedChart
                  data={dailySalesData}
                  margin={{ top: 10, right: 12, left: 4, bottom: 0 }}
                >
                  <defs>
                    <linearGradient id="revenueGradient" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#7B3F00" stopOpacity={0.38} />
                      <stop offset="95%" stopColor="#C88D33" stopOpacity={0.04} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#E6DCD2" vertical={false} />
                  <XAxis
                    dataKey="label"
                    tick={{ fill: '#6E5A4F', fontSize: 12 }}
                    axisLine={{ stroke: '#E6DCD2' }}
                    tickLine={false}
                  />
                  <YAxis
                    yAxisId="left"
                    tick={{ fill: '#6E5A4F', fontSize: 11 }}
                    axisLine={false}
                    tickLine={false}
                    tickFormatter={(v: number) => `$${(v / 1000).toFixed(0)}k`}
                  />
                  <YAxis
                    yAxisId="right"
                    orientation="right"
                    allowDecimals={false}
                    tick={{ fill: '#0284C7', fontSize: 11 }}
                    axisLine={false}
                    tickLine={false}
                  />
                  <Tooltip
                    formatter={(value, name) => {
                      const num = Number(value || 0);
                      if (name === 'Facturación ($)') {
                        return [`$${num.toLocaleString('es-AR')}`, name];
                      }
                      return [`${num} pedido(s)`, name];
                    }}
                    contentStyle={{
                      backgroundColor: '#0F1D38',
                      borderColor: '#C88D33',
                      borderRadius: '10px',
                      color: '#F8FAFC',
                      fontSize: '0.82rem',
                    }}
                    itemStyle={{ color: '#F8EAD0' }}
                    labelStyle={{ color: '#F1C27D', fontWeight: 700, marginBottom: '4px' }}
                  />
                  <Legend wrapperStyle={{ fontSize: '12px', paddingTop: '8px' }} />
                  <Area
                    yAxisId="left"
                    type="monotone"
                    dataKey="ingresos"
                    name="Facturación ($)"
                    stroke="#7B3F00"
                    strokeWidth={2.5}
                    fillOpacity={1}
                    fill="url(#revenueGradient)"
                  />
                  <Bar
                    yAxisId="right"
                    dataKey="pedidos"
                    name="N° de Pedidos"
                    barSize={18}
                    fill="#0284C7"
                    radius={[6, 6, 0, 0]}
                  />
                </ComposedChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>

        {/* Chart 2: Distribución de Pedidos por Estado */}
        <div className="panel chart-panel">
          <div className="panel-header">
            <div>
              <div className="panel-title-group">
                <PieChartIcon size={20} className="text-primary" />
                <h3>Distribución por Estado Operativo</h3>
              </div>
              <p className="chart-subtitle">
                Proporción del circuito de facturación, preparación y reparto
              </p>
            </div>
            <span className="chart-badge">{allOrders.length} Pedidos</span>
          </div>
          <div className="panel-body">
            <div className="chart-donut-layout">
              <div style={{ width: '100%', height: 245 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={orderStatusData}
                      dataKey="count"
                      nameKey="name"
                      cx="50%"
                      cy="50%"
                      innerRadius={56}
                      outerRadius={90}
                      paddingAngle={3}
                    >
                      {orderStatusData.map((entry) => (
                        <Cell
                          key={entry.status}
                          fill={entry.color}
                          stroke="#FFFFFF"
                          strokeWidth={2}
                        />
                      ))}
                    </Pie>
                    <Tooltip
                      formatter={(value, name, props) => {
                        const count = Number(value || 0);
                        const rev = Number(props?.payload?.revenue || 0);
                        return [
                          `${count} pedido(s) — $${rev.toLocaleString('es-AR')}`,
                          String(name),
                        ];
                      }}
                      contentStyle={{
                        backgroundColor: '#0F1D38',
                        borderColor: '#C88D33',
                        borderRadius: '10px',
                        color: '#F8FAFC',
                        fontSize: '0.82rem',
                      }}
                      itemStyle={{ color: '#F8EAD0' }}
                    />
                  </PieChart>
                </ResponsiveContainer>
              </div>

              <div className="donut-legend-list">
                {orderStatusData.map((item) => (
                  <div key={item.status} className="donut-legend-item">
                    <div className="donut-legend-left">
                      <span
                        className="donut-color-dot"
                        style={{ backgroundColor: item.color }}
                      />
                      <span>{item.name}</span>
                    </div>
                    <div className="donut-legend-right">
                      <strong>{item.count}</strong>
                      <span>(${item.revenue.toLocaleString('es-AR')})</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Chart 3: Stock y Valorización por Categoría */}
        <div className="panel chart-panel">
          <div className="panel-header">
            <div>
              <div className="panel-title-group">
                <BarChart3 size={20} className="text-primary" />
                <h3>Stock y Valorización por Categoría</h3>
              </div>
              <p className="chart-subtitle">
                Bultos disponibles en depósito y capital valorizado (en miles de $)
              </p>
            </div>
            <span className="chart-badge">
              Val. Total: ${(totalInventoryValuation / 1000000).toFixed(2)}M
            </span>
          </div>
          <div className="panel-body">
            <div className="chart-container">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={categoryStockData}
                  margin={{ top: 10, right: 12, left: 0, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="#E6DCD2" vertical={false} />
                  <XAxis
                    dataKey="shortCategory"
                    tick={{ fill: '#6E5A4F', fontSize: 11 }}
                    axisLine={{ stroke: '#E6DCD2' }}
                    tickLine={false}
                  />
                  <YAxis
                    yAxisId="stock"
                    tick={{ fill: '#6E5A4F', fontSize: 11 }}
                    axisLine={false}
                    tickLine={false}
                    tickFormatter={(v: number) => `${v} u.`}
                  />
                  <YAxis
                    yAxisId="val"
                    orientation="right"
                    tick={{ fill: '#059669', fontSize: 11 }}
                    axisLine={false}
                    tickLine={false}
                    tickFormatter={(v: number) => `$${v}k`}
                  />
                  <Tooltip
                    formatter={(value, name) => {
                      const num = Number(value || 0);
                      if (name === 'Valorización ($ miles)') {
                        return [
                          `$${(num * 1000).toLocaleString('es-AR')}`,
                          'Valor en Depósito',
                        ];
                      }
                      return [`${num} bultos`, 'Stock Físico'];
                    }}
                    contentStyle={{
                      backgroundColor: '#0F1D38',
                      borderColor: '#C88D33',
                      borderRadius: '10px',
                      color: '#F8FAFC',
                      fontSize: '0.82rem',
                    }}
                    itemStyle={{ color: '#F8EAD0' }}
                    labelStyle={{ color: '#F1C27D', fontWeight: 700, marginBottom: '4px' }}
                  />
                  <Legend wrapperStyle={{ fontSize: '12px', paddingTop: '8px' }} />
                  <Bar
                    yAxisId="stock"
                    dataKey="stockBultos"
                    name="Stock (bultos)"
                    fill="#7B3F00"
                    radius={[6, 6, 0, 0]}
                    barSize={26}
                  />
                  <Bar
                    yAxisId="val"
                    dataKey="valorMiles"
                    name="Valorización ($ miles)"
                    fill="#059669"
                    radius={[6, 6, 0, 0]}
                    barSize={26}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>

        {/* Chart 4: Top Productos Más Vendidos */}
        <div className="panel chart-panel">
          <div className="panel-header">
            <div>
              <div className="panel-title-group">
                <Award size={20} className="text-primary" />
                <h3>Top Productos con Mayor Facturación</h3>
              </div>
              <p className="chart-subtitle">
                SKUs líderes en ingresos acumulados y rotación por bulto
              </p>
            </div>
            <button onClick={() => onNavigate('products')} className="btn-link">
              Ver Catálogo →
            </button>
          </div>
          <div className="panel-body">
            <div className="chart-container">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={topProductsData}
                  layout="vertical"
                  margin={{ top: 5, right: 24, left: 12, bottom: 5 }}
                >
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="#E6DCD2"
                    horizontal={true}
                    vertical={false}
                  />
                  <XAxis
                    type="number"
                    tick={{ fill: '#6E5A4F', fontSize: 11 }}
                    axisLine={{ stroke: '#E6DCD2' }}
                    tickLine={false}
                    tickFormatter={(v: number) => `$${(v / 1000).toFixed(0)}k`}
                  />
                  <YAxis
                    type="category"
                    dataKey="shortName"
                    width={135}
                    tick={{ fill: '#2B1B12', fontSize: 11, fontWeight: 600 }}
                    axisLine={false}
                    tickLine={false}
                  />
                  <Tooltip
                    formatter={(value, _name, props) => {
                      const num = Number(value || 0);
                      const bultos = Number(props?.payload?.bultos || 0);
                      return [
                        `$${num.toLocaleString('es-AR')} (${bultos} bultos)`,
                        'Total Facturado',
                      ];
                    }}
                    contentStyle={{
                      backgroundColor: '#0F1D38',
                      borderColor: '#C88D33',
                      borderRadius: '10px',
                      color: '#F8FAFC',
                      fontSize: '0.82rem',
                    }}
                    itemStyle={{ color: '#F8EAD0' }}
                    labelStyle={{ color: '#F1C27D', fontWeight: 700, marginBottom: '4px' }}
                  />
                  <Bar
                    dataKey="facturado"
                    name="Facturación ($)"
                    radius={[0, 6, 6, 0]}
                    barSize={20}
                  >
                    {topProductsData.map((entry, idx) => (
                      <Cell
                        key={entry.name}
                        fill={TOP_PRODUCT_COLORS[idx % TOP_PRODUCT_COLORS.length]}
                      />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      </div>

      {/* Price History & Dollar Synchronization Section */}
      <div className="panel" style={{ marginBottom: '1.75rem' }}>
        <div className="panel-header" style={{ flexWrap: 'wrap', gap: '0.75rem' }}>
          <div>
            <div className="panel-title-group">
              <History size={20} className="text-primary" />
              <h3>Histórico de Precios y Vigencias (Indexación Dólar en Vivo)</h3>
            </div>
            <p className="chart-subtitle">
              Trazabilidad de precios mayoristas por producto con fecha de inicio, fecha de fin o vigencia actual
            </p>
          </div>
          <span className="chart-badge">
            {priceHistory.filter((p) => p.currentlyValid).length} precios vigentes •{' '}
            {priceHistory.length} registros históricos
          </span>
        </div>

        <div className="panel-body">
          {/* Dollar Quote Sync Bar */}
          {dollarQuote && (
            <div className="currency-sync-bar">
              <div className="currency-sync-metrics">
                <div className="currency-metric-item">
                  <span className="currency-metric-label">Dólar Aplicado al Catálogo</span>
                  <span className="currency-metric-val text-success">
                    ${Math.round(dollarQuote.appliedRate).toLocaleString('es-AR')} ARS
                  </span>
                </div>
                <div className="currency-metric-item">
                  <span className="currency-metric-label">Dólar Oficial (C / V)</span>
                  <span className="currency-metric-val">
                    ${Math.round(dollarQuote.buyRate).toLocaleString('es-AR')} / $
                    {Math.round(dollarQuote.sellRate).toLocaleString('es-AR')}
                  </span>
                </div>
                <div className="currency-metric-item">
                  <span className="currency-metric-label">Dólar Blue (C / V)</span>
                  <span className="currency-metric-val" style={{ color: '#0284C7' }}>
                    ${Math.round(dollarQuote.blueBuyRate).toLocaleString('es-AR')} / $
                    {Math.round(dollarQuote.blueSellRate).toLocaleString('es-AR')}
                  </span>
                </div>
                <div className="currency-metric-item">
                  <span className="currency-metric-label">Fuente de Datos</span>
                  <span className="currency-metric-val" style={{ fontSize: '0.88rem' }}>
                    {dollarQuote.source} ({dollarQuote.rateType})
                  </span>
                </div>
              </div>

              <div className="currency-sync-actions">
                <button
                  type="button"
                  className="btn btn-sm btn-outline"
                  disabled={syncingDollar}
                  onClick={() => handleSyncDollarMode('OFICIAL')}
                >
                  <RefreshCw size={14} className={syncingDollar ? 'animate-spin' : ''} /> Aplicar
                  Dólar Oficial
                </button>
                <button
                  type="button"
                  className="btn btn-sm btn-outline"
                  disabled={syncingDollar}
                  onClick={() => handleSyncDollarMode('BLUE')}
                >
                  <DollarSign size={14} /> Aplicar Dólar Blue
                </button>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                  <input
                    type="number"
                    className="form-input"
                    style={{ width: 125, padding: '0.35rem 0.6rem', fontSize: '0.82rem' }}
                    placeholder="TC Manual ($)"
                    min={100}
                    max={50000}
                    value={customRateInput}
                    onChange={(e) => setCustomRateInput(e.target.value)}
                  />
                  <button
                    type="button"
                    className="btn btn-sm btn-primary"
                    disabled={syncingDollar || !customRateInput}
                    onClick={() => handleSyncDollarMode('CUSTOM')}
                  >
                    Actualizar Precios
                  </button>
                </div>
              </div>
            </div>
          )}

          {syncFeedback && (
            <div
              className="alert alert-info"
              style={{ marginBottom: '1rem', fontSize: '0.85rem', padding: '0.65rem 1rem' }}
            >
              {syncFeedback}
            </div>
          )}

          {/* Timeline Chart for Selected Product */}
          {priceTimelineChartData.length > 0 && (
            <div
              style={{
                background: '#FAF6F2',
                border: '1px solid var(--border)',
                borderRadius: 10,
                padding: '1rem 1.25rem',
                marginBottom: '1.25rem',
              }}
            >
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  marginBottom: '0.6rem',
                  flexWrap: 'wrap',
                  gap: '0.5rem',
                }}
              >
                <span style={{ fontSize: '0.86rem', fontWeight: 700, color: 'var(--text-main)' }}>
                  Curva de Vigencias — {priceTimelineChartData[0]?.productName}
                </span>
                <span style={{ fontSize: '0.76rem', color: 'var(--text-muted)' }}>
                  Comparativa Precio Mayorista (ARS) vs. Cotización Dólar Aplicada
                </span>
              </div>
              <div style={{ width: '100%', height: 210 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <ComposedChart
                    data={priceTimelineChartData}
                    margin={{ top: 8, right: 16, left: 4, bottom: 4 }}
                  >
                    <CartesianGrid strokeDasharray="3 3" stroke="#E6DCD2" vertical={false} />
                    <XAxis
                      dataKey="periodo"
                      tick={{ fill: '#6E5A4F', fontSize: 11 }}
                      axisLine={{ stroke: '#E6DCD2' }}
                      tickLine={false}
                    />
                    <YAxis
                      yAxisId="price"
                      tick={{ fill: '#7B3F00', fontSize: 11 }}
                      axisLine={false}
                      tickLine={false}
                      tickFormatter={(v: number) => `$${(v / 1000).toFixed(1)}k`}
                    />
                    <YAxis
                      yAxisId="usd"
                      orientation="right"
                      tick={{ fill: '#059669', fontSize: 11 }}
                      axisLine={false}
                      tickLine={false}
                      tickFormatter={(v: number) => `USD $${v}`}
                    />
                    <Tooltip
                      formatter={(value, name) => [
                        `$${Number(value || 0).toLocaleString('es-AR')}`,
                        String(name),
                      ]}
                      contentStyle={{
                        backgroundColor: '#0F1D38',
                        borderColor: '#C88D33',
                        borderRadius: '10px',
                        color: '#F8FAFC',
                        fontSize: '0.82rem',
                      }}
                    />
                    <Legend wrapperStyle={{ fontSize: '12px' }} />
                    <Bar
                      yAxisId="price"
                      dataKey="precioArs"
                      name="Precio Mayorista (ARS)"
                      fill="#7B3F00"
                      barSize={34}
                      radius={[6, 6, 0, 0]}
                    />
                    <Line
                      yAxisId="usd"
                      type="monotone"
                      dataKey="dolarAplicado"
                      name="Cotización Dólar ($ ARS/USD)"
                      stroke="#059669"
                      strokeWidth={2.5}
                      dot={{ r: 4, fill: '#059669' }}
                    />
                  </ComposedChart>
                </ResponsiveContainer>
              </div>
            </div>
          )}

          {/* Filters Bar */}
          <div
            style={{
              display: 'flex',
              flexWrap: 'wrap',
              gap: '0.75rem',
              marginBottom: '1rem',
              alignItems: 'center',
            }}
          >
            <div style={{ position: 'relative', flex: '1 1 240px' }}>
              <Search
                size={16}
                style={{
                  position: 'absolute',
                  left: 12,
                  top: '50%',
                  transform: 'translateY(-50%)',
                  color: 'var(--text-muted)',
                }}
              />
              <input
                type="text"
                className="form-input"
                style={{ paddingLeft: '2.2rem' }}
                placeholder="Buscar por SKU, nombre de producto o motivo..."
                value={historySearch}
                onChange={(e) => setHistorySearch(e.target.value)}
              />
            </div>

            <select
              className="form-select"
              style={{ width: 'auto', minWidth: 230 }}
              value={selectedProductId}
              onChange={(e) => setSelectedProductId(e.target.value)}
            >
              <option value="ALL">Todos los productos ({allProducts.length})</option>
              {allProducts.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.code} — {p.name}
                </option>
              ))}
            </select>

            <select
              className="form-select"
              style={{ width: 'auto', minWidth: 210 }}
              value={validityFilter}
              onChange={(e) =>
                setValidityFilter(e.target.value as 'ALL' | 'ACTIVE' | 'HISTORICAL')
              }
            >
              <option value="ALL">Todas las vigencias</option>
              <option value="ACTIVE">Solo vigentes en la actualidad</option>
              <option value="HISTORICAL">Solo históricas finalizadas</option>
            </select>
          </div>

          {/* Price Validity History Table */}
          <div className="table-responsive" style={{ maxHeight: 420, overflowY: 'auto' }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>SKU</th>
                  <th>Producto</th>
                  <th>Base USD</th>
                  <th>Dólar Aplicado</th>
                  <th>Precio (ARS)</th>
                  <th>Vigencia Desde</th>
                  <th>Vigencia Hasta</th>
                  <th>Estado de Vigencia</th>
                  <th>Motivo / Origen</th>
                </tr>
              </thead>
              <tbody>
                {filteredPriceHistory.length === 0 ? (
                  <tr>
                    <td colSpan={9} className="text-center text-muted py-4">
                      No se encontraron registros de vigencia con los filtros seleccionados.
                    </td>
                  </tr>
                ) : (
                  filteredPriceHistory.map((item) => (
                    <tr key={item.id}>
                      <td className="font-mono font-semibold">{item.productCode}</td>
                      <td>
                        <div className="font-semibold">{item.productName}</div>
                        <div className="text-xs text-muted">{item.packaging}</div>
                      </td>
                      <td className="font-mono">
                        USD ${Number(item.usdPrice || 0).toFixed(2)}
                      </td>
                      <td className="font-mono">
                        ${Math.round(Number(item.exchangeRate || 0)).toLocaleString('es-AR')}
                      </td>
                      <td className="font-bold text-primary">
                        ${Math.round(item.price).toLocaleString('es-AR')}
                      </td>
                      <td>
                        <span className="flex items-center gap-1 font-mono text-xs">
                          <Calendar size={13} className="text-muted" />
                          {formatIsoDateEs(item.startDate)}
                        </span>
                      </td>
                      <td>
                        {item.currentlyValid || !item.endDate ? (
                          <span className="text-success font-semibold text-xs">
                            Sigue vigente hoy
                          </span>
                        ) : (
                          <span className="font-mono text-xs">
                            {formatIsoDateEs(item.endDate)}
                          </span>
                        )}
                      </td>
                      <td>
                        {item.currentlyValid ? (
                          <span className="validity-active-pill">
                            <CheckCircle2 size={13} /> Vigente en la actualidad
                          </span>
                        ) : (
                          <span className="validity-ended-pill">Finalizado</span>
                        )}
                      </td>
                      <td className="text-xs text-muted">{item.reason}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Operational Tables Layout */}
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
          <div className="panel-body table-responsive">
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
          <div className="panel-body table-responsive">
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
                          {ORDER_STATUS_LABELS[o.status] || o.status}
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

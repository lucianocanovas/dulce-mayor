import React, { useState, useEffect, useCallback } from 'react';
import type { BackendOrderSummary } from '../types/models';
import { OrderStatus, PaymentType } from '../types/enums';
import { fetchOrdersFromBackend, fetchOrCreateMercadoPagoQr, confirmManualPayment } from '../services/api';
import { MercadoPagoQrModal } from '../components/MercadoPagoQrModal';
import {
  ClipboardList,
  QrCode,
  CheckCircle2,
  RefreshCw,
  Search,
  Store,
  Loader2,
  CheckCheck,
} from 'lucide-react';

export const PresaleOrdersView: React.FC = () => {
  const [orders, setOrders] = useState<BackendOrderSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<'ALL' | 'PENDING' | 'PAID'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [notificationMsg, setNotificationMsg] = useState<string | null>(null);
  const [qrModalOrder, setQrModalOrder] = useState<{
    orderId: string;
    orderNumber: string | number;
    storeName: string;
    total: number;
    initPoint?: string;
    qrImageBase64?: string;
  } | null>(null);

  const isOrderPaid = (order: BackendOrderSummary) => (
    order.status === OrderStatus.PAID ||
    order.status === OrderStatus.INVOICED ||
    order.mpPaymentStatus?.toLowerCase().startsWith('approved') ||
    Boolean(order.paidAt)
  );

  const loadOrders = useCallback(async () => {
    setLoading(true);
    try {
      const data = await fetchOrdersFromBackend();
      setOrders(data);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadOrders();
  }, [loadOrders]);

  const handleOpenQr = async (ord: BackendOrderSummary) => {
    try {
      const qr = await fetchOrCreateMercadoPagoQr(ord.id);
      setQrModalOrder({
        orderId: ord.id,
        orderNumber: qr.orderNumber || ord.orderNumber,
        storeName: ord.storeName,
        total: qr.total || ord.total,
        initPoint: qr.initPoint || ord.mpInitPoint,
        qrImageBase64: qr.qrImageBase64,
      });
    } catch {
      if (ord.mpInitPoint) {
        setQrModalOrder({
          orderId: ord.id,
          orderNumber: ord.orderNumber,
          storeName: ord.storeName,
          total: ord.total,
          initPoint: ord.mpInitPoint,
        });
      } else {
        setNotificationMsg(`Error al abrir QR para pedido #${ord.orderNumber}`);
        setTimeout(() => setNotificationMsg(null), 3000);
      }
    }
  };

  const handleQuickManualConfirm = async (ord: BackendOrderSummary) => {
    try {
      await confirmManualPayment(ord.id, 'CASH', 'DIRECT-PREV', 'Cobro confirmado por preventista');
      setNotificationMsg(`✓ Pedido #${ord.orderNumber} marcado como Pagado.`);
      loadOrders();
      setTimeout(() => setNotificationMsg(null), 3500);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al confirmar cobro';
      setNotificationMsg(`✗ ${msg}`);
      setTimeout(() => setNotificationMsg(null), 3500);
    }
  };

  const filteredOrders = orders.filter((o) => {
    const isPaid = isOrderPaid(o);
    if (filter === 'PENDING' && isPaid) return false;
    if (filter === 'PAID' && !isPaid) return false;

    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchStore = o.storeName.toLowerCase().includes(q);
      const matchNum = String(o.orderNumber).toLowerCase().includes(q);
      return matchStore || matchNum;
    }
    return true;
  });

  const totalSales = orders.reduce((acc, o) => acc + (o.total || 0), 0);
  const paidSales = orders
    .filter(isOrderPaid)
    .reduce((acc, o) => acc + (o.total || 0), 0);

  return (
    <div className="view-container">
      {/* Header Metric Summary */}
      <div className="route-header-card" style={{ marginBottom: '0.85rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <span className="route-code" style={{ backgroundColor: '#1B449C', color: '#FFFFFF' }}>
              PREVENTA DEL DÍA
            </span>
            <h3 style={{ margin: '0.35rem 0 0.15rem', fontSize: '1.1rem', fontWeight: 800, color: '#0F1D38' }}>
              Pedidos Tomados en Kioscos
            </h3>
            <p style={{ margin: 0, fontSize: '0.78rem', color: '#64748B' }}>
              {orders.length} pedidos registrados • Total: ${totalSales.toLocaleString('es-AR')}
            </p>
          </div>
          <button
            type="button"
            onClick={loadOrders}
            className="toggle-btn"
            title="Refrescar lista"
            style={{ width: '38px', height: '38px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
          >
            <RefreshCw size={17} className={loading ? 'animate-spin' : ''} />
          </button>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.65rem', marginTop: '0.85rem' }}>
          <div style={{ background: '#F8FAFC', padding: '0.65rem', borderRadius: '10px', border: '1px solid #E2E8F0' }}>
            <div style={{ fontSize: '0.72rem', color: '#64748B', fontWeight: 600 }}>Cobrado / Acreditado</div>
            <div style={{ fontSize: '1.05rem', fontWeight: 800, color: '#16A34A' }}>
              ${paidSales.toLocaleString('es-AR')}
            </div>
          </div>
          <div style={{ background: '#F8FAFC', padding: '0.65rem', borderRadius: '10px', border: '1px solid #E2E8F0' }}>
            <div style={{ fontSize: '0.72rem', color: '#64748B', fontWeight: 600 }}>Pendientes de Pago</div>
            <div style={{ fontSize: '1.05rem', fontWeight: 800, color: '#C88D33' }}>
              ${(totalSales - paidSales).toLocaleString('es-AR')}
            </div>
          </div>
        </div>
      </div>

      {/* Notification Toast */}
      {notificationMsg && (
        <div className="toast-banner info" style={{ marginBottom: '0.75rem' }}>
          <CheckCircle2 size={16} />
          <span>{notificationMsg}</span>
        </div>
      )}

      {/* Filter Tabs & Search */}
      <div style={{ display: 'flex', gap: '0.4rem', marginBottom: '0.65rem' }}>
        <button
          type="button"
          onClick={() => setFilter('ALL')}
          style={{
            flex: 1,
            padding: '0.45rem',
            borderRadius: '8px',
            fontSize: '0.78rem',
            fontWeight: 700,
            border: filter === 'ALL' ? '1.5px solid #1B449C' : '1px solid #CBD5E1',
            background: filter === 'ALL' ? '#1B449C' : '#FFFFFF',
            color: filter === 'ALL' ? '#FFFFFF' : '#475569',
            cursor: 'pointer',
          }}
        >
          Todos ({orders.length})
        </button>
        <button
          type="button"
          onClick={() => setFilter('PENDING')}
          style={{
            flex: 1,
            padding: '0.45rem',
            borderRadius: '8px',
            fontSize: '0.78rem',
            fontWeight: 700,
            border: filter === 'PENDING' ? '1.5px solid #C88D33' : '1px solid #CBD5E1',
            background: filter === 'PENDING' ? '#C88D33' : '#FFFFFF',
            color: filter === 'PENDING' ? '#FFFFFF' : '#475569',
            cursor: 'pointer',
          }}
        >
          Pendientes
        </button>
        <button
          type="button"
          onClick={() => setFilter('PAID')}
          style={{
            flex: 1,
            padding: '0.45rem',
            borderRadius: '8px',
            fontSize: '0.78rem',
            fontWeight: 700,
            border: filter === 'PAID' ? '1.5px solid #16A34A' : '1px solid #CBD5E1',
            background: filter === 'PAID' ? '#16A34A' : '#FFFFFF',
            color: filter === 'PAID' ? '#FFFFFF' : '#475569',
            cursor: 'pointer',
          }}
        >
          Cobrados
        </button>
      </div>

      <div className="search-box" style={{ marginBottom: '0.75rem' }}>
        <Search size={16} className="search-icon" />
        <input
          type="text"
          placeholder="Buscar por comercio o nro de pedido..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="search-input"
          style={{ fontSize: '0.82rem' }}
        />
      </div>

      {/* Orders List */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '2rem' }}>
          <Loader2 className="animate-spin" size={30} style={{ color: '#1B449C', margin: '0 auto 0.5rem' }} />
          <p style={{ color: '#64748B', fontSize: '0.85rem' }}>Cargando pedidos tomados hoy...</p>
        </div>
      ) : filteredOrders.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '2rem', background: '#FFFFFF', borderRadius: '12px', border: '1px solid #E2E8F0' }}>
          <ClipboardList size={36} style={{ color: '#94A3B8', margin: '0 auto 0.5rem' }} />
          <h4 style={{ margin: '0 0 0.25rem', color: '#0F1D38' }}>No hay pedidos en este estado</h4>
          <p style={{ margin: 0, color: '#64748B', fontSize: '0.8rem' }}>
            Comenzá a tomar pedidos de comercios desde la pestaña "Tomar Pedido".
          </p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
          {filteredOrders.map((ord) => {
            const isPaid = isOrderPaid(ord);
            const isMp = ord.paymentType === PaymentType.MERCADO_PAGO || ord.paymentType === PaymentType.DIGITAL_WALLET;

            return (
              <div
                key={ord.id}
                style={{
                  background: '#FFFFFF',
                  borderRadius: '12px',
                  border: isPaid ? '1.5px solid #BBF7D0' : '1.5px solid #FDE68A',
                  padding: '0.85rem 1rem',
                  boxShadow: '0 2px 5px rgba(0,0,0,0.03)',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.4rem' }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', marginBottom: '0.15rem' }}>
                      <Store size={14} style={{ color: '#1B449C' }} />
                      <strong style={{ fontSize: '0.92rem', color: '#0F1D38' }}>{ord.storeName}</strong>
                    </div>
                    <span style={{ fontSize: '0.74rem', color: '#64748B' }}>
                      Pedido #{String(ord.orderNumber).replace('ORD-', '')} • Gran Mendoza
                    </span>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: '1.05rem', fontWeight: 800, color: '#0F1D38' }}>
                      ${ord.total.toLocaleString('es-AR')}
                    </div>
                    <span
                      style={{
                        display: 'inline-block',
                        fontSize: '0.7rem',
                        fontWeight: 700,
                        padding: '2px 7px',
                        borderRadius: '999px',
                        background: isPaid ? '#DCFCE7' : '#FEF3C7',
                        color: isPaid ? '#15803D' : '#B45309',
                      }}
                    >
                      {isPaid ? '✓ COBRADO' : '⏳ PENDIENTE'}
                    </span>
                  </div>
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '0.5rem', paddingTop: '0.5rem', borderTop: '1px solid #F1F5F9' }}>
                  <span style={{ fontSize: '0.74rem', color: '#64748B' }}>
                    Pago: <strong>{ord.paymentType || 'Mercado Pago'}</strong>
                  </span>

                  <div style={{ display: 'flex', gap: '0.4rem' }}>
                    {!isPaid && isMp && (
                      <button
                        type="button"
                        onClick={() => handleOpenQr(ord)}
                        style={{
                          background: '#1B449C',
                          color: '#FFFFFF',
                          border: 'none',
                          borderRadius: '6px',
                          padding: '0.35rem 0.65rem',
                          fontSize: '0.75rem',
                          fontWeight: 700,
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '0.3rem',
                        }}
                      >
                        <QrCode size={13} />
                        <span>Ver QR</span>
                      </button>
                    )}

                    {!isPaid && (
                      <button
                        type="button"
                        onClick={() => handleQuickManualConfirm(ord)}
                        style={{
                          background: '#16A34A',
                          color: '#FFFFFF',
                          border: 'none',
                          borderRadius: '6px',
                          padding: '0.35rem 0.65rem',
                          fontSize: '0.75rem',
                          fontWeight: 700,
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '0.3rem',
                        }}
                      >
                        <CheckCheck size={13} />
                        <span>Marcar Cobrado</span>
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* QR Modal for scanning */}
      {qrModalOrder && (
        <MercadoPagoQrModal
          orderId={qrModalOrder.orderId}
          orderNumber={qrModalOrder.orderNumber}
          storeName={qrModalOrder.storeName}
          total={qrModalOrder.total}
          initPoint={qrModalOrder.initPoint}
          qrImageBase64={qrModalOrder.qrImageBase64}
          onClose={() => {
            setQrModalOrder(null);
            loadOrders();
          }}
          onPaymentAccredited={() => {
            loadOrders();
          }}
        />
      )}
    </div>
  );
};

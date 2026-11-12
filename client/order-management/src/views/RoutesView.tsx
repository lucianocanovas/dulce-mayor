import React, { useState, useEffect, useCallback } from 'react';
import type { BackendOrderSummary, RouteStop } from '../types/models';
import { OrderStatus, PaymentType, UserRole } from '../types/enums';
import { useAuth } from '../context/AuthContext';
import { LeafletRouteMap } from '../components/LeafletRouteMap';
import { MercadoPagoQrModal } from '../components/MercadoPagoQrModal';
import {
  fetchDailyRoute,
  saveStoredStops,
  markDeliveryOnBackend,
  fetchOrdersFromBackend,
  fetchOrCreateMercadoPagoQr,
} from '../services/api';
import {
  MapPin,
  List,
  CheckCircle2,
  Clock,
  Navigation,
  ShoppingBag,
  Send,
  Loader2,
  QrCode,
  RefreshCw,
} from 'lucide-react';

interface RoutesViewProps {
  onStartOrderForStop?: (stop: RouteStop) => void;
}

export const RoutesView: React.FC<RoutesViewProps> = ({ onStartOrderForStop }) => {
  const { user } = useAuth();
  const [stops, setStops] = useState<RouteStop[]>([]);
  const [pendingOrders, setPendingOrders] = useState<BackendOrderSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [viewMode, setViewMode] = useState<'list' | 'map'>('list');
  const [selectedStopId, setSelectedStopId] = useState<string>('');
  const [notificationMsg, setNotificationMsg] = useState<string | null>(null);
  const [qrModalOrder, setQrModalOrder] = useState<{
    orderId: string;
    orderNumber: string | number;
    storeName: string;
    total: number;
    initPoint?: string;
    qrImageBase64?: string;
  } | null>(null);

  const isDelivery = user?.role === UserRole.DELIVERY_AGENT;

  const loadRoute = useCallback(async () => {
    setLoading(true);
    try {
      const [routeData, allOrders] = await Promise.all([
        fetchDailyRoute(user?.role),
        !isDelivery ? fetchOrdersFromBackend() : Promise.resolve([]),
      ]);
      setStops(routeData.stops);
      setPendingOrders(
        allOrders.filter(
          (o) =>
            o.status === OrderStatus.PENDING_PAYMENT &&
            (o.paymentType === PaymentType.MERCADO_PAGO ||
              o.paymentType === PaymentType.DIGITAL_WALLET)
        )
      );
      const nextPending = routeData.stops.find((s) => s.status === 'PENDING');
      if (nextPending) {
        setSelectedStopId(nextPending.id);
      } else if (routeData.stops.length > 0) {
        setSelectedStopId(routeData.stops[0].id);
      }
    } finally {
      setLoading(false);
    }
  }, [user?.role, isDelivery]);

  useEffect(() => {
    loadRoute();
  }, [loadRoute]);

  const handleOpenQrForOrder = async (
    orderId: string,
    orderNumber: string | number,
    storeName: string,
    total: number,
    existingInitPoint?: string
  ) => {
    try {
      const qr = await fetchOrCreateMercadoPagoQr(orderId);
      setQrModalOrder({
        orderId,
        orderNumber: qr.orderNumber || orderNumber,
        storeName,
        total: qr.total || total,
        initPoint: qr.initPoint || existingInitPoint,
        qrImageBase64: qr.qrImageBase64,
      });
    } catch (err: unknown) {
      if (existingInitPoint) {
        setQrModalOrder({
          orderId,
          orderNumber,
          storeName,
          total,
          initPoint: existingInitPoint,
        });
      } else {
        const msg = err instanceof Error ? err.message : 'No se pudo generar el QR de Mercado Pago.';
        setNotificationMsg(`✗ ${msg}`);
      }
    }
  };

  const handleMarkDelivered = async (stopId: string) => {
    const stopToDeliver = stops.find((s) => s.id === stopId);
    if (stopToDeliver?.orderId) {
      markDeliveryOnBackend(stopToDeliver.orderId);
    }

    const updated = stops.map((s) => {
      if (s.id === stopId) {
        return {
          ...s,
          status: 'DELIVERED' as const,
          deliveredAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        };
      }
      return s;
    });

    setStops(updated);
    saveStoredStops(updated);

    // Find next stop to notify
    const currentIndex = stops.findIndex((s) => s.id === stopId);
    const nextStop = stops[currentIndex + 1];

    if (nextStop) {
      setNotificationMsg(
        `✓ Parada #${currentIndex + 1} entregada. Se notificó al siguiente comercio: ${nextStop.pointOfSale.name}.`
      );
      setSelectedStopId(nextStop.id);
    } else {
      setNotificationMsg('🎉 ¡Ruta completada! Todas las entregas del día fueron registradas.');
    }

    setTimeout(() => {
      setNotificationMsg(null);
    }, 4500);
  };

  const completedCount = stops.filter(
    (s) => s.status === 'DELIVERED' || s.status === 'VISITED'
  ).length;

  const selectedStop = stops.find((s) => s.id === selectedStopId) || stops[0];

  if (loading) {
    return (
      <div className="view-container" style={{ textAlign: 'center', padding: '3rem' }}>
        <Loader2 className="animate-spin" size={36} style={{ color: 'var(--dm-primary-orange)', margin: '0 auto 1rem' }} />
        <p style={{ color: 'var(--dm-text-subtle)' }}>Sincronizando ruta diaria con la base de datos...</p>
      </div>
    );
  }

  return (
    <div className="view-container">
      {/* View Mode Toggle & Summary */}
      <div className="route-header-card">
        <div className="route-info">
          <div className="route-badge-row">
            <span className="route-code">RUT-MZA-CENTRO</span>
            <span className="agent-badge">
              {isDelivery ? 'Modo Repartidor' : 'Modo Preventista'}
            </span>
          </div>
          <p className="route-progress-text">
            <strong>{completedCount}</strong> de <strong>{stops.length}</strong> paradas completadas
          </p>
          <div className="progress-bar-track">
            <div
              className="progress-bar-fill"
              style={{
                width: stops.length > 0 ? `${(completedCount / stops.length) * 100}%` : '0%',
              }}
            />
          </div>
        </div>

        <div className="view-mode-toggle">
          <button
            onClick={() => setViewMode('list')}
            className={`toggle-btn ${viewMode === 'list' ? 'active' : ''}`}
            aria-label="Ver como lista"
          >
            <List size={18} />
            <span>Lista</span>
          </button>
          <button
            onClick={() => setViewMode('map')}
            className={`toggle-btn ${viewMode === 'map' ? 'active' : ''}`}
            aria-label="Ver en mapa interactivo"
          >
            <MapPin size={18} />
            <span>Mapa GPS</span>
          </button>
          <button
            onClick={() => loadRoute()}
            className="toggle-btn"
            aria-label="Actualizar ruta"
            title="Sincronizar con servidor"
          >
            <RefreshCw size={17} />
          </button>
        </div>
      </div>

      {/* Pending QR Payments Section (Preventista) */}
      {!isDelivery && pendingOrders.length > 0 && (
        <div
          style={{
            background: '#FFFBEB',
            border: '1px solid #FDE68A',
            borderRadius: '14px',
            padding: '0.85rem 1rem',
            marginBottom: '0.9rem',
          }}
        >
          <div style={{ fontWeight: 800, fontSize: '0.88rem', color: '#92400E', marginBottom: '0.5rem' }}>
            Cobros QR Pendientes de Acreditación ({pendingOrders.length})
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.45rem' }}>
            {pendingOrders.map((ord) => (
              <div
                key={ord.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  gap: '0.5rem',
                  background: '#FFFFFF',
                  padding: '0.55rem 0.75rem',
                  borderRadius: '10px',
                  border: '1px solid #F3E8D2',
                }}
              >
                <div style={{ fontSize: '0.82rem' }}>
                  <strong>Pedido #{ord.orderNumber}</strong> • {ord.storeName}
                  <div style={{ color: '#7A6254' }}>${ord.total.toLocaleString('es-AR')}</div>
                </div>
                <button
                  type="button"
                  className="btn btn-sm btn-primary"
                  onClick={() =>
                    handleOpenQrForOrder(
                      ord.id,
                      ord.orderNumber,
                      ord.storeName,
                      ord.total,
                      ord.mpInitPoint
                    )
                  }
                >
                  <QrCode size={14} />
                  <span>Ver QR</span>
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Notification Banner */}
      {notificationMsg && (
        <div className="toast-banner info">
          <Send size={18} />
          <span>{notificationMsg}</span>
        </div>
      )}

      {/* Map View */}
      {viewMode === 'map' ? (
        <div className="route-map-section">
          <LeafletRouteMap
            stops={stops}
            selectedStopId={selectedStopId}
            onSelectStop={(stop) => setSelectedStopId(stop.id)}
          />

          {selectedStop && (
            <div className="selected-stop-card">
              <div className="selected-stop-header">
                <div>
                  <span className="stop-num-tag">Parada #{selectedStop.orderIndex}</span>
                  <h4 className="stop-name">{selectedStop.pointOfSale.name}</h4>
                  <p className="stop-addr">{selectedStop.pointOfSale.address}, {selectedStop.pointOfSale.neighborhood}</p>
                </div>
                <span className={`status-pill ${selectedStop.status.toLowerCase()}`}>
                  {selectedStop.status === 'DELIVERED'
                    ? 'Entregado'
                    : selectedStop.status === 'VISITED'
                    ? 'Visitado'
                    : 'Pendiente'}
                </span>
              </div>

              {selectedStop.orderTotal && (
                <div className="stop-order-row">
                  <span>Pedido #{selectedStop.orderId}</span>
                  <strong>${selectedStop.orderTotal.toLocaleString('es-AR')}</strong>
                </div>
              )}

              <div className="selected-stop-actions">
                {isDelivery ? (
                  <button
                    onClick={() => handleMarkDelivered(selectedStop.id)}
                    disabled={selectedStop.status === 'DELIVERED'}
                    className="btn btn-primary btn-block"
                  >
                    <CheckCircle2 size={16} />
                    <span>{selectedStop.status === 'DELIVERED' ? 'Entrega Confirmada' : 'Confirmar Entrega y Cobro'}</span>
                  </button>
                ) : (
                  <button
                    onClick={() => {
                      if (onStartOrderForStop) onStartOrderForStop(selectedStop);
                    }}
                    className="btn btn-primary btn-block"
                  >
                    <ShoppingBag size={16} />
                    <span>Tomar Pedido Mayorista</span>
                  </button>
                )}
              </div>
            </div>
          )}
        </div>
      ) : (
        /* List View */
        <div className="stops-list">
          {stops.map((stop) => {
            const isDelivered = stop.status === 'DELIVERED';

            return (
              <div
                key={stop.id}
                className={`stop-card ${selectedStopId === stop.id ? 'active-target' : ''} ${
                  isDelivered ? 'delivered' : ''
                }`}
                onClick={() => setSelectedStopId(stop.id)}
              >
                <div className="stop-indicator">
                  <div className={`stop-index-circle ${isDelivered ? 'done' : ''}`}>
                    {isDelivered ? <CheckCircle2 size={16} /> : stop.orderIndex}
                  </div>
                  {stop.orderIndex < stops.length && <div className="stop-connector-line" />}
                </div>

                <div className="stop-content">
                  <div className="stop-content-top">
                    <div>
                      <h4 className="stop-store-name">{stop.pointOfSale.name}</h4>
                      <p className="stop-location">
                        <MapPin size={13} />
                        <span>
                          {stop.pointOfSale.address}, {stop.pointOfSale.neighborhood} ({stop.pointOfSale.city})
                        </span>
                      </p>
                    </div>

                    <div className="stop-time-badge">
                      <Clock size={13} />
                      <span>{stop.deliveredAt || stop.estimatedArrival}</span>
                    </div>
                  </div>

                  {stop.orderTotal && (
                    <div className="stop-order-info">
                      <span className="order-tag">
                        {stop.orderNumber ? `Pedido #${stop.orderNumber}` : 'Pedido Activo'}
                      </span>
                      <span className="order-sum">${stop.orderTotal.toLocaleString('es-AR')}</span>
                    </div>
                  )}

                  {(stop.orderId || stop.sourceChannel || stop.paymentType) && (
                    <div className="stop-badges-strip">
                      {stop.sourceChannel === 'LANDING' ? (
                        <span className="dm-mini-badge web">Tienda Web</span>
                      ) : stop.orderId ? (
                        <span className="dm-mini-badge field">Preventa</span>
                      ) : null}

                      {stop.paid ? (
                        <span className="dm-mini-badge paid">
                          Pagado {stop.invoiceNumber ? `• Factura #${stop.invoiceNumber}` : ''}
                        </span>
                      ) : stop.status === 'PENDING_PAYMENT' ? (
                        <span className="dm-mini-badge pending">Pendiente de Pago</span>
                      ) : stop.paymentType === 'CASH' ? (
                        <span className="dm-mini-badge cash">Efectivo contra entrega</span>
                      ) : null}
                    </div>
                  )}

                  {stop.notes && <p className="stop-notes">"{stop.notes}"</p>}

                  <div className="stop-actions-row">
                    <a
                      href={`https://www.google.com/maps/dir/?api=1&destination=${stop.pointOfSale.latitude},${stop.pointOfSale.longitude}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="btn btn-sm btn-outline-secondary"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <Navigation size={14} />
                      <span>Cómo llegar</span>
                    </a>

                    {!isDelivery &&
                      stop.status === 'PENDING_PAYMENT' &&
                      stop.orderId &&
                      stop.orderTotal && (
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleOpenQrForOrder(
                              stop.orderId!,
                              stop.orderNumber || stop.orderId!,
                              stop.pointOfSale.name,
                              stop.orderTotal!,
                              stop.mpInitPoint
                            );
                          }}
                          className="btn btn-sm btn-outline-secondary"
                        >
                          <QrCode size={14} />
                          <span>Ver QR</span>
                        </button>
                      )}

                    {isDelivery ? (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleMarkDelivered(stop.id);
                        }}
                        disabled={isDelivered}
                        className={`btn btn-sm ${isDelivered ? 'btn-ghost' : 'btn-primary'}`}
                      >
                        <CheckCircle2 size={14} />
                        <span>{isDelivered ? 'Entregado' : 'Marcar Entregado'}</span>
                      </button>
                    ) : (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          if (onStartOrderForStop) onStartOrderForStop(stop);
                        }}
                        className="btn btn-sm btn-primary"
                      >
                        <ShoppingBag size={14} />
                        <span>Tomar Pedido</span>
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

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
            loadRoute();
          }}
          onPaymentAccredited={(status) => {
            setNotificationMsg(
              `✓ Pedido #${status.orderNumber} acreditado (Factura #${status.invoiceNumber || '-'}) y listo en Distribución.`
            );
            loadRoute();
          }}
        />
      )}
    </div>
  );
};

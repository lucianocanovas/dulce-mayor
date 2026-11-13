import React, { useState, useEffect, useCallback, useMemo } from 'react';
import type { RouteStop } from '../types/models';
import { UserRole } from '../types/enums';
import { LeafletRouteMap } from '../components/LeafletRouteMap';
import {
  fetchDailyRoute,
  saveStoredStops,
  markDeliveryOnBackend,
} from '../services/api';
import { optimizeDeliveryRoute, haversineDistanceKm } from '../utils/routeOptimizer';
import type { RouteOptimizationResult } from '../utils/routeOptimizer';
import {
  Navigation,
  CheckCircle2,
  Clock,
  Zap,
  Locate,
  RefreshCw,
  Phone,
  Package,
  Check,
} from 'lucide-react';

interface DeliveryDashboardViewProps {
  initialSubTab?: 'map' | 'pending' | 'delivered';
}

export const DeliveryDashboardView: React.FC<DeliveryDashboardViewProps> = ({
  initialSubTab = 'map',
}) => {
  const [stops, setStops] = useState<RouteStop[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeSubTab, setActiveSubTab] = useState<'map' | 'pending' | 'delivered'>(initialSubTab);
  const [selectedStopId, setSelectedStopId] = useState<string>('');
  const [notificationMsg, setNotificationMsg] = useState<string | null>(null);
  const [deliveringStopIds, setDeliveringStopIds] = useState<Set<string>>(new Set());

  useEffect(() => {
    setActiveSubTab(initialSubTab);
  }, [initialSubTab]);

  // Driver GPS Location
  const [driverLocation, setDriverLocation] = useState<[number, number]>([-32.8895, -68.8458]);
  const [gpsStatus, setGpsStatus] = useState<'live' | 'fallback' | 'locating'>('locating');
  const [gpsAccuracy, setGpsAccuracy] = useState<number | null>(null);

  // Route optimization state
  const [isOptimized, setIsOptimized] = useState(false);
  const [optMetrics, setOptMetrics] = useState<RouteOptimizationResult | null>(null);

  // Listen to live GPS position
  useEffect(() => {
    let watchId: number | null = null;
    if ('geolocation' in navigator) {
      setGpsStatus('locating');
      watchId = navigator.geolocation.watchPosition(
        (pos) => {
          setDriverLocation([pos.coords.latitude, pos.coords.longitude]);
          setGpsAccuracy(Math.round(pos.coords.accuracy));
          setGpsStatus('live');
        },
        (err) => {
          console.warn('Geolocation access error, using Mendoza Centro coordinates:', err.message);
          // Default to Mendoza KM 0 (Peatonal & San Martin)
          setDriverLocation([-32.8895, -68.8458]);
          setGpsStatus('fallback');
        },
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 5000 }
      );
    } else {
      setGpsStatus('fallback');
    }

    return () => {
      if (watchId !== null) navigator.geolocation.clearWatch(watchId);
    };
  }, []);

  // Load Daily Route Stops
  const loadRoute = useCallback(async () => {
    setLoading(true);
    try {
      const routeData = await fetchDailyRoute(UserRole.DELIVERY_AGENT);
      setStops(routeData.stops);

      const nextPending = routeData.stops.find((s) => s.status === 'PENDING');
      if (nextPending) {
        setSelectedStopId(nextPending.id);
      } else if (routeData.stops.length > 0) {
        setSelectedStopId(routeData.stops[0].id);
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadRoute();
  }, [loadRoute]);

  // Pending vs Delivered stops
  const pendingStops = useMemo(
    () => stops.filter((s) => s.status !== 'DELIVERED'),
    [stops]
  );

  const deliveredStops = useMemo(
    () => stops.filter((s) => s.status === 'DELIVERED'),
    [stops]
  );

  // Run Route Optimization (TSP from Driver's GPS coords)
  const handleOptimizeRoute = useCallback(() => {
    if (pendingStops.length === 0) return;

    const [driverLat, driverLon] = driverLocation;
    const result = optimizeDeliveryRoute(driverLat, driverLon, pendingStops);

    // Merge optimized pending stops with already delivered stops
    const updatedStops = [
      ...deliveredStops,
      ...result.optimizedStops,
    ];

    setStops(updatedStops);
    saveStoredStops(updatedStops);
    setOptMetrics(result);
    setIsOptimized(true);

    if (result.optimizedStops.length > 0) {
      setSelectedStopId(result.optimizedStops[0].id);
    }

    setNotificationMsg(
      `⚡ ¡Ruta Optimizada! ${result.optimizedStops.length} paradas ordenadas por mínima distancia (${result.totalDistanceKm} km en total).`
    );
    setTimeout(() => setNotificationMsg(null), 4500);
  }, [pendingStops, deliveredStops, driverLocation]);

  // Auto-optimize once when data and GPS are loaded
  useEffect(() => {
    if (!isOptimized && stops.length > 0 && gpsStatus !== 'locating') {
      handleOptimizeRoute();
    }
  }, [isOptimized, stops.length, gpsStatus, handleOptimizeRoute]);

  // Mark a stop as delivered
  const handleMarkDelivered = async (stopId: string) => {
    const stopToDeliver = stops.find((s) => s.id === stopId);
    if (!stopToDeliver || deliveringStopIds.has(stopId)) return;

    if (stopToDeliver.orderId) {
      markDeliveryOnBackend(stopToDeliver.orderId);
    }

    setDeliveringStopIds((current) => new Set(current).add(stopId));
    setNotificationMsg(`✓ Entrega en "${stopToDeliver.pointOfSale.name}" registrada. Se retirará del mapa en 5 segundos.`);

    window.setTimeout(() => {
      setDeliveringStopIds((current) => {
        const next = new Set(current);
        next.delete(stopId);
        return next;
      });

      setStops((current) => {
        const updated = current.map((stop) => (
          stop.id === stopId
            ? {
                ...stop,
                status: 'DELIVERED' as const,
                deliveredAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
              }
            : stop
        ));
        saveStoredStops(updated);

        const remainingPending = updated.filter((stop) => stop.status !== 'DELIVERED');
        if (remainingPending.length > 0) {
          setSelectedStopId(remainingPending[0].id);
          setNotificationMsg(
            `✓ Entrega en "${stopToDeliver.pointOfSale.name}" completada. Próximo destino: "${remainingPending[0].pointOfSale.name}".`
          );
        } else {
          setNotificationMsg('🎉 ¡Excelente trabajo! Todas las entregas del día fueron completadas.');
        }
        return updated;
      });
    }, 5000);

    setTimeout(() => setNotificationMsg(null), 4500);
  };

  const selectedStop = pendingStops.find((s) => s.id === selectedStopId) || pendingStops[0];

  return (
    <div className="view-container">
      {/* Driver Header Summary */}
      <div className="route-header-card" style={{ marginBottom: '0.75rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem', marginBottom: '0.25rem' }}>
              <span className="route-code" style={{ background: '#1B449C', color: '#FFFFFF' }}>
                HOJA DE RUTA REPARTO
              </span>
              <span style={{
                fontSize: '0.7rem',
                fontWeight: 700,
                padding: '2px 8px',
                borderRadius: '999px',
                background: gpsStatus === 'live' ? '#DCFCE7' : '#FEF3C7',
                color: gpsStatus === 'live' ? '#15803D' : '#B45309',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.25rem',
              }}>
                <Locate size={11} className={gpsStatus === 'locating' ? 'animate-spin' : ''} />
                <span>{gpsStatus === 'live' ? `GPS Activo (±${gpsAccuracy || 5}m)` : 'GPS Simulado'}</span>
              </span>
            </div>
            <h3 style={{ margin: '0.2rem 0 0.15rem', fontSize: '1.15rem', fontWeight: 800, color: '#0F1D38' }}>
              Ruta Más Eficiente de Entrega
            </h3>
            <p style={{ margin: 0, fontSize: '0.78rem', color: '#64748B' }}>
              {deliveredStops.length} de {stops.length} pedidos entregados ({pendingStops.length} pendientes)
            </p>
          </div>

          <button
            type="button"
            onClick={loadRoute}
            className="toggle-btn"
            title="Refrescar ruta con servidor"
            style={{ width: '36px', height: '36px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
          >
            <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
          </button>
        </div>

        {/* Progress Bar */}
        <div className="progress-bar-track" style={{ marginTop: '0.75rem' }}>
          <div
            className="progress-bar-fill"
            style={{
              width: stops.length > 0 ? `${(deliveredStops.length / stops.length) * 100}%` : '0%',
              backgroundColor: '#16A34A',
            }}
          />
        </div>

        {/* Optimization Metrics Banner */}
        <div style={{
          background: 'linear-gradient(135deg, #0F1D38 0%, #1B449C 100%)',
          borderRadius: '10px',
          padding: '0.75rem 0.9rem',
          marginTop: '0.75rem',
          color: '#FFFFFF',
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.45rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.8rem', fontWeight: 700, color: '#F8C36A' }}>
              <Zap size={15} />
              <span>Ruta Óptima (TSP)</span>
            </div>
            <button
              type="button"
              onClick={handleOptimizeRoute}
              style={{
                background: '#C88D33',
                color: '#0F1D38',
                border: 'none',
                borderRadius: '6px',
                padding: '0.3rem 0.6rem',
                fontSize: '0.72rem',
                fontWeight: 800,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '0.3rem',
                whiteSpace: 'nowrap',
              }}
            >
              <RefreshCw size={12} />
              <span>Recalcular GPS</span>
            </button>
          </div>

          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.35rem 0.9rem', marginTop: '0.5rem', fontSize: '0.76rem' }}>
            <div>
              <span style={{ color: '#CBD5E1' }}>Distancia: </span>
              <strong>{optMetrics?.totalDistanceKm || 0} km</strong>
            </div>
            <div>
              <span style={{ color: '#CBD5E1' }}>Tiempo: </span>
              <strong>~{optMetrics?.estimatedDurationMin || 0} min</strong>
            </div>
            {optMetrics && optMetrics.savingsPercent > 0 && (
              <div>
                <span style={{ color: '#86EFAC' }}>Ahorro: </span>
                <strong style={{ color: '#86EFAC' }}>-{optMetrics.savingsPercent}%</strong>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Sub Tabs: Mapa GPS | Pendientes | Entregados */}
      <div style={{ display: 'flex', gap: '0.3rem', marginBottom: '0.75rem' }}>
        <button
          type="button"
          onClick={() => setActiveSubTab('map')}
          style={{
            flex: 1,
            minWidth: 0,
            padding: '0.5rem 0.25rem',
            borderRadius: '8px',
            fontSize: '0.72rem',
            fontWeight: 800,
            border: activeSubTab === 'map' ? '1.5px solid #1B449C' : '1px solid #CBD5E1',
            background: activeSubTab === 'map' ? '#1B449C' : '#FFFFFF',
            color: activeSubTab === 'map' ? '#FFFFFF' : '#475569',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '0.25rem',
            whiteSpace: 'nowrap',
            cursor: 'pointer',
          }}
        >
          <Navigation size={14} style={{ flexShrink: 0 }} />
          <span>Mapa GPS</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveSubTab('pending')}
          style={{
            flex: 1,
            minWidth: 0,
            padding: '0.5rem 0.25rem',
            borderRadius: '8px',
            fontSize: '0.72rem',
            fontWeight: 800,
            border: activeSubTab === 'pending' ? '1.5px solid #C88D33' : '1px solid #CBD5E1',
            background: activeSubTab === 'pending' ? '#C88D33' : '#FFFFFF',
            color: activeSubTab === 'pending' ? '#FFFFFF' : '#475569',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '0.25rem',
            whiteSpace: 'nowrap',
            cursor: 'pointer',
          }}
        >
          <Package size={14} style={{ flexShrink: 0 }} />
          <span>Pendientes ({pendingStops.length})</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveSubTab('delivered')}
          style={{
            flex: 1,
            minWidth: 0,
            padding: '0.5rem 0.25rem',
            borderRadius: '8px',
            fontSize: '0.72rem',
            fontWeight: 800,
            border: activeSubTab === 'delivered' ? '1.5px solid #16A34A' : '1px solid #CBD5E1',
            background: activeSubTab === 'delivered' ? '#16A34A' : '#FFFFFF',
            color: activeSubTab === 'delivered' ? '#FFFFFF' : '#475569',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '0.25rem',
            whiteSpace: 'nowrap',
            cursor: 'pointer',
          }}
        >
          <CheckCircle2 size={14} style={{ flexShrink: 0 }} />
          <span>Entregados ({deliveredStops.length})</span>
        </button>
      </div>

      {/* Notification Toast */}
      {notificationMsg && (
        <div className="toast-banner info" style={{ marginBottom: '0.75rem' }}>
          <Check size={16} />
          <span>{notificationMsg}</span>
        </div>
      )}

      {/* SUB-TAB 1: MAPA GPS & NEXT ACTIVE STOP */}
      {activeSubTab === 'map' && (
        <div className="route-map-section">
          <LeafletRouteMap
            stops={pendingStops}
            driverLocation={driverLocation}
            selectedStopId={selectedStopId}
            onSelectStop={(stop) => setSelectedStopId(stop.id)}
            isDeliveryRole={true}
          />

          {selectedStop && (
            <div className="selected-stop-card" style={{ marginTop: '0.75rem' }}>
              <div className="selected-stop-header">
                <div>
                  <span className="stop-num-tag" style={{ background: '#1B449C', color: '#FFFFFF' }}>
                    Parada #{selectedStop.orderIndex || 1} en Ruta
                  </span>
                  <h4 className="stop-name" style={{ margin: '0.2rem 0' }}>{selectedStop.pointOfSale.name}</h4>
                  <p className="stop-addr" style={{ margin: 0, fontSize: '0.8rem', color: '#64748B' }}>
                    {selectedStop.pointOfSale.address}, {selectedStop.pointOfSale.neighborhood} ({selectedStop.pointOfSale.city})
                  </p>
                </div>
                <span className={`status-pill ${selectedStop.status.toLowerCase()}`}>
                  {selectedStop.status === 'DELIVERED' ? '✓ Entregado' : '⏳ Pendiente'}
                </span>
              </div>

              {selectedStop.pointOfSale.phone && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.78rem', color: '#475569', margin: '0.4rem 0' }}>
                  <Phone size={13} style={{ color: '#1B449C' }} />
                  <span>Contacto: {selectedStop.pointOfSale.ownerName} ({selectedStop.pointOfSale.phone})</span>
                </div>
              )}

              {selectedStop.orderTotal && (
                <div className="stop-order-row" style={{ marginTop: '0.45rem' }}>
                  <span>Bulto / Pedido #{selectedStop.orderId}</span>
                  <strong>${selectedStop.orderTotal.toLocaleString('es-AR')}</strong>
                </div>
              )}

              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', marginTop: '0.75rem' }}>
                <a
                  href={`https://www.google.com/maps/dir/?api=1&destination=${selectedStop.pointOfSale.latitude},${selectedStop.pointOfSale.longitude}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="btn btn-outline"
                  style={{
                    flex: '1 1 120px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '0.35rem',
                    padding: '0.65rem 0.5rem',
                    fontSize: '0.78rem',
                    textDecoration: 'none',
                    fontWeight: 700,
                    whiteSpace: 'nowrap',
                  }}
                >
                  <Navigation size={15} />
                  <span>Abrir GPS</span>
                </a>

                <button
                  type="button"
                  onClick={() => handleMarkDelivered(selectedStop.id)}
                  disabled={selectedStop.status === 'DELIVERED' || deliveringStopIds.has(selectedStop.id)}
                  className="btn btn-primary"
                  style={{
                    flex: '2 1 160px',
                    backgroundColor: selectedStop.status === 'DELIVERED' || deliveringStopIds.has(selectedStop.id) ? '#94A3B8' : '#16A34A',
                    borderColor: selectedStop.status === 'DELIVERED' || deliveringStopIds.has(selectedStop.id) ? '#94A3B8' : '#16A34A',
                    color: '#FFFFFF',
                    fontWeight: 800,
                    fontSize: '0.82rem',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '0.4rem',
                    whiteSpace: 'nowrap',
                  }}
                >
                  <CheckCircle2 size={16} />
                  <span>{selectedStop.status === 'DELIVERED' || deliveringStopIds.has(selectedStop.id) ? 'Entrega registrada' : 'Confirmar Entrega'}</span>
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* SUB-TAB 2: LISTA DE PENDIENTES EN SECUENCIA ÓPTIMA */}
      {activeSubTab === 'pending' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
          {pendingStops.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '2.5rem', background: '#FFFFFF', borderRadius: '12px', border: '1px solid #E2E8F0' }}>
              <CheckCircle2 size={42} style={{ color: '#16A34A', margin: '0 auto 0.5rem' }} />
              <h4 style={{ margin: '0 0 0.25rem', color: '#0F1D38' }}>¡Sin pedidos pendientes de entrega!</h4>
              <p style={{ margin: 0, color: '#64748B', fontSize: '0.82rem' }}>
                Completaste todas las paradas programadas para hoy.
              </p>
            </div>
          ) : (
            pendingStops.map((stop, idx) => {
              const distFromDriver = haversineDistanceKm(
                driverLocation[0],
                driverLocation[1],
                stop.pointOfSale.latitude,
                stop.pointOfSale.longitude
              );

              return (
                <div
                  key={stop.id}
                  style={{
                    background: '#FFFFFF',
                    borderRadius: '12px',
                    border: '1.5px solid #E2E8F0',
                    padding: '0.85rem 1rem',
                    boxShadow: '0 2px 6px rgba(0,0,0,0.03)',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div style={{ display: 'flex', gap: '0.65rem', alignItems: 'flex-start' }}>
                      <div style={{
                        width: '32px',
                        height: '32px',
                        borderRadius: '50%',
                        background: '#1B449C',
                        color: '#FFFFFF',
                        fontWeight: 800,
                        fontSize: '0.85rem',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexShrink: 0,
                      }}>
                        {stop.orderIndex || idx + 1}
                      </div>
                      <div>
                        <strong style={{ fontSize: '0.95rem', color: '#0F1D38', display: 'block' }}>
                          {stop.pointOfSale.name}
                        </strong>
                        <span style={{ fontSize: '0.78rem', color: '#64748B' }}>
                          {stop.pointOfSale.address}, {stop.pointOfSale.neighborhood}
                        </span>
                        <div style={{ fontSize: '0.72rem', color: '#1B449C', fontWeight: 700, marginTop: '0.2rem' }}>
                          📍 a {distFromDriver.toFixed(1)} km de tu posición actual
                        </div>
                      </div>
                    </div>

                    <div style={{ textAlign: 'right' }}>
                      <div style={{ fontSize: '1rem', fontWeight: 800, color: '#0F1D38' }}>
                        ${stop.orderTotal?.toLocaleString('es-AR') || '0'}
                      </div>
                      <span style={{
                        fontSize: '0.68rem',
                        fontWeight: 700,
                        background: '#FEF3C7',
                        color: '#B45309',
                        padding: '2px 6px',
                        borderRadius: '999px',
                      }}>
                        Pendiente
                      </span>
                    </div>
                  </div>

                  <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.75rem', paddingTop: '0.65rem', borderTop: '1px solid #F1F5F9' }}>
                    <a
                      href={`https://www.google.com/maps/dir/?api=1&destination=${stop.pointOfSale.latitude},${stop.pointOfSale.longitude}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      style={{
                        flex: 1,
                        padding: '0.45rem',
                        borderRadius: '6px',
                        border: '1px solid #CBD5E1',
                        background: '#FFFFFF',
                        color: '#1B449C',
                        fontWeight: 700,
                        fontSize: '0.76rem',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '0.3rem',
                        textDecoration: 'none',
                      }}
                    >
                      <Navigation size={13} />
                      <span>GPS Maps</span>
                    </a>

                    <button
                      type="button"
                      onClick={() => handleMarkDelivered(stop.id)}
                      disabled={deliveringStopIds.has(stop.id)}
                      style={{
                        flex: 2,
                        padding: '0.45rem',
                        borderRadius: '6px',
                        border: 'none',
                        background: '#16A34A',
                        color: '#FFFFFF',
                        fontWeight: 800,
                        fontSize: '0.78rem',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '0.35rem',
                        cursor: 'pointer',
                      }}
                    >
                      <CheckCircle2 size={14} />
                      <span>{deliveringStopIds.has(stop.id) ? 'Entrega registrada' : 'Confirmar Entrega'}</span>
                    </button>
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}

      {/* SUB-TAB 3: YA ENTREGADOS */}
      {activeSubTab === 'delivered' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
          {deliveredStops.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '2.5rem', background: '#FFFFFF', borderRadius: '12px', border: '1px solid #E2E8F0' }}>
              <Clock size={40} style={{ color: '#94A3B8', margin: '0 auto 0.5rem' }} />
              <h4 style={{ margin: '0 0 0.25rem', color: '#0F1D38' }}>Sin entregas registradas aún</h4>
              <p style={{ margin: 0, color: '#64748B', fontSize: '0.82rem' }}>
                A medida que confirmes entregas, quedarán archivadas aquí con su hora de entrega.
              </p>
            </div>
          ) : (
            deliveredStops.map((stop) => (
              <div
                key={stop.id}
                style={{
                  background: '#F0FDF4',
                  borderRadius: '12px',
                  border: '1.5px solid #BBF7D0',
                  padding: '0.85rem 1rem',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
                  <div style={{
                    width: '32px',
                    height: '32px',
                    borderRadius: '50%',
                    background: '#16A34A',
                    color: '#FFFFFF',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}>
                    <Check size={18} />
                  </div>
                  <div>
                    <strong style={{ fontSize: '0.92rem', color: '#065F46', display: 'block' }}>
                      {stop.pointOfSale.name}
                    </strong>
                    <span style={{ fontSize: '0.76rem', color: '#475569' }}>
                      {stop.pointOfSale.address} • Entregado a las {stop.deliveredAt || '11:45'}
                    </span>
                  </div>
                </div>

                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: '0.95rem', fontWeight: 800, color: '#065F46' }}>
                    ${stop.orderTotal?.toLocaleString('es-AR') || '0'}
                  </div>
                  <span style={{ fontSize: '0.68rem', fontWeight: 700, color: '#16A34A' }}>
                    Completado
                  </span>
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
};

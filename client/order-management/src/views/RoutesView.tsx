import React, { useState, useEffect } from 'react';
import type { RouteStop } from '../types/models';
import { UserRole } from '../types/enums';
import { useAuth } from '../context/AuthContext';
import { LeafletRouteMap } from '../components/LeafletRouteMap';
import { getStoredStops, saveStoredStops } from '../services/api';
import {
  MapPin,
  List,
  CheckCircle2,
  Clock,
  Navigation,
  ShoppingBag,
  Send,
} from 'lucide-react';

interface RoutesViewProps {
  onStartOrderForStop?: (stop: RouteStop) => void;
}

export const RoutesView: React.FC<RoutesViewProps> = ({ onStartOrderForStop }) => {
  const { user } = useAuth();
  const [stops, setStops] = useState<RouteStop[]>([]);
  const [viewMode, setViewMode] = useState<'list' | 'map'>('list');
  const [selectedStopId, setSelectedStopId] = useState<string>('');
  const [notificationMsg, setNotificationMsg] = useState<string | null>(null);

  const isDelivery = user?.role === UserRole.DELIVERY_AGENT;

  useEffect(() => {
    const loaded = getStoredStops();
    setStops(loaded);
    // Auto-select first pending stop
    const nextPending = loaded.find((s) => s.status === 'PENDING');
    if (nextPending) {
      setSelectedStopId(nextPending.id);
    }
  }, []);

  const handleMarkDelivered = (stopId: string) => {
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

  return (
    <div className="view-container">
      {/* Route Header Card */}
      <div className="route-header-card">
        <div className="route-info-left">
          <span className="route-badge">Ruta Mendoza Centro #01</span>
          <h2 className="route-title">
            {isDelivery ? 'Itinerario de Reparto' : 'Ruta de Preventa Diaria'}
          </h2>
          <span className="route-date">
            Progreso: <strong>{completedCount} de {stops.length}</strong> paradas
          </span>
        </div>

        {/* View Toggle (List vs Map) */}
        <div className="view-toggle">
          <button
            onClick={() => setViewMode('list')}
            className={`toggle-btn ${viewMode === 'list' ? 'active' : ''}`}
            title="Vista de lista"
          >
            <List size={18} />
          </button>
          <button
            onClick={() => setViewMode('map')}
            className={`toggle-btn ${viewMode === 'map' ? 'active' : ''}`}
            title="Vista de mapa interactivo"
          >
            <MapPin size={18} />
          </button>
        </div>
      </div>

      {/* Progress Bar */}
      <div className="progress-bar-container">
        <div
          className="progress-bar-fill"
          style={{ width: `${(completedCount / (stops.length || 1)) * 100}%` }}
        />
      </div>

      {/* Notification Toast */}
      {notificationMsg && (
        <div className="route-toast">
          <Send size={16} />
          <span>{notificationMsg}</span>
        </div>
      )}

      {/* Content based on View Mode */}
      {viewMode === 'map' ? (
        <div className="map-view-layout">
          <LeafletRouteMap
            stops={stops}
            selectedStopId={selectedStopId}
            onSelectStop={(s) => setSelectedStopId(s.id)}
            isDeliveryRole={isDelivery}
            onMarkDelivered={handleMarkDelivered}
          />

          {/* Quick Drawer below map for Selected Stop */}
          {selectedStop && (
            <div className="map-floating-card">
              <div className="stop-header-row">
                <span className="stop-index-pill">Parada #{selectedStop.orderIndex}</span>
                <span
                  className={`status-chip ${
                    selectedStop.status === 'DELIVERED' || selectedStop.status === 'VISITED'
                      ? 'chip-success'
                      : 'chip-warning'
                  }`}
                >
                  {selectedStop.status === 'DELIVERED'
                    ? 'Entregado'
                    : selectedStop.status === 'VISITED'
                    ? 'Visitado'
                    : 'Pendiente'}
                </span>
              </div>

              <h3 className="stop-name">{selectedStop.pointOfSale.name}</h3>
              <p className="stop-address">
                <Navigation size={13} /> {selectedStop.pointOfSale.address} (
                {selectedStop.pointOfSale.city})
              </p>

              <div className="stop-actions-row">
                {isDelivery ? (
                  selectedStop.status === 'PENDING' ? (
                    <button
                      onClick={() => handleMarkDelivered(selectedStop.id)}
                      className="btn-primary btn-block"
                    >
                      <CheckCircle2 size={16} />
                      <span>Confirmar Entrega</span>
                    </button>
                  ) : (
                    <div className="delivered-tag">
                      ✓ Entregado a las {selectedStop.deliveredAt || '09:20'}
                    </div>
                  )
                ) : (
                  <button
                    onClick={() => onStartOrderForStop && onStartOrderForStop(selectedStop)}
                    className="btn-primary btn-block"
                  >
                    <ShoppingBag size={16} />
                    <span>Iniciar Pedido en Comercio</span>
                  </button>
                )}
              </div>
            </div>
          )}
        </div>
      ) : (
        /* List Mode */
        <div className="stops-list">
          {stops.map((stop, index) => {
            const isCompleted = stop.status === 'DELIVERED' || stop.status === 'VISITED';
            const isCurrentPending = !isCompleted && selectedStopId === stop.id;

            return (
              <div
                key={stop.id}
                className={`stop-card ${isCurrentPending ? 'stop-card-active' : ''} ${
                  isCompleted ? 'stop-card-done' : ''
                }`}
                onClick={() => setSelectedStopId(stop.id)}
              >
                <div className="stop-card-left">
                  <div className={`stop-number-badge ${isCompleted ? 'badge-done' : ''}`}>
                    {isCompleted ? <CheckCircle2 size={16} /> : index + 1}
                  </div>
                  <div>
                    <div className="stop-name-row">
                      <h4 className="stop-card-title">{stop.pointOfSale.name}</h4>
                      {isCurrentPending && <span className="next-tag">Siguiente parada</span>}
                    </div>
                    <span className="stop-card-address">{stop.pointOfSale.address}</span>
                    <div className="stop-meta-row">
                      <span className="stop-time">
                        <Clock size={12} /> Llegada est: {stop.estimatedArrival}
                      </span>
                      {stop.orderTotal && (
                        <span className="stop-total">
                          ${stop.orderTotal.toLocaleString('es-AR')}
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                <div className="stop-card-right">
                  {isDelivery ? (
                    stop.status === 'PENDING' ? (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleMarkDelivered(stop.id);
                        }}
                        className="btn-action-delivery"
                        title="Marcar como entregado"
                      >
                        <CheckCircle2 size={16} />
                        <span>Entregar</span>
                      </button>
                    ) : (
                      <span className="done-pill">Entregado</span>
                    )
                  ) : (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        if (onStartOrderForStop) onStartOrderForStop(stop);
                      }}
                      className="btn-action-presale"
                    >
                      <ShoppingBag size={15} />
                      <span>Pedir</span>
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

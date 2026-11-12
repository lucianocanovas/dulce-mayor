import React, { useEffect, useRef } from 'react';
import L from 'leaflet';
import type { RouteStop } from '../types/models';
import { Locate } from 'lucide-react';

interface LeafletRouteMapProps {
  stops: RouteStop[];
  onSelectStop?: (stop: RouteStop) => void;
  selectedStopId?: string;
  isDeliveryRole?: boolean;
  onMarkDelivered?: (stopId: string) => void;
  driverLocation?: [number, number] | null;
}

export const LeafletRouteMap: React.FC<LeafletRouteMapProps> = ({
  stops,
  onSelectStop,
  selectedStopId,
  driverLocation,
}) => {
  const mapRef = useRef<L.Map | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const markersRef = useRef<{ [id: string]: L.Marker }>({});
  const driverMarkerRef = useRef<L.Marker | null>(null);
  const polylineRef = useRef<L.Polyline | null>(null);

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;

    // Center on Greater Mendoza
    const initialCenter: [number, number] = driverLocation || [-32.8950, -68.8400];
    const map = L.map(containerRef.current, {
      zoomControl: false,
    }).setView(initialCenter, 13);

    // Free OpenStreetMap Tiles
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap contributors',
    }).addTo(map);

    // Zoom control in top right for clean mobile layout
    L.control.zoom({ position: 'topright' }).addTo(map);

    mapRef.current = map;

    return () => {
      map.remove();
      mapRef.current = null;
    };
  }, []);

  // Update Markers, Driver GPS Pin and Polyline when stops or driverLocation change
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    // Clear old markers
    Object.values(markersRef.current).forEach((marker) => marker.remove());
    markersRef.current = {};

    if (driverMarkerRef.current) {
      driverMarkerRef.current.remove();
      driverMarkerRef.current = null;
    }

    if (polylineRef.current) {
      polylineRef.current.remove();
      polylineRef.current = null;
    }

    const allBoundsCoords: [number, number][] = [];
    const routeCoords: [number, number][] = [];

    // Render Driver GPS Live Pin if present
    if (driverLocation) {
      allBoundsCoords.push(driverLocation);
      routeCoords.push(driverLocation);

      const driverIcon = L.divIcon({
        className: 'custom-driver-pin',
        html: `
          <div style="position: relative; width: 36px; height: 36px; display: flex; align-items: center; justify-content: center;">
            <div style="
              position: absolute;
              width: 36px;
              height: 36px;
              border-radius: 50%;
              background: rgba(27, 68, 156, 0.25);
              animation: gpsPulse 2s infinite ease-out;
            "></div>
            <div style="
              width: 22px;
              height: 22px;
              border-radius: 50%;
              background: #1B449C;
              border: 3px solid #FFFFFF;
              box-shadow: 0 2px 8px rgba(15, 29, 56, 0.4);
              display: flex;
              align-items: center;
              justify-content: center;
              color: #FFFFFF;
              font-size: 11px;
            ">
              📍
            </div>
          </div>
        `,
        iconSize: [36, 36],
        iconAnchor: [18, 18],
        popupAnchor: [0, -18],
      });

      const driverMarker = L.marker(driverLocation, { icon: driverIcon, zIndexOffset: 1000 }).addTo(map);
      driverMarker.bindPopup(`
        <div style="font-family: system-ui, sans-serif; padding: 2px;">
          <strong style="color: #1B449C;">Tu Ubicación GPS (Repartidor)</strong>
          <p style="margin: 3px 0 0; font-size: 12px; color: #475569;">Punto de inicio de ruta optimizada</p>
        </div>
      `);
      driverMarkerRef.current = driverMarker;
    }

    // Render Stops
    stops.forEach((stop, index) => {
      const { pointOfSale, status } = stop;
      const latLng: [number, number] = [pointOfSale.latitude, pointOfSale.longitude];
      allBoundsCoords.push(latLng);
      routeCoords.push(latLng);

      const isDelivered = status === 'DELIVERED' || status === 'VISITED';
      const isSelected = selectedStopId === stop.id;

      // Color coding per Design System (Azul Real & Oro Caramelo)
      const bgColor = isDelivered ? '#16A34A' : isSelected ? '#C88D33' : '#1B449C';
      const displayIndex = stop.orderIndex || index + 1;

      // Custom Clean HTML Marker Icon with stop sequence number
      const customIcon = L.divIcon({
        className: 'custom-map-pin',
        html: `
          <div style="
            background-color: ${bgColor};
            color: #FFFFFF;
            width: 32px;
            height: 32px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 700;
            font-size: 13px;
            border: 2.5px solid #FFFFFF;
            box-shadow: 0 3px 8px rgba(15, 29, 56, 0.25);
            cursor: pointer;
            transition: transform 0.15s ease;
          ">
            ${isDelivered ? '✓' : displayIndex}
          </div>
        `,
        iconSize: [32, 32],
        iconAnchor: [16, 16],
        popupAnchor: [0, -18],
      });

      const marker = L.marker(latLng, { icon: customIcon }).addTo(map);

      // Popup Content
      const popupHtml = `
        <div style="font-family: system-ui, sans-serif; min-width: 180px; padding: 2px;">
          <div style="font-size: 11px; font-weight: 700; color: #1B449C; text-transform: uppercase;">
            Parada #${displayIndex}
          </div>
          <div style="font-size: 14px; font-weight: 700; color: #0F1D38; margin: 2px 0;">
            ${pointOfSale.name}
          </div>
          <div style="font-size: 12px; color: #475569; margin-bottom: 6px;">
            ${pointOfSale.address}, ${pointOfSale.city}
          </div>
          ${
            stop.orderTotal
              ? `<div style="font-size: 12px; font-weight: 600; color: #0F1D38; margin-bottom: 6px;">
                  Total: $${stop.orderTotal.toLocaleString('es-AR')}
                 </div>`
              : ''
          }
          <div style="font-size: 11px; padding: 2px 6px; border-radius: 4px; display: inline-block; font-weight: 600; ${
            isDelivered
              ? 'background: #F0FDF4; color: #16A34A;'
              : 'background: #FFFBEB; color: #D97706;'
          }">
            ${isDelivered ? '● Entregado / Visitado' : '⏳ Pendiente'}
          </div>
        </div>
      `;

      marker.bindPopup(popupHtml);

      marker.on('click', () => {
        if (onSelectStop) {
          onSelectStop(stop);
        }
      });

      markersRef.current[stop.id] = marker;
    });

    // Draw route polyline between driver and stops
    if (routeCoords.length > 1) {
      polylineRef.current = L.polyline(routeCoords, {
        color: '#1B449C',
        weight: 3.5,
        opacity: 0.85,
        dashArray: '6, 8',
      }).addTo(map);

      // Fit map bounds to encompass driver and all stops
      if (allBoundsCoords.length > 0) {
        map.fitBounds(L.latLngBounds(allBoundsCoords), {
          padding: [35, 35],
          maxZoom: 15,
        });
      }
    }
  }, [stops, selectedStopId, onSelectStop, driverLocation]);

  const handleCenterOnDriver = () => {
    if (mapRef.current && driverLocation) {
      mapRef.current.setView(driverLocation, 15, { animate: true });
    }
  };

  return (
    <div className="map-wrapper" style={{ position: 'relative' }}>
      <div ref={containerRef} className="leaflet-map-container" />
      {driverLocation && (
        <button
          type="button"
          onClick={handleCenterOnDriver}
          className="map-center-btn"
          title="Centrar en mi ubicación GPS"
          style={{
            position: 'absolute',
            bottom: '12px',
            right: '12px',
            zIndex: 500,
            backgroundColor: '#FFFFFF',
            border: '1px solid #E2E8F0',
            borderRadius: '50%',
            width: '40px',
            height: '40px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 2px 6px rgba(0,0,0,0.2)',
            cursor: 'pointer',
            color: '#1B449C',
          }}
        >
          <Locate size={20} />
        </button>
      )}
    </div>
  );
};

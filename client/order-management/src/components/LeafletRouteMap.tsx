import React, { useEffect, useRef } from 'react';
import L from 'leaflet';
import type { RouteStop } from '../types/models';

interface LeafletRouteMapProps {
  stops: RouteStop[];
  onSelectStop?: (stop: RouteStop) => void;
  selectedStopId?: string;
  isDeliveryRole?: boolean;
  onMarkDelivered?: (stopId: string) => void;
}

export const LeafletRouteMap: React.FC<LeafletRouteMapProps> = ({
  stops,
  onSelectStop,
  selectedStopId,
}) => {
  const mapRef = useRef<L.Map | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const markersRef = useRef<{ [id: string]: L.Marker }>({});
  const polylineRef = useRef<L.Polyline | null>(null);

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;

    // Center on Greater Mendoza
    const initialCenter: [number, number] = [-32.8950, -68.8400];
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

  // Update Markers and Polyline when stops change
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    // Clear old markers
    Object.values(markersRef.current).forEach((marker) => marker.remove());
    markersRef.current = {};

    if (polylineRef.current) {
      polylineRef.current.remove();
      polylineRef.current = null;
    }

    const latLngs: [number, number][] = [];

    stops.forEach((stop, index) => {
      const { pointOfSale, status } = stop;
      const latLng: [number, number] = [pointOfSale.latitude, pointOfSale.longitude];
      latLngs.push(latLng);

      const isDelivered = status === 'DELIVERED' || status === 'VISITED';
      const isSelected = selectedStopId === stop.id;

      // Color coding per Design System
      const bgColor = isDelivered ? '#28A745' : isSelected ? '#D85817' : '#F58721';

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
            box-shadow: 0 3px 8px rgba(43,24,16,0.3);
            cursor: pointer;
            transition: transform 0.15s ease;
          ">
            ${isDelivered ? '✓' : index + 1}
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
          <div style="font-size: 11px; font-weight: 700; color: #D85817; text-transform: uppercase;">
            Parada #${index + 1}
          </div>
          <div style="font-size: 14px; font-weight: 700; color: #2B1810; margin: 2px 0;">
            ${pointOfSale.name}
          </div>
          <div style="font-size: 12px; color: #665349; margin-bottom: 6px;">
            ${pointOfSale.address}, ${pointOfSale.city}
          </div>
          ${
            stop.orderTotal
              ? `<div style="font-size: 12px; font-weight: 600; color: #2B1810; margin-bottom: 6px;">
                  Total: $${stop.orderTotal.toLocaleString('es-AR')}
                 </div>`
              : ''
          }
          <div style="font-size: 11px; padding: 2px 6px; border-radius: 4px; display: inline-block; font-weight: 600; ${
            isDelivered
              ? 'background: #E8F5E9; color: #28A745;'
              : 'background: #FFF8E1; color: #E69500;'
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

    // Draw route polyline between stops
    if (latLngs.length > 1) {
      polylineRef.current = L.polyline(latLngs, {
        color: '#F58721',
        weight: 3.5,
        opacity: 0.85,
        dashArray: '6, 8',
      }).addTo(map);

      // Fit map bounds to encompass all stops
      map.fitBounds(L.latLngBounds(latLngs), {
        padding: [30, 30],
        maxZoom: 15,
      });
    }
  }, [stops, selectedStopId, onSelectStop]);

  return (
    <div className="map-wrapper">
      <div ref={containerRef} className="leaflet-map-container" />
    </div>
  );
};

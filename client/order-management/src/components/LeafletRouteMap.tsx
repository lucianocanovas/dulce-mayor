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

/**
 * Calculates compass bearing in degrees from point A to point B (for directional arrow markers).
 */
function calculateBearing(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const toDeg = (rad: number) => (rad * 180) / Math.PI;
  const dLon = toRad(lon2 - lon1);
  const y = Math.sin(dLon) * Math.cos(toRad(lat2));
  const x =
    Math.cos(toRad(lat1)) * Math.sin(toRad(lat2)) -
    Math.sin(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.cos(dLon);
  return (toDeg(Math.atan2(y, x)) + 360) % 360;
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
  const routeLayersRef = useRef<L.Layer[]>([]);
  const hasFittedBoundsRef = useRef<boolean>(false);

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;

    const initialCenter: [number, number] = driverLocation || [-32.8950, -68.8400];
    const map = L.map(containerRef.current, {
      zoomControl: false,
    }).setView(initialCenter, 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap contributors',
    }).addTo(map);

    L.control.zoom({ position: 'topright' }).addTo(map);
    mapRef.current = map;

    return () => {
      map.remove();
      mapRef.current = null;
      hasFittedBoundsRef.current = false;
    };
  }, []);

  // Update Markers, Driver GPS Pin and Sequential Optimal Route
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    const abortController = new AbortController();

    // Clear old markers and route layers
    Object.values(markersRef.current).forEach((marker) => marker.remove());
    markersRef.current = {};

    if (driverMarkerRef.current) {
      driverMarkerRef.current.remove();
      driverMarkerRef.current = null;
    }

    routeLayersRef.current.forEach((layer) => layer.remove());
    routeLayersRef.current = [];

    const allBoundsCoords: [number, number][] = [];

    // 1. Render Driver GPS Live Pin
    if (driverLocation) {
      allBoundsCoords.push(driverLocation);

      const driverIcon = L.divIcon({
        className: 'custom-driver-pin',
        html: `
          <div style="position: relative; width: 34px; height: 34px; display: flex; align-items: center; justify-content: center;">
            <div style="
              position: absolute;
              width: 34px;
              height: 34px;
              border-radius: 50%;
              background: rgba(27, 68, 156, 0.28);
              animation: gpsPulse 2s infinite ease-out;
            "></div>
            <div style="
              width: 18px;
              height: 18px;
              border-radius: 50%;
              background: #1B449C;
              border: 3px solid #FFFFFF;
              box-shadow: 0 2px 8px rgba(15, 29, 56, 0.45);
            "></div>
          </div>
        `,
        iconSize: [34, 34],
        iconAnchor: [17, 17],
        popupAnchor: [0, -16],
      });

      const driverMarker = L.marker(driverLocation, { icon: driverIcon, zIndexOffset: 1000 }).addTo(map);
      driverMarker.bindPopup(`
        <div style="font-family: system-ui, sans-serif; padding: 2px;">
          <strong style="color: #1B449C;">Tu Ubicación GPS</strong>
          <p style="margin: 2px 0 0; font-size: 12px; color: #475569;">Inicio del recorrido óptimo</p>
        </div>
      `);
      driverMarkerRef.current = driverMarker;
    }

    // 2. Separate pending stops (ordered by orderIndex) from delivered stops
    const pendingOrdered = stops
      .filter((s) => s.status !== 'DELIVERED' && s.status !== 'VISITED')
      .sort((a, b) => (a.orderIndex || 999) - (b.orderIndex || 999));

    const deliveredList = stops.filter(
      (s) => s.status === 'DELIVERED' || s.status === 'VISITED'
    );

    // 3. Render all stop markers
    const renderStopMarker = (stop: RouteStop, badgeText: string, isDelivered: boolean, isNextTarget: boolean) => {
      const { pointOfSale } = stop;
      const latLng: [number, number] = [pointOfSale.latitude, pointOfSale.longitude];
      allBoundsCoords.push(latLng);

      const isSelected = selectedStopId === stop.id;
      const bgColor = isDelivered
        ? '#16A34A'
        : isNextTarget
        ? '#C88D33'
        : isSelected
        ? '#0F1D38'
        : '#1B449C';

      const customIcon = L.divIcon({
        className: 'custom-map-pin',
        html: `
          <div style="
            background-color: ${bgColor};
            color: #FFFFFF;
            width: ${isNextTarget ? '34px' : '28px'};
            height: ${isNextTarget ? '34px' : '28px'};
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 800;
            font-size: ${isNextTarget ? '14px' : '12px'};
            border: 2.5px solid #FFFFFF;
            box-shadow: 0 3px 8px rgba(15, 29, 56, 0.3);
            cursor: pointer;
          ">
            ${badgeText}
          </div>
        `,
        iconSize: isNextTarget ? [34, 34] : [28, 28],
        iconAnchor: isNextTarget ? [17, 17] : [14, 14],
        popupAnchor: [0, -16],
      });

      const marker = L.marker(latLng, {
        icon: customIcon,
        zIndexOffset: isNextTarget ? 500 : isDelivered ? 100 : 300,
      }).addTo(map);

      const popupHtml = `
        <div style="font-family: system-ui, sans-serif; min-width: 170px; padding: 2px;">
          <div style="font-size: 11px; font-weight: 700; color: ${isDelivered ? '#16A34A' : '#1B449C'}; text-transform: uppercase;">
            ${isDelivered ? 'Entregado' : isNextTarget ? `Próxima Parada #${badgeText}` : `Parada #${badgeText}`}
          </div>
          <div style="font-size: 13px; font-weight: 700; color: #0F1D38; margin: 2px 0;">
            ${pointOfSale.name}
          </div>
          <div style="font-size: 11px; color: #475569; margin-bottom: 4px;">
            ${pointOfSale.address}, ${pointOfSale.city}
          </div>
          ${
            stop.orderTotal
              ? `<div style="font-size: 12px; font-weight: 700; color: #0F1D38;">
                  Total: $${stop.orderTotal.toLocaleString('es-AR')}
                 </div>`
              : ''
          }
        </div>
      `;

      marker.bindPopup(popupHtml);
      marker.on('click', () => {
        if (onSelectStop) onSelectStop(stop);
      });

      markersRef.current[stop.id] = marker;
    };

    deliveredList.forEach((stop) => renderStopMarker(stop, '✓', true, false));
    pendingOrdered.forEach((stop, idx) =>
      renderStopMarker(stop, String(stop.orderIndex || idx + 1), false, idx === 0)
    );

    // 4. Build sequential waypoints ONLY for pending stops starting from driverLocation
    const waypoints: [number, number][] = [];
    if (driverLocation) waypoints.push(driverLocation);
    pendingOrdered.forEach((s) => {
      waypoints.push([s.pointOfSale.latitude, s.pointOfSale.longitude]);
    });

    const drawStraightFallback = () => {
      if (waypoints.length < 2) return;
      for (let i = 0; i < waypoints.length - 1; i++) {
        const from = waypoints[i];
        const to = waypoints[i + 1];
        const isFirstLeg = i === 0 && Boolean(driverLocation);

        const segLine = L.polyline([from, to], {
          color: isFirstLeg ? '#C88D33' : '#1B449C',
          weight: isFirstLeg ? 4.5 : 3,
          opacity: isFirstLeg ? 0.95 : 0.7,
          dashArray: isFirstLeg ? undefined : '6, 8',
        }).addTo(map);
        routeLayersRef.current.push(segLine);

        // Add directional arrow at midpoint so order is unmistakable
        const midLat = (from[0] + to[0]) / 2;
        const midLon = (from[1] + to[1]) / 2;
        const bearing = calculateBearing(from[0], from[1], to[0], to[1]);

        const arrowIcon = L.divIcon({
          className: 'route-direction-arrow',
          html: `<div style="
            transform: rotate(${bearing - 90}deg);
            color: ${isFirstLeg ? '#C88D33' : '#1B449C'};
            font-size: 14px;
            font-weight: 900;
            text-shadow: 0 0 3px #FFFFFF, 0 0 3px #FFFFFF;
            line-height: 1;
          ">➤</div>`,
          iconSize: [16, 16],
          iconAnchor: [8, 8],
        });
        const arrowMarker = L.marker([midLat, midLon], { icon: arrowIcon, interactive: false }).addTo(map);
        routeLayersRef.current.push(arrowMarker);
      }
    };

    const drawOSRMRoute = async () => {
      if (waypoints.length < 2) return;
      try {
        const coordsParam = waypoints.map(([lat, lng]) => `${lng},${lat}`).join(';');
        const url = `https://router.project-osrm.org/route/v1/driving/${coordsParam}?overview=full&geometries=geojson&steps=false`;
        const res = await fetch(url, { signal: abortController.signal });
        if (!res.ok) throw new Error('OSRM status ' + res.status);
        const data = await res.json();

        if (!data.routes || data.routes.length === 0) {
          drawStraightFallback();
          return;
        }

        const route = data.routes[0];
        const legs = route.legs || [];

        // If legs are available via steps or full geometry, draw full street geometry
        const fullCoords: [number, number][] = route.geometry.coordinates.map(
          ([lng, lat]: [number, number]) => [lat, lng]
        );

        // Outer border for crisp contrast on OSM map
        const outlineLine = L.polyline(fullCoords, {
          color: '#FFFFFF',
          weight: 7,
          opacity: 0.85,
        }).addTo(map);
        routeLayersRef.current.push(outlineLine);

        // Main street-following route line
        const mainLine = L.polyline(fullCoords, {
          color: '#1B449C',
          weight: 4.5,
          opacity: 0.9,
        }).addTo(map);
        routeLayersRef.current.push(mainLine);

        // Also highlight the direct leg to Stop #1 if we have waypoints
        if (driverLocation && pendingOrdered.length > 0 && legs.length > 0) {
          // Fetch just the first leg for distinct gold highlight so next destination stands out
          const firstLegUrl = `https://router.project-osrm.org/route/v1/driving/${driverLocation[1]},${driverLocation[0]};${pendingOrdered[0].pointOfSale.longitude},${pendingOrdered[0].pointOfSale.latitude}?overview=full&geometries=geojson`;
          const legRes = await fetch(firstLegUrl, { signal: abortController.signal });
          if (legRes.ok) {
            const legData = await legRes.json();
            if (legData.routes?.[0]?.geometry?.coordinates) {
              const firstLegCoords: [number, number][] = legData.routes[0].geometry.coordinates.map(
                ([lng, lat]: [number, number]) => [lat, lng]
              );
              const nextLegLine = L.polyline(firstLegCoords, {
                color: '#C88D33',
                weight: 5,
                opacity: 0.95,
              }).addTo(map);
              routeLayersRef.current.push(nextLegLine);
            }
          }
        }
      } catch (err: unknown) {
        if ((err as Error)?.name !== 'AbortError') {
          drawStraightFallback();
        }
      }
    };

    if (waypoints.length >= 2) {
      drawOSRMRoute();
    }

    // Fit bounds on first meaningful render
    if (allBoundsCoords.length > 0 && !hasFittedBoundsRef.current) {
      map.fitBounds(L.latLngBounds(allBoundsCoords), {
        padding: [32, 32],
        maxZoom: 15,
      });
      hasFittedBoundsRef.current = true;
    }

    return () => {
      abortController.abort();
    };
  }, [stops, selectedStopId, onSelectStop, driverLocation]);

  const handleCenterOnDriver = () => {
    if (mapRef.current && driverLocation) {
      mapRef.current.setView(driverLocation, 15, { animate: true });
    }
  };

  return (
    <div className="map-wrapper" style={{ position: 'relative' }}>
      <div ref={containerRef} className="leaflet-map-container" />
      {/* Map Legend Pill */}
      <div
        style={{
          position: 'absolute',
          top: '10px',
          left: '10px',
          zIndex: 500,
          backgroundColor: 'rgba(255, 255, 255, 0.94)',
          border: '1px solid #E2E8F0',
          borderRadius: '8px',
          padding: '4px 8px',
          fontSize: '0.68rem',
          fontWeight: 700,
          color: '#0F1D38',
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          boxShadow: '0 2px 6px rgba(0,0,0,0.12)',
        }}
      >
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '3px' }}>
          <span style={{ width: 9, height: 3, background: '#C88D33', display: 'inline-block', borderRadius: 2 }} />
          Próxima (#1)
        </span>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '3px' }}>
          <span style={{ width: 9, height: 3, background: '#1B449C', display: 'inline-block', borderRadius: 2 }} />
          Siguientes
        </span>
      </div>

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

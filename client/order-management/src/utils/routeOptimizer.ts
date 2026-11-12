import type { RouteStop } from '../types/models';

/**
 * Calculates the Haversine distance in kilometers between two GPS coordinates.
 */
export function haversineDistanceKm(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  const R = 6371; // Earth radius in km
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

export interface RouteOptimizationResult {
  optimizedStops: RouteStop[];
  totalDistanceKm: number;
  originalDistanceKm: number;
  savingsKm: number;
  savingsPercent: number;
  estimatedDurationMin: number;
}

/**
 * Calculates total route distance from start position through all stops in given order.
 */
function calculateTotalDistance(
  startLat: number,
  startLon: number,
  stops: RouteStop[]
): number {
  if (stops.length === 0) return 0;
  let dist = haversineDistanceKm(
    startLat,
    startLon,
    stops[0].pointOfSale.latitude,
    stops[0].pointOfSale.longitude
  );

  for (let i = 0; i < stops.length - 1; i++) {
    const s1 = stops[i].pointOfSale;
    const s2 = stops[i + 1].pointOfSale;
    dist += haversineDistanceKm(s1.latitude, s1.longitude, s2.latitude, s2.longitude);
  }
  return dist;
}

/**
 * Optimizes the delivery route from the driver's current GPS position
 * using a Greedy Nearest Neighbor heuristic followed by 2-Opt edge swaps.
 */
export function optimizeDeliveryRoute(
  driverLat: number,
  driverLon: number,
  stops: RouteStop[]
): RouteOptimizationResult {
  if (stops.length <= 1) {
    const dist = stops.length === 1
      ? haversineDistanceKm(driverLat, driverLon, stops[0].pointOfSale.latitude, stops[0].pointOfSale.longitude)
      : 0;
    return {
      optimizedStops: stops.map((s, idx) => ({ ...s, orderIndex: idx + 1 })),
      totalDistanceKm: Math.round(dist * 10) / 10,
      originalDistanceKm: Math.round(dist * 10) / 10,
      savingsKm: 0,
      savingsPercent: 0,
      estimatedDurationMin: Math.round(dist * 2.5 + stops.length * 8),
    };
  }

  const originalDistance = calculateTotalDistance(driverLat, driverLon, stops);

  // 1. Nearest Neighbor heuristic starting from driver coordinates
  const unvisited = [...stops];
  const ordered: RouteStop[] = [];
  let currentLat = driverLat;
  let currentLon = driverLon;

  while (unvisited.length > 0) {
    let nearestIdx = 0;
    let minD = Infinity;

    for (let i = 0; i < unvisited.length; i++) {
      const pos = unvisited[i].pointOfSale;
      const d = haversineDistanceKm(currentLat, currentLon, pos.latitude, pos.longitude);
      if (d < minD) {
        minD = d;
        nearestIdx = i;
      }
    }

    const nextStop = unvisited.splice(nearestIdx, 1)[0];
    ordered.push(nextStop);
    currentLat = nextStop.pointOfSale.latitude;
    currentLon = nextStop.pointOfSale.longitude;
  }

  // 2. 2-Opt local search refinement to uncross subpaths
  let improved = true;
  let iterations = 0;
  const maxIterations = 50;

  while (improved && iterations < maxIterations) {
    improved = false;
    iterations++;

    for (let i = 0; i < ordered.length - 1; i++) {
      for (let k = i + 1; k < ordered.length; k++) {
        const prevLat = i === 0 ? driverLat : ordered[i - 1].pointOfSale.latitude;
        const prevLon = i === 0 ? driverLon : ordered[i - 1].pointOfSale.longitude;

        const nodeA = ordered[i].pointOfSale;
        const nodeB = ordered[k].pointOfSale;
        const nextNode = k + 1 < ordered.length ? ordered[k + 1].pointOfSale : null;

        const currentDist =
          haversineDistanceKm(prevLat, prevLon, nodeA.latitude, nodeA.longitude) +
          (nextNode ? haversineDistanceKm(nodeB.latitude, nodeB.longitude, nextNode.latitude, nextNode.longitude) : 0);

        const newDist =
          haversineDistanceKm(prevLat, prevLon, nodeB.latitude, nodeB.longitude) +
          (nextNode ? haversineDistanceKm(nodeA.latitude, nodeA.longitude, nextNode.latitude, nextNode.longitude) : 0);

        if (newDist < currentDist - 0.001) {
          // Reverse segment between i and k
          const segment = ordered.slice(i, k + 1).reverse();
          ordered.splice(i, segment.length, ...segment);
          improved = true;
        }
      }
    }
  }

  const optimizedDistance = calculateTotalDistance(driverLat, driverLon, ordered);
  const savingsKm = Math.max(0, originalDistance - optimizedDistance);
  const savingsPercent = originalDistance > 0 ? Math.round((savingsKm / originalDistance) * 100) : 0;

  // Urban Mendoza estimation: ~25km/h speed + 8 minutes average drop-off per stop
  const travelTimeMin = (optimizedDistance / 25) * 60;
  const deliveryHandlingTimeMin = ordered.length * 8;
  const estimatedDurationMin = Math.round(travelTimeMin + deliveryHandlingTimeMin);

  return {
    optimizedStops: ordered.map((stop, idx) => ({
      ...stop,
      orderIndex: idx + 1,
    })),
    totalDistanceKm: Math.round(optimizedDistance * 10) / 10,
    originalDistanceKm: Math.round(originalDistance * 10) / 10,
    savingsKm: Math.round(savingsKm * 10) / 10,
    savingsPercent,
    estimatedDurationMin,
  };
}

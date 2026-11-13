export interface GeocodeResult {
  latitude: number;
  longitude: number;
  displayName: string;
}

/**
 * Geocodes a street address in Mendoza, Argentina using OpenStreetMap Nominatim.
 * Returns null if no valid match is found.
 */
export async function geocodeAddressWithOSM(
  address: string,
  city: string,
  neighborhood?: string
): Promise<GeocodeResult | null> {
  const cleanAddress = address.trim();
  if (cleanAddress.length < 4) return null;

  const queries = [
    [cleanAddress, neighborhood?.trim(), city, 'Mendoza', 'Argentina'].filter(Boolean).join(', '),
    [cleanAddress, city, 'Mendoza', 'Argentina'].filter(Boolean).join(', '),
    [cleanAddress, 'Mendoza', 'Argentina'].join(', '),
  ];

  const seen = new Set<string>();
  for (const q of queries) {
    if (seen.has(q)) continue;
    seen.add(q);

    try {
      const url = `https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=ar&q=${encodeURIComponent(q)}`;
      const res = await fetch(url, {
        headers: {
          Accept: 'application/json',
        },
      });
      if (!res.ok) continue;

      const data = (await res.json()) as Array<{
        lat?: string;
        lon?: string;
        display_name?: string;
      }>;

      if (Array.isArray(data) && data.length > 0) {
        const lat = Number.parseFloat(data[0].lat ?? '');
        const lon = Number.parseFloat(data[0].lon ?? '');
        if (Number.isFinite(lat) && Number.isFinite(lon)) {
          return {
            latitude: lat,
            longitude: lon,
            displayName: data[0].display_name || q,
          };
        }
      }
    } catch {
      // Try next fallback query
    }
  }

  return null;
}

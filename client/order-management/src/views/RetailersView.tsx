import React, { useState, useEffect } from 'react';
import type { PointOfSale } from '../types/models';
import { fetchPointsOfSale, createPointOfSaleOnBackend } from '../services/api';
import { geocodeAddressWithOSM } from '../utils/geocoding';
import { Plus, MapPin, Phone, User, Check, Search, Navigation, Loader2, ShoppingBag, CheckCircle2, AlertCircle } from 'lucide-react';

interface RetailersViewProps {
  onSelectPOS?: (pos: PointOfSale) => void;
}

export const RetailersView: React.FC<RetailersViewProps> = ({ onSelectPOS }) => {
  const [retailers, setRetailers] = useState<PointOfSale[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // New POS form state
  const [name, setName] = useState('');
  const [ownerName, setOwnerName] = useState('');
  const [taxIdOrDni, setTaxIdOrDni] = useState('');
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');
  const [neighborhood, setNeighborhood] = useState('');
  const [city, setCity] = useState('Mendoza Capital');
  const [latitude, setLatitude] = useState<number>(-32.8895);
  const [longitude, setLongitude] = useState<number>(-68.8458);
  const [latInput, setLatInput] = useState<string>('-32.8895');
  const [lonInput, setLonInput] = useState<string>('-68.8458');
  const [coordSource, setCoordSource] = useState<'idle' | 'searching' | 'osm' | 'gps' | 'manual' | 'not_found'>('idle');
  const [osmMatchLabel, setOsmMatchLabel] = useState<string>('');
  const [geoErrorMsg, setGeoErrorMsg] = useState<string | null>(null);
  const [isLocating, setIsLocating] = useState(false);

  useEffect(() => {
    async function loadRetailers() {
      setLoading(true);
      try {
        const data = await fetchPointsOfSale();
        setRetailers(data);
      } finally {
        setLoading(false);
      }
    }
    loadRetailers();
  }, []);

  useEffect(() => {
    if (!showForm) return;
    const cleanAddr = address.trim();
    if (cleanAddr.length < 5) {
      if (coordSource === 'osm' || coordSource === 'not_found') {
        setCoordSource('idle');
        setOsmMatchLabel('');
      }
      return;
    }

    if (coordSource === 'gps' || coordSource === 'manual') return;

    let cancelled = false;
    const timer = setTimeout(async () => {
      setCoordSource('searching');
      const result = await geocodeAddressWithOSM(cleanAddr, city, neighborhood);
      if (cancelled) return;
      if (result) {
        setLatitude(result.latitude);
        setLongitude(result.longitude);
        setLatInput(result.latitude.toFixed(5));
        setLonInput(result.longitude.toFixed(5));
        setOsmMatchLabel(result.displayName);
        setCoordSource('osm');
        setGeoErrorMsg(null);
      } else {
        setOsmMatchLabel('');
        setCoordSource('not_found');
      }
    }, 700);

    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [address, city, neighborhood, showForm]);

  const handleCaptureGPS = () => {
    setIsLocating(true);
    setGeoErrorMsg(null);
    if ('geolocation' in navigator) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setLatitude(pos.coords.latitude);
          setLongitude(pos.coords.longitude);
          setLatInput(pos.coords.latitude.toFixed(5));
          setLonInput(pos.coords.longitude.toFixed(5));
          setCoordSource('gps');
          setOsmMatchLabel('');
          setIsLocating(false);
        },
        () => {
          setGeoErrorMsg('No se pudo obtener el GPS del dispositivo. Ingresá las coordenadas manualmente.');
          setCoordSource('not_found');
          setIsLocating(false);
        },
        { timeout: 5000 }
      );
    } else {
      setGeoErrorMsg('Tu navegador no soporta geolocalización. Ingresá las coordenadas manualmente.');
      setCoordSource('not_found');
      setIsLocating(false);
    }
  };

  const handleManualLatChange = (val: string) => {
    setLatInput(val);
    setCoordSource('manual');
    setGeoErrorMsg(null);
    const parsed = Number.parseFloat(val);
    if (Number.isFinite(parsed)) setLatitude(parsed);
  };

  const handleManualLonChange = (val: string) => {
    setLonInput(val);
    setCoordSource('manual');
    setGeoErrorMsg(null);
    const parsed = Number.parseFloat(val);
    if (Number.isFinite(parsed)) setLongitude(parsed);
  };

  const handleCreateRetailer = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !address.trim()) return;

    setIsSubmitting(true);
    setGeoErrorMsg(null);

    let finalLat = Number.parseFloat(latInput);
    let finalLon = Number.parseFloat(lonInput);

    try {
      if (coordSource !== 'gps' && coordSource !== 'manual' && coordSource !== 'osm') {
        const result = await geocodeAddressWithOSM(address.trim(), city, neighborhood);
        if (result) {
          finalLat = result.latitude;
          finalLon = result.longitude;
          setLatitude(finalLat);
          setLongitude(finalLon);
          setLatInput(finalLat.toFixed(5));
          setLonInput(finalLon.toFixed(5));
          setCoordSource('osm');
        } else {
          setCoordSource('not_found');
          setGeoErrorMsg('No se encontró la dirección en OpenStreetMap. Cargá Latitud y Longitud a mano o usá "Capturar GPS Actual".');
          setIsSubmitting(false);
          return;
        }
      }

      if (!Number.isFinite(finalLat) || !Number.isFinite(finalLon)) {
        setGeoErrorMsg('Ingresá coordenadas válidas de Latitud y Longitud.');
        setIsSubmitting(false);
        return;
      }

      const created = await createPointOfSaleOnBackend({
        storeName: name.trim(),
        ownerName: ownerName.trim() || 'Titular',
        taxIdOrDni: taxIdOrDni.trim() || '20-00000000-0',
        phone: phone.trim() || '261-4200000',
        address: address.trim(),
        neighborhood: neighborhood.trim() || 'Centro',
        city,
        latitude: finalLat,
        longitude: finalLon,
      });

      setRetailers((prev) => [created, ...prev]);
      setSuccessMsg(`¡Comercio "${created.name}" registrado en el sistema!`);
      setShowForm(false);

      // Reset form
      setName('');
      setOwnerName('');
      setTaxIdOrDni('');
      setPhone('');
      setAddress('');
      setNeighborhood('');
      setCoordSource('idle');
      setOsmMatchLabel('');
      setLatInput('-32.8895');
      setLonInput('-68.8458');

      setTimeout(() => setSuccessMsg(null), 3000);
    } catch (err) {
      console.error(err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const filteredRetailers = retailers.filter((r) => {
    const q = searchQuery.toLowerCase();
    return (
      r.name.toLowerCase().includes(q) ||
      r.ownerName.toLowerCase().includes(q) ||
      r.address.toLowerCase().includes(q) ||
      r.neighborhood.toLowerCase().includes(q) ||
      r.city.toLowerCase().includes(q)
    );
  });

  return (
    <div className="view-container">
      {/* Top Action & Search */}
      <div className="retailers-header">
        <div className="search-box">
          <Search size={18} className="search-icon" />
          <input
            type="text"
            placeholder="Buscar por kiosco, titular o dirección..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="search-input"
          />
        </div>

        <button
          onClick={() => setShowForm(!showForm)}
          className={`btn ${showForm ? 'btn-secondary' : 'btn-primary'} new-pos-toggle`}
        >
          <Plus size={18} />
          <span>{showForm ? 'Cancelar' : 'Nuevo Comercio'}</span>
        </button>
      </div>

      {successMsg && (
        <div className="toast-banner success">
          <Check size={18} />
          <span>{successMsg}</span>
        </div>
      )}

      {/* New Retailer Modal / Expandable Form */}
      {showForm && (
        <form onSubmit={handleCreateRetailer} className="new-pos-card">
          <h3 className="form-title">Alta de Nuevo Punto de Venta</h3>
          <p className="form-subtitle">Ingresá los datos del comercio minorista y su geolocalización</p>

          <div className="form-group">
            <label>Nombre de Fantasía / Kiosco *</label>
            <input
              type="text"
              placeholder="Ej. Kiosco Don Pedro"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              className="form-control"
            />
          </div>

          <div className="form-row-2">
            <div className="form-group">
              <label>Titular / Responsable</label>
              <input
                type="text"
                placeholder="Nombre y apellido"
                value={ownerName}
                onChange={(e) => setOwnerName(e.target.value)}
                className="form-control"
              />
            </div>
            <div className="form-group">
              <label>CUIT / DNI</label>
              <input
                type="text"
                placeholder="20-xxxxxxxx-x"
                value={taxIdOrDni}
                onChange={(e) => setTaxIdOrDni(e.target.value)}
                className="form-control"
              />
            </div>
          </div>

          <div className="form-row-2">
            <div className="form-group">
              <label>Teléfono de Contacto</label>
              <input
                type="tel"
                placeholder="261-xxxxxxx"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className="form-control"
              />
            </div>
            <div className="form-group">
              <label>Departamento / Ciudad</label>
              <select
                value={city}
                onChange={(e) => setCity(e.target.value)}
                className="form-control"
              >
                <option value="Mendoza Capital">Mendoza Capital</option>
                <option value="Godoy Cruz">Godoy Cruz</option>
                <option value="Guaymallén">Guaymallén</option>
                <option value="Maipú">Maipú</option>
                <option value="Las Heras">Las Heras</option>
                <option value="Luján de Cuyo">Luján de Cuyo</option>
              </select>
            </div>
          </div>

          <div className="form-row-2">
            <div className="form-group">
              <label>Dirección (Calle y Número) *</label>
              <input
                type="text"
                placeholder="Ej. San Martín 1250"
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                required
                className="form-control"
              />
            </div>
            <div className="form-group">
              <label>Barrio</label>
              <input
                type="text"
                placeholder="Ej. Bombal, Centro"
                value={neighborhood}
                onChange={(e) => setNeighborhood(e.target.value)}
                className="form-control"
              />
            </div>
          </div>

          {/* GPS & OpenStreetMap Coordinates */}
          <div className="gps-capture-box" style={{ flexDirection: 'column', alignItems: 'stretch', gap: '0.6rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '0.5rem' }}>
              <span className="gps-label">Ubicación en OpenStreetMap / GPS</span>
              <button
                type="button"
                onClick={handleCaptureGPS}
                disabled={isLocating}
                className="btn btn-sm btn-secondary gps-btn"
              >
                <Navigation size={14} className={isLocating ? 'animate-spin' : ''} />
                <span>{isLocating ? 'Obteniendo GPS...' : 'Capturar GPS Actual'}</span>
              </button>
            </div>

            {coordSource === 'searching' && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.75rem', color: '#2563EB', fontWeight: 600 }}>
                <Loader2 size={13} className="animate-spin" />
                <span>Buscando dirección en OpenStreetMap...</span>
              </div>
            )}
            {coordSource === 'osm' && (
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.35rem', fontSize: '0.75rem', color: '#15803D', fontWeight: 600 }}>
                <CheckCircle2 size={14} style={{ flexShrink: 0, marginTop: '1px' }} />
                <span>Ubicación encontrada en OpenStreetMap{osmMatchLabel ? `: ${osmMatchLabel}` : ''}</span>
              </div>
            )}
            {coordSource === 'not_found' && (
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.35rem', fontSize: '0.75rem', color: '#B45309', fontWeight: 600 }}>
                <AlertCircle size={14} style={{ flexShrink: 0, marginTop: '1px' }} />
                <span>No se encontró en OpenStreetMap. Cargá las coordenadas a mano o usá "Capturar GPS Actual".</span>
              </div>
            )}
            {coordSource === 'gps' && (
              <div style={{ fontSize: '0.75rem', color: '#1B449C', fontWeight: 600 }}>
                Usando coordenadas actuales del dispositivo ({latitude.toFixed(5)}, {longitude.toFixed(5)}).
              </div>
            )}
            {geoErrorMsg && (
              <div style={{ fontSize: '0.75rem', color: '#DC2626', fontWeight: 600 }}>
                {geoErrorMsg}
              </div>
            )}

            <div className="form-row-2">
              <div className="form-group">
                <label>Latitud</label>
                <input
                  type="number"
                  step="any"
                  value={latInput}
                  onChange={(e) => handleManualLatChange(e.target.value)}
                  placeholder="-32.8895"
                  className="form-control"
                />
              </div>
              <div className="form-group">
                <label>Longitud</label>
                <input
                  type="number"
                  step="any"
                  value={lonInput}
                  onChange={(e) => handleManualLonChange(e.target.value)}
                  placeholder="-68.8458"
                  className="form-control"
                />
              </div>
            </div>
          </div>

          <div className="form-actions">
            <button
              type="button"
              onClick={() => setShowForm(false)}
              className="btn btn-secondary"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="btn btn-primary"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="animate-spin" size={16} />
                  <span>Guardando...</span>
                </>
              ) : (
                <>
                  <Check size={16} />
                  <span>Guardar Comercio</span>
                </>
              )}
            </button>
          </div>
        </form>
      )}

      {/* Retailers List */}
      <div className="retailers-list">
        <div className="list-count-badge">
          <span>{filteredRetailers.length} comercios registrados</span>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '2rem' }}>
            <Loader2 className="animate-spin" size={30} style={{ color: 'var(--dm-primary-orange)', margin: '0 auto 0.5rem' }} />
            <p style={{ color: 'var(--dm-text-subtle)' }}>Cargando comercios desde la base de datos...</p>
          </div>
        ) : (
          filteredRetailers.map((pos) => (
            <div key={pos.id} className="pos-card">
              <div className="pos-card-top">
                <div>
                  <h4 className="pos-name">{pos.name}</h4>
                  <div className="pos-meta-owner">
                    <User size={13} />
                    <span>{pos.ownerName}</span>
                    <span className="meta-dot">•</span>
                    <span>{pos.taxIdOrDni}</span>
                  </div>
                </div>
                {pos.hasActiveOrder && (
                  <span className="order-active-badge">Pedido Activo</span>
                )}
              </div>

              <div className="pos-details">
                <div className="pos-detail-row">
                  <MapPin size={14} className="detail-icon" />
                  <span>{pos.address}, {pos.neighborhood} ({pos.city})</span>
                </div>
                <div className="pos-detail-row">
                  <Phone size={14} className="detail-icon" />
                  <span>{pos.phone}</span>
                </div>
              </div>

              {onSelectPOS && (
                <div style={{ marginTop: '0.65rem', paddingTop: '0.5rem', borderTop: '1px solid #F1F5F9' }}>
                  <button
                    type="button"
                    onClick={() => onSelectPOS(pos)}
                    style={{
                      width: '100%',
                      background: '#1B449C',
                      color: '#FFFFFF',
                      border: 'none',
                      borderRadius: '8px',
                      padding: '0.45rem',
                      fontSize: '0.78rem',
                      fontWeight: 800,
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '0.35rem',
                    }}
                  >
                    <ShoppingBag size={14} />
                    <span>Tomar Pedido para este Comercio</span>
                  </button>
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
};

import React, { useState, useEffect } from 'react';
import type { PointOfSale } from '../types/models';
import { fetchPointsOfSale, createPointOfSaleOnBackend } from '../services/api';
import { Plus, MapPin, Phone, User, Check, Search, Navigation, Loader2 } from 'lucide-react';

export const RetailersView: React.FC = () => {
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

  const handleCaptureGPS = () => {
    setIsLocating(true);
    if ('geolocation' in navigator) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setLatitude(pos.coords.latitude);
          setLongitude(pos.coords.longitude);
          setIsLocating(false);
        },
        () => {
          // Default to random offset in Mendoza
          setLatitude(-32.8900 + (Math.random() - 0.5) * 0.02);
          setLongitude(-68.8400 + (Math.random() - 0.5) * 0.02);
          setIsLocating(false);
        },
        { timeout: 5000 }
      );
    } else {
      setIsLocating(false);
    }
  };

  const handleCreateRetailer = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !address.trim()) return;

    setIsSubmitting(true);
    try {
      const created = await createPointOfSaleOnBackend({
        storeName: name.trim(),
        ownerName: ownerName.trim() || 'Titular',
        taxIdOrDni: taxIdOrDni.trim() || '20-00000000-0',
        phone: phone.trim() || '261-4200000',
        address: address.trim(),
        neighborhood: neighborhood.trim() || 'Centro',
        city,
        latitude,
        longitude,
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

          {/* GPS Coordinates Capture */}
          <div className="gps-capture-box">
            <div className="gps-coords">
              <span className="gps-label">Coordenadas GPS:</span>
              <span className="gps-val">{latitude.toFixed(5)}, {longitude.toFixed(5)}</span>
            </div>
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
            </div>
          ))
        )}
      </div>
    </div>
  );
};

import React, { useState } from 'react';
import type { PointOfSale } from '../types/models';
import { getStoredPOS, saveStoredPOS } from '../services/api';
import { Plus, MapPin, Phone, User, Check, Search, Navigation } from 'lucide-react';

export const RetailersView: React.FC = () => {
  const [retailers, setRetailers] = useState<PointOfSale[]>(getStoredPOS());
  const [showForm, setShowForm] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

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

  const handleCreateRetailer = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !address.trim()) return;

    const newPos: PointOfSale = {
      id: `pos-${Date.now()}`,
      name: name.trim(),
      ownerName: ownerName.trim() || 'Titular sin especificar',
      taxIdOrDni: taxIdOrDni.trim() || '20-00000000-0',
      phone: phone.trim() || '261-0000000',
      address: address.trim(),
      neighborhood: neighborhood.trim() || 'Centro',
      city,
      latitude,
      longitude,
      hasActiveOrder: false,
    };

    const updated = [newPos, ...retailers];
    setRetailers(updated);
    saveStoredPOS(updated);
    setShowForm(false);

    // Reset Form
    setName('');
    setOwnerName('');
    setTaxIdOrDni('');
    setPhone('');
    setAddress('');
    setNeighborhood('');

    setSuccessMsg(`Comercio "${newPos.name}" dado de alta con éxito en el sistema.`);
    setTimeout(() => setSuccessMsg(null), 3500);
  };

  const filtered = retailers.filter(
    (r) =>
      r.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      r.address.toLowerCase().includes(searchQuery.toLowerCase()) ||
      r.ownerName.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="view-container">
      {/* Header and Toggle */}
      <div className="retailers-header">
        <div>
          <h2 className="view-title">Comercios & Kioscos</h2>
          <span className="view-subtitle">{retailers.length} comercios registrados</span>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          className="btn-primary btn-sm"
        >
          <Plus size={16} />
          <span>{showForm ? 'Ver Lista' : 'Nuevo Comercio'}</span>
        </button>
      </div>

      {successMsg && (
        <div className="order-toast-success">
          <Check size={16} />
          <span>{successMsg}</span>
        </div>
      )}

      {showForm ? (
        /* Onboarding Form */
        <div className="onboarding-card">
          <h3 className="form-card-title">Dar de Alta Comercio Minorista</h3>
          <p className="form-card-desc">
            Registra los datos del nuevo punto de venta para incorporarlo a las rutas de preventa y reparto.
          </p>

          <form onSubmit={handleCreateRetailer} className="onboarding-form">
            <div className="form-group">
              <label className="form-label">Nombre de Fantasía del Kiosco / Comercio *</label>
              <input
                type="text"
                placeholder="ej: Kiosco La Esquina"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="form-input"
                required
              />
            </div>

            <div className="form-row-2">
              <div className="form-group">
                <label className="form-label">Nombre del Titular</label>
                <input
                  type="text"
                  placeholder="ej: Osvaldo Giménez"
                  value={ownerName}
                  onChange={(e) => setOwnerName(e.target.value)}
                  className="form-input"
                />
              </div>
              <div className="form-group">
                <label className="form-label">CUIT o DNI</label>
                <input
                  type="text"
                  placeholder="20-12345678-9"
                  value={taxIdOrDni}
                  onChange={(e) => setTaxIdOrDni(e.target.value)}
                  className="form-input"
                />
              </div>
            </div>

            <div className="form-row-2">
              <div className="form-group">
                <label className="form-label">Teléfono de Contacto</label>
                <input
                  type="tel"
                  placeholder="261-4123456"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className="form-input"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Departamento / Ciudad</label>
                <select
                  value={city}
                  onChange={(e) => setCity(e.target.value)}
                  className="form-input"
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
                <label className="form-label">Calle y Número *</label>
                <input
                  type="text"
                  placeholder="ej: Av. San Martín 850"
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  className="form-input"
                  required
                />
              </div>
              <div className="form-group">
                <label className="form-label">Barrio</label>
                <input
                  type="text"
                  placeholder="ej: Bombal"
                  value={neighborhood}
                  onChange={(e) => setNeighborhood(e.target.value)}
                  className="form-input"
                />
              </div>
            </div>

            {/* GPS Location Button */}
            <div className="gps-capture-box">
              <div className="gps-info">
                <MapPin size={16} className="gps-icon" />
                <span>
                  Coordenadas: {latitude.toFixed(4)}, {longitude.toFixed(4)}
                </span>
              </div>
              <button
                type="button"
                onClick={handleCaptureGPS}
                className="btn-gps"
                disabled={isLocating}
              >
                <Navigation size={14} />
                <span>{isLocating ? 'Obteniendo...' : 'Capturar GPS Actual'}</span>
              </button>
            </div>

            <div className="form-actions-row">
              <button
                type="button"
                onClick={() => setShowForm(false)}
                className="btn-outline"
              >
                Cancelar
              </button>
              <button type="submit" className="btn-primary">
                Guardar Comercio
              </button>
            </div>
          </form>
        </div>
      ) : (
        /* Search and POS List */
        <>
          <div className="search-box">
            <Search size={16} className="search-icon" />
            <input
              type="text"
              placeholder="Buscar por nombre, calle o titular..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="search-input"
            />
          </div>

          <div className="retailers-list">
            {filtered.map((pos) => (
              <div key={pos.id} className="retailer-card">
                <div className="retailer-card-body">
                  <div className="retailer-name-row">
                    <h4 className="retailer-name">{pos.name}</h4>
                    <span className="city-pill">{pos.city}</span>
                  </div>
                  <span className="retailer-address">
                    <MapPin size={13} /> {pos.address} ({pos.neighborhood})
                  </span>
                  <div className="retailer-owner-row">
                    <span className="owner-text">
                      <User size={13} /> {pos.ownerName}
                    </span>
                    <span className="phone-text">
                      <Phone size={13} /> {pos.phone}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
};

import React, { useState } from 'react';
import type { PointOfSale } from '../types/models';
import { createPointOfSaleOnBackend } from '../services/api';
import { Store, X, Loader2, Navigation } from 'lucide-react';

interface NewPointOfSaleModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (newPos: PointOfSale) => void;
}

export const NewPointOfSaleModal: React.FC<NewPointOfSaleModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [storeName, setStoreName] = useState('');
  const [ownerName, setOwnerName] = useState('');
  const [taxIdOrDni, setTaxIdOrDni] = useState('');
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');
  const [neighborhood, setNeighborhood] = useState('');
  const [city, setCity] = useState('Mendoza Capital');
  const [latitude, setLatitude] = useState<number>(-32.8895);
  const [longitude, setLongitude] = useState<number>(-68.8458);
  const [isLocating, setIsLocating] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  if (!isOpen) return null;

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
          // Default to slight variation in Greater Mendoza
          setLatitude(-32.8900 + (Math.random() - 0.5) * 0.02);
          setLongitude(-68.8400 + (Math.random() - 0.5) * 0.02);
          setIsLocating(false);
        },
        { timeout: 6000 }
      );
    } else {
      setIsLocating(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!storeName.trim() || !address.trim()) {
      setErrorMsg('Por favor ingresá al menos el nombre del comercio y su dirección.');
      return;
    }

    setIsSubmitting(true);
    setErrorMsg(null);
    try {
      const created = await createPointOfSaleOnBackend({
        storeName: storeName.trim(),
        ownerName: ownerName.trim() || 'Titular',
        taxIdOrDni: taxIdOrDni.trim() || '20-00000000-0',
        phone: phone.trim() || '261-4000000',
        address: address.trim(),
        neighborhood: neighborhood.trim() || 'Centro',
        city,
        latitude,
        longitude,
      });

      onSuccess(created);
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al registrar el comercio.';
      setErrorMsg(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="pos-modal-overlay" role="dialog" aria-modal="true" aria-label="Nuevo Comercio">
      <div className="pos-modal-sheet">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <div style={{
              background: '#1B449C',
              color: '#FFFFFF',
              width: '32px',
              height: '32px',
              borderRadius: '8px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}>
              <Store size={18} />
            </div>
            <div>
              <h3 style={{ margin: 0, fontSize: '1.05rem', fontWeight: 800, color: '#0F1D38' }}>
                Registrar Nuevo Comercio
              </h3>
              <span style={{ fontSize: '0.78rem', color: '#64748B' }}>
                Carga in situ para preventistas de Dulce Mayor
              </span>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              color: '#64748B',
              padding: '4px',
            }}
          >
            <X size={20} />
          </button>
        </div>

        {errorMsg && (
          <div style={{
            background: '#FEE2E2',
            color: '#991B1B',
            padding: '0.55rem 0.75rem',
            borderRadius: '8px',
            fontSize: '0.8rem',
            marginBottom: '0.85rem',
          }}>
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: '#1B449C', marginBottom: '0.2rem' }}>
              Nombre del Local / Kiosco *
            </label>
            <input
              type="text"
              required
              placeholder="Ej: Kiosco Dulce Tentación"
              value={storeName}
              onChange={(e) => setStoreName(e.target.value)}
              className="form-control"
              style={{ width: '100%', padding: '0.55rem', borderRadius: '8px', border: '1px solid #CBD5E1', fontSize: '0.88rem' }}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: '#475569', marginBottom: '0.2rem' }}>
                Dueño / Contacto
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type="text"
                  placeholder="Ej: Roberto Páez"
                  value={ownerName}
                  onChange={(e) => setOwnerName(e.target.value)}
                  style={{ width: '100%', padding: '0.55rem', borderRadius: '8px', border: '1px solid #CBD5E1', fontSize: '0.88rem' }}
                />
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: '#475569', marginBottom: '0.2rem' }}>
                CUIT o DNI
              </label>
              <input
                type="text"
                placeholder="20-33444555-9"
                value={taxIdOrDni}
                onChange={(e) => setTaxIdOrDni(e.target.value)}
                style={{ width: '100%', padding: '0.55rem', borderRadius: '8px', border: '1px solid #CBD5E1', fontSize: '0.88rem' }}
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: '#475569', marginBottom: '0.2rem' }}>
                Teléfono / WhatsApp
              </label>
              <input
                type="tel"
                placeholder="261-5551234"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                style={{ width: '100%', padding: '0.55rem', borderRadius: '8px', border: '1px solid #CBD5E1', fontSize: '0.88rem' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: '#475569', marginBottom: '0.2rem' }}>
                Municipio / Departamento
              </label>
              <select
                value={city}
                onChange={(e) => setCity(e.target.value)}
                style={{ width: '100%', padding: '0.55rem', borderRadius: '8px', border: '1px solid #CBD5E1', fontSize: '0.88rem', background: '#FFFFFF' }}
              >
                <option value="Mendoza Capital">Mendoza Capital</option>
                <option value="Godoy Cruz">Godoy Cruz</option>
                <option value="Guaymallén">Guaymallén</option>
                <option value="Las Heras">Las Heras</option>
                <option value="Maipú">Maipú</option>
                <option value="Luján de Cuyo">Luján de Cuyo</option>
              </select>
            </div>
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: '#1B449C', marginBottom: '0.2rem' }}>
              Dirección y Número *
            </label>
            <input
              type="text"
              required
              placeholder="Ej: Av. San Martín 850"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              style={{ width: '100%', padding: '0.55rem', borderRadius: '8px', border: '1px solid #CBD5E1', fontSize: '0.88rem' }}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: '#475569', marginBottom: '0.2rem' }}>
              Barrio / Referencia
            </label>
            <input
              type="text"
              placeholder="Ej: Bombal, Centro, 5ta Sección"
              value={neighborhood}
              onChange={(e) => setNeighborhood(e.target.value)}
              style={{ width: '100%', padding: '0.55rem', borderRadius: '8px', border: '1px solid #CBD5E1', fontSize: '0.88rem' }}
            />
          </div>

          <div style={{
            background: '#F8FAFC',
            padding: '0.65rem 0.85rem',
            borderRadius: '8px',
            border: '1px dashed #CBD5E1',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}>
            <div>
              <div style={{ fontSize: '0.78rem', fontWeight: 700, color: '#0F1D38' }}>Geolocalización GPS</div>
              <div style={{ fontSize: '0.72rem', color: '#64748B' }}>
                Lat: {latitude.toFixed(4)}, Lon: {longitude.toFixed(4)}
              </div>
            </div>
            <button
              type="button"
              onClick={handleCaptureGPS}
              disabled={isLocating}
              style={{
                background: '#FFFFFF',
                border: '1px solid #CBD5E1',
                borderRadius: '6px',
                padding: '0.35rem 0.65rem',
                fontSize: '0.75rem',
                fontWeight: 700,
                color: '#1B449C',
                display: 'flex',
                alignItems: 'center',
                gap: '0.3rem',
                cursor: 'pointer',
              }}
            >
              {isLocating ? <Loader2 size={13} className="animate-spin" /> : <Navigation size={13} />}
              <span>{isLocating ? 'Capturando...' : 'GPS Actual'}</span>
            </button>
          </div>

          <div style={{ display: 'flex', gap: '0.65rem', marginTop: '0.75rem' }}>
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              style={{
                flex: 1,
                padding: '0.7rem',
                borderRadius: '8px',
                border: '1px solid #CBD5E1',
                background: '#FFFFFF',
                color: '#475569',
                fontWeight: 700,
                fontSize: '0.85rem',
                cursor: 'pointer',
              }}
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              style={{
                flex: 2,
                padding: '0.7rem',
                borderRadius: '8px',
                border: 'none',
                background: '#1B449C',
                color: '#FFFFFF',
                fontWeight: 800,
                fontSize: '0.88rem',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '0.4rem',
                boxShadow: '0 3px 8px rgba(27, 68, 156, 0.3)',
              }}
            >
              {isSubmitting ? (
                <>
                  <Loader2 size={16} className="animate-spin" />
                  <span>Guardando...</span>
                </>
              ) : (
                <>
                  <Store size={16} />
                  <span>Guardar y Seleccionar</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

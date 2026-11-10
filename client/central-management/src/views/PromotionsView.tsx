import React, { useEffect, useState } from 'react';
import { apiRequest } from '../services/api';
import { Tag, Plus, Trash2, CheckCircle2, XCircle, X, AlertCircle, Eye, Edit2, AlertTriangle } from 'lucide-react';

interface Promotion {
  id: string;
  code: string;
  description: string;
  discountPercentage: number;
  minimumQuantity: number | null;
  startDate: string;
  endDate: string;
  productId: string | null;
  productName: string | null;
  active: boolean;
}

interface SimpleProduct {
  id: string;
  name: string;
  code: string;
}

export const PromotionsView: React.FC = () => {
  const [promotions, setPromotions] = useState<Promotion[]>([]);
  const [products, setProducts] = useState<SimpleProduct[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Modals
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [viewPromo, setViewPromo] = useState<Promotion | null>(null);
  const [editPromo, setEditPromo] = useState<Promotion | null>(null);
  const [deletePromo, setDeletePromo] = useState<Promotion | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Form State
  const [code, setCode] = useState('');
  const [description, setDescription] = useState('');
  const [discountPercentage, setDiscountPercentage] = useState<number>(10);
  const [minimumQuantity, setMinimumQuantity] = useState<number>(1);
  const [startDate, setStartDate] = useState(new Date().toISOString().split('T')[0]);
  const [endDate, setEndDate] = useState(
    new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  );
  const [productId, setProductId] = useState<string>('');

  const fetchPromotions = async () => {
    try {
      setLoading(true);
      const data = await apiRequest<Promotion[]>('/promotions');
      setPromotions(data);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Error al cargar promociones');
      }
    } finally {
      setLoading(false);
    }
  };

  const fetchProducts = async () => {
    try {
      const data = await apiRequest<SimpleProduct[]>('/products');
      setProducts(data);
    } catch {
      // Non-blocking
    }
  };

  useEffect(() => {
    fetchPromotions();
    fetchProducts();
  }, []);

  const openCreateModal = () => {
    resetForm();
    setIsModalOpen(true);
  };

  const openEditModal = (p: Promotion) => {
    setEditPromo(p);
    setCode(p.code);
    setDescription(p.description || '');
    setDiscountPercentage(p.discountPercentage);
    setMinimumQuantity(p.minimumQuantity || 1);
    setStartDate(p.startDate || new Date().toISOString().split('T')[0]);
    setEndDate(p.endDate || new Date().toISOString().split('T')[0]);
    setProductId(p.productId || '');
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      await apiRequest<Promotion>('/promotions', {
        method: 'POST',
        body: JSON.stringify({
          code,
          description,
          discountPercentage,
          minimumQuantity,
          startDate,
          endDate,
          productId: productId || null,
        }),
      });

      setIsModalOpen(false);
      resetForm();
      fetchPromotions();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Error al crear la promoción');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editPromo) return;
    setSubmitting(true);
    setError(null);

    try {
      await apiRequest<Promotion>(`/promotions/${editPromo.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          code,
          description,
          discountPercentage,
          minimumQuantity,
          startDate,
          endDate,
          productId: productId || null,
        }),
      });

      setEditPromo(null);
      resetForm();
      fetchPromotions();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Error al actualizar la promoción');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!deletePromo) return;
    setSubmitting(true);
    try {
      await apiRequest<void>(`/promotions/${deletePromo.id}`, {
        method: 'DELETE',
      });
      setDeletePromo(null);
      fetchPromotions();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Error al eliminar promoción');
    } finally {
      setSubmitting(false);
    }
  };

  const resetForm = () => {
    setCode('');
    setDescription('');
    setDiscountPercentage(10);
    setMinimumQuantity(1);
    setStartDate(new Date().toISOString().split('T')[0]);
    setEndDate(new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]);
    setProductId('');
  };

  return (
    <div className="promotions-view">
      <div className="view-header">
        <div>
          <h2>Módulo de Promociones y Descuentos Mayoristas</h2>
          <p className="text-muted">Cupones de descuento, combos por volumen y promociones comerciales vigentes.</p>
        </div>
        <div className="header-actions">
          <button
            type="button"
            onClick={openCreateModal}
            className="btn btn-primary"
          >
            <Plus size={16} /> Nueva Promoción
          </button>
          <button onClick={fetchPromotions} className="btn btn-secondary">
            Actualizar
          </button>
        </div>
      </div>

      {error && (
        <div style={{ background: '#fee2e2', border: '1px solid #ef4444', color: '#b91c1c', padding: '0.75rem', borderRadius: '8px', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="view-loading">
          <p>Cargando promociones...</p>
        </div>
      ) : (
        <div className="panel">
          <div className="panel-body no-padding">
            {promotions.length === 0 ? (
              <p className="text-muted text-center py-8">
                No hay promociones registradas en este momento.
              </p>
            ) : (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Código Cupón</th>
                    <th>Descripción Comercial</th>
                    <th>Descuento</th>
                    <th>Mín. Bultos</th>
                    <th>Vigencia</th>
                    <th>Alcance</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {promotions.map((p) => {
                    const isExpired = new Date(p.endDate) < new Date();
                    return (
                      <tr key={p.id}>
                        <td>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                            <Tag size={15} className="text-primary" />
                            <span className="font-mono font-bold">{p.code}</span>
                          </div>
                        </td>
                        <td>{p.description}</td>
                        <td className="font-bold text-success" style={{ fontSize: '1.05rem' }}>
                          {p.discountPercentage}% OFF
                        </td>
                        <td>{p.minimumQuantity ? `${p.minimumQuantity} bultos` : '1'}</td>
                        <td className="text-xs text-muted">
                          <div>Desde: {p.startDate}</div>
                          <div>Hasta: {p.endDate}</div>
                        </td>
                        <td>
                          {p.productName ? (
                            <span className="badge badge-neutral">{p.productName}</span>
                          ) : (
                            <span className="badge badge-success">Catálogo General</span>
                          )}
                        </td>
                        <td>
                          {isExpired ? (
                            <span className="status-indicator warning">
                              <XCircle size={14} /> Vencida
                            </span>
                          ) : (
                            <span className="status-indicator good">
                              <CheckCircle2 size={14} /> Activa
                            </span>
                          )}
                        </td>
                        <td>
                          <div className="action-btn-group">
                            <button
                              onClick={() => setViewPromo(p)}
                              className="action-icon-btn view"
                              title="Ver detalles"
                            >
                              <Eye size={14} />
                              <span>Ver</span>
                            </button>
                            <button
                              onClick={() => openEditModal(p)}
                              className="action-icon-btn edit"
                              title="Editar promoción"
                            >
                              <Edit2 size={14} />
                              <span>Editar</span>
                            </button>
                            <button
                              onClick={() => setDeletePromo(p)}
                              className="action-icon-btn delete"
                              title="Eliminar promoción"
                            >
                              <Trash2 size={14} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        </div>
      )}

      {/* Modal: VER DETALLE */}
      {viewPromo && (
        <div className="modal-backdrop" onClick={() => setViewPromo(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header-row">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Tag size={20} className="text-primary" />
                <h3 style={{ margin: 0 }}>Cupón {viewPromo.code}</h3>
              </div>
              <button onClick={() => setViewPromo(null)} className="modal-close-btn">
                <X size={20} />
              </button>
            </div>

            <div style={{ background: 'var(--primary-light)', padding: '1rem', borderRadius: '8px', marginBottom: '1.25rem', textAlign: 'center' }}>
              <span style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--primary)' }}>
                {viewPromo.discountPercentage}% OFF
              </span>
              <p style={{ margin: 0, fontWeight: 600, color: 'var(--text-main)' }}>{viewPromo.description}</p>
            </div>

            <div className="details-grid">
              <div className="detail-item">
                <span className="detail-label">Compra Mínima</span>
                <span className="detail-value">{viewPromo.minimumQuantity || 1} bultos</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Alcance de Aplicación</span>
                <span className="detail-value">{viewPromo.productName || 'Todos los productos'}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Fecha de Inicio</span>
                <span className="detail-value">{viewPromo.startDate}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Fecha de Vencimiento</span>
                <span className="detail-value">{viewPromo.endDate}</span>
              </div>
            </div>

            <div className="modal-actions">
              <button
                onClick={() => {
                  const p = viewPromo;
                  setViewPromo(null);
                  openEditModal(p);
                }}
                className="btn btn-primary"
              >
                <Edit2 size={16} /> Editar Promoción
              </button>
              <button onClick={() => setViewPromo(null)} className="btn btn-secondary">
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: CREAR PROMOCIÓN */}
      {isModalOpen && (
        <div className="modal-backdrop" onClick={() => setIsModalOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header-row">
              <h3>Crear Nueva Promoción</h3>
              <button onClick={() => setIsModalOpen(false)} className="modal-close-btn">
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleCreate} className="modal-form">
              <label>
                Código del Cupón (ej. BIENVENIDA, VERANO2026) *
                <input
                  type="text"
                  value={code}
                  onChange={(e) => setCode(e.target.value.toUpperCase())}
                  placeholder="CODIGO10"
                  required
                />
              </label>

              <label>
                Descripción Comercial *
                <input
                  type="text"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="10% de descuento en la primera compra"
                  required
                />
              </label>

              <div className="grid-cols-2 gap-4">
                <label>
                  % de Descuento *
                  <input
                    type="number"
                    min="1"
                    max="100"
                    value={discountPercentage}
                    onChange={(e) => setDiscountPercentage(Number(e.target.value))}
                    required
                  />
                </label>

                <label>
                  Cantidad Mínima de Bultos *
                  <input
                    type="number"
                    min="1"
                    value={minimumQuantity}
                    onChange={(e) => setMinimumQuantity(Number(e.target.value))}
                    required
                  />
                </label>
              </div>

              <div className="grid-cols-2 gap-4">
                <label>
                  Fecha Inicio *
                  <input
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    required
                  />
                </label>

                <label>
                  Fecha Fin *
                  <input
                    type="date"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    required
                  />
                </label>
              </div>

              <label>
                Producto Específico (Opcional - vacío para toda la tienda):
                <select
                  value={productId}
                  onChange={(e) => setProductId(e.target.value)}
                  style={{ width: '100%', padding: '0.65rem', borderRadius: '6px', border: '1px solid var(--border)' }}
                >
                  <option value="">Aplica a todo el catálogo</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({p.code})
                    </option>
                  ))}
                </select>
              </label>

              <div className="modal-actions">
                <button type="submit" disabled={submitting} className="btn btn-primary">
                  {submitting ? 'Guardando...' : 'Crear Promoción'}
                </button>
                <button type="button" onClick={() => setIsModalOpen(false)} className="btn btn-secondary">
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: EDITAR PROMOCIÓN */}
      {editPromo && (
        <div className="modal-backdrop" onClick={() => setEditPromo(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header-row">
              <h3>Editar Promoción ({editPromo.code})</h3>
              <button onClick={() => setEditPromo(null)} className="modal-close-btn">
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleUpdate} className="modal-form">
              <label>
                Código del Cupón *
                <input
                  type="text"
                  value={code}
                  onChange={(e) => setCode(e.target.value.toUpperCase())}
                  required
                />
              </label>

              <label>
                Descripción Comercial *
                <input
                  type="text"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  required
                />
              </label>

              <div className="grid-cols-2 gap-4">
                <label>
                  % de Descuento *
                  <input
                    type="number"
                    min="1"
                    max="100"
                    value={discountPercentage}
                    onChange={(e) => setDiscountPercentage(Number(e.target.value))}
                    required
                  />
                </label>

                <label>
                  Cantidad Mínima de Bultos *
                  <input
                    type="number"
                    min="1"
                    value={minimumQuantity}
                    onChange={(e) => setMinimumQuantity(Number(e.target.value))}
                    required
                  />
                </label>
              </div>

              <div className="grid-cols-2 gap-4">
                <label>
                  Fecha Inicio *
                  <input
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    required
                  />
                </label>

                <label>
                  Fecha Fin *
                  <input
                    type="date"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    required
                  />
                </label>
              </div>

              <label>
                Producto Específico:
                <select
                  value={productId}
                  onChange={(e) => setProductId(e.target.value)}
                  style={{ width: '100%', padding: '0.65rem', borderRadius: '6px', border: '1px solid var(--border)' }}
                >
                  <option value="">Aplica a todo el catálogo</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({p.code})
                    </option>
                  ))}
                </select>
              </label>

              <div className="modal-actions">
                <button type="submit" disabled={submitting} className="btn btn-primary">
                  {submitting ? 'Guardando...' : 'Guardar Cambios'}
                </button>
                <button type="button" onClick={() => setEditPromo(null)} className="btn btn-secondary">
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: ELIMINAR PROMOCIÓN */}
      {deletePromo && (
        <div className="modal-backdrop" onClick={() => setDeletePromo(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1rem', color: 'var(--danger)' }}>
              <AlertTriangle size={28} />
              <h3 style={{ margin: 0, color: 'var(--text-main)' }}>¿Eliminar Promoción?</h3>
            </div>
            <p style={{ marginBottom: '1rem', color: 'var(--text-muted)' }}>
              ¿Estás seguro de que deseas eliminar el cupón <strong>{deletePromo.code}</strong> ({deletePromo.discountPercentage}% OFF)? Los clientes y preventistas ya no podrán aplicarlo.
            </p>
            <div className="modal-actions">
              <button
                type="button"
                onClick={handleDelete}
                disabled={submitting}
                className="btn btn-danger-outline"
                style={{ background: 'var(--danger)', color: '#fff', border: 'none' }}
              >
                {submitting ? 'Eliminando...' : 'Sí, Eliminar'}
              </button>
              <button
                type="button"
                onClick={() => setDeletePromo(null)}
                className="btn btn-secondary"
              >
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

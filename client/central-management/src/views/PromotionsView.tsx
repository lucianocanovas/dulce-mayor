import React, { useEffect, useState } from 'react';
import { apiRequest } from '../services/api';
import { Tag, Plus, Trash2, Calendar, CheckCircle2, XCircle, X, AlertCircle } from 'lucide-react';

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
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // Form
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

  const handleDelete = async (id: string, code: string) => {
    if (!window.confirm(`¿Está seguro de eliminar la promoción '${code}'?`)) {
      return;
    }

    try {
      await apiRequest<void>(`/promotions/${id}`, {
        method: 'DELETE',
      });
      fetchPromotions();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Error al eliminar promoción');
    }
  };

  const resetForm = () => {
    setCode('');
    setDescription('');
    setDiscountPercentage(10);
    setMinimumQuantity(1);
    setProductId('');
  };

  return (
    <div className="promotions-view">
      <div className="view-header">
        <div>
          <h2>Módulo de Promociones y Descuentos</h2>
          <p className="text-muted">Cupones de descuento, combos por volumen y promociones vigentes.</p>
        </div>
        <button
          type="button"
          onClick={() => setIsModalOpen(true)}
          className="btn btn-primary flex items-center gap-2"
        >
          <Plus size={18} />
          Nueva Promoción
        </button>
      </div>

      {error && (
        <div className="login-error mb-4">
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      <div className="panel">
        {loading ? (
          <div className="p-8 text-center text-muted">Cargando promociones vigentes...</div>
        ) : promotions.length === 0 ? (
          <div className="p-8 text-center text-muted">
            <Tag size={48} className="mx-auto mb-2 opacity-50" />
            No hay promociones activas registradas en el catálogo.
          </div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Código de Cupón</th>
                <th>Descripción</th>
                <th>Descuento</th>
                <th>Mínimo Unidades</th>
                <th>Alcance de Producto</th>
                <th>Vigencia</th>
                <th>Estado</th>
                <th className="text-right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {promotions.map((p) => (
                <tr key={p.id}>
                  <td>
                    <span className="font-mono font-bold bg-primary-soft text-primary px-2 py-1 rounded">
                      {p.code}
                    </span>
                  </td>
                  <td>{p.description || 'Sin descripción'}</td>
                  <td>
                    <span className="font-bold text-success text-base">
                      {p.discountPercentage}% OFF
                    </span>
                  </td>
                  <td>{p.minimumQuantity ? `${p.minimumQuantity} u.` : '1 u.'}</td>
                  <td>
                    {p.productName ? (
                      <span className="badge badge-info">{p.productName}</span>
                    ) : (
                      <span className="badge">Todo el catálogo</span>
                    )}
                  </td>
                  <td>
                    <div className="text-xs flex items-center gap-1 text-muted">
                      <Calendar size={13} />
                      {p.startDate} al {p.endDate}
                    </div>
                  </td>
                  <td>
                    {p.active ? (
                      <span className="badge badge-success flex items-center gap-1">
                        <CheckCircle2 size={12} /> Activa
                      </span>
                    ) : (
                      <span className="badge badge-warning flex items-center gap-1">
                        <XCircle size={12} /> Vencida
                      </span>
                    )}
                  </td>
                  <td className="text-right">
                    <button
                      type="button"
                      onClick={() => handleDelete(p.id, p.code)}
                      className="btn-icon text-danger"
                      title="Eliminar promoción"
                    >
                      <Trash2 size={16} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Modal Nueva Promoción */}
      {isModalOpen && (
        <div className="modal-backdrop">
          <div className="modal-dialog">
            <div className="modal-header">
              <div className="flex items-center gap-2">
                <Tag className="text-primary" size={22} />
                <h3 className="font-bold">Crear Nueva Promoción / Cupón</h3>
              </div>
              <button
                type="button"
                onClick={() => {
                  setIsModalOpen(false);
                  resetForm();
                }}
                className="btn-icon"
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleCreate}>
              <div className="modal-body space-y-4">
                <div className="form-row grid grid-cols-2 gap-4">
                  <label className="form-group">
                    <span className="form-label">Código del Cupón:</span>
                    <input
                      type="text"
                      value={code}
                      onChange={(e) => setCode(e.target.value.toUpperCase())}
                      placeholder="ej. PRIMAVERA20"
                      required
                    />
                  </label>
                  <label className="form-group">
                    <span className="form-label">% Descuento:</span>
                    <input
                      type="number"
                      min="1"
                      max="100"
                      value={discountPercentage}
                      onChange={(e) => setDiscountPercentage(Number(e.target.value))}
                      required
                    />
                  </label>
                </div>

                <label className="form-group">
                  <span className="form-label">Descripción de la Promoción:</span>
                  <input
                    type="text"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder="ej. 15% de descuento en pedidos mayores a 5 packs"
                    required
                  />
                </label>

                <div className="form-row grid grid-cols-2 gap-4">
                  <label className="form-group">
                    <span className="form-label">Cantidad Mínima de Unidades:</span>
                    <input
                      type="number"
                      min="1"
                      value={minimumQuantity}
                      onChange={(e) => setMinimumQuantity(Number(e.target.value))}
                      required
                    />
                  </label>
                  <label className="form-group">
                    <span className="form-label">Producto Específico (Opcional):</span>
                    <select
                      value={productId}
                      onChange={(e) => setProductId(e.target.value)}
                    >
                      <option value="">Aplica a todos los productos</option>
                      {products.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.name} ({p.code})
                        </option>
                      ))}
                    </select>
                  </label>
                </div>

                <div className="form-row grid grid-cols-2 gap-4">
                  <label className="form-group">
                    <span className="form-label">Fecha Inicio:</span>
                    <input
                      type="date"
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      required
                    />
                  </label>
                  <label className="form-group">
                    <span className="form-label">Fecha Fin:</span>
                    <input
                      type="date"
                      value={endDate}
                      onChange={(e) => setEndDate(e.target.value)}
                      required
                    />
                  </label>
                </div>
              </div>

              <div className="modal-footer flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => {
                    setIsModalOpen(false);
                    resetForm();
                  }}
                  className="btn btn-secondary"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="btn btn-primary"
                >
                  {submitting ? 'Guardando...' : 'Crear Promoción'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

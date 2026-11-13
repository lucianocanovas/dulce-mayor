import React, { useEffect, useState } from 'react';
import { apiRequest } from '../services/api';
import {
  Plus,
  Search,
  RefreshCw,
  AlertCircle,
  CheckCircle,
  Package,
  Eye,
  Edit2,
  Trash2,
  Upload,
  Link,
  X,
  AlertTriangle,
} from 'lucide-react';

interface ProductItem {
  id: string;
  code: string;
  name: string;
  description: string;
  packaging: string;
  unitsPerPack: number;
  onSale: boolean;
  subCategoryId?: string;
  subCategoryName: string;
  categoryName: string;
  currentPrice: number;
  basePriceUsd?: number;
  exchangeRate?: number;
  currentStock: number;
  imageUrl?: string;
  imageBase64?: string;
}

interface SubCategoryOption {
  id: string;
  name: string;
  categoryId?: string;
  categoryName?: string;
}

export const ProductsView: React.FC = () => {
  const [products, setProducts] = useState<ProductItem[]>([]);
  const [subCategories, setSubCategories] = useState<SubCategoryOption[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [modalError, setModalError] = useState<string | null>(null);

  // Modals state
  const [viewProduct, setViewProduct] = useState<ProductItem | null>(null);
  const [editProduct, setEditProduct] = useState<ProductItem | null>(null);
  const [deleteProduct, setDeleteProduct] = useState<ProductItem | null>(null);
  const [stockProduct, setStockProduct] = useState<ProductItem | null>(null);
  const [showAddModal, setShowAddModal] = useState(false);

  // Stock Adjustment Form
  const [adjustMode, setAdjustMode] = useState<'add' | 'subtract'>('add');
  const [adjustQty, setAdjustQty] = useState(50);
  const [adjustNote, setAdjustNote] = useState('Ingreso de mercadería desde proveedor');
  const [isAdjusting, setIsAdjusting] = useState(false);

  // New / Edit Product Form State
  const [formData, setFormData] = useState({
    code: '',
    name: '',
    description: '',
    packaging: 'Pack x 12 u.',
    unitsPerPack: 12,
    price: 10000,
    initialStock: 100,
    onSale: false,
    subCategoryId: '',
    imageUrl: '',
  });
  const [imageUploadMode, setImageUploadMode] = useState<'url' | 'file'>('url');
  const [submitting, setSubmitting] = useState(false);

  const fetchProducts = async () => {
    try {
      setLoading(true);
      const [productsData, subCatData] = await Promise.all([
        apiRequest<ProductItem[]>('/products'),
        apiRequest<SubCategoryOption[]>('/products/subcategories').catch(() => []),
      ]);
      setProducts(productsData);
      setSubCategories(subCatData);
    } catch (err) {
      console.error('Failed to load products', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
    const handleCurrencySynced = () => {
      fetchProducts();
    };
    window.addEventListener('dm-currency-synced', handleCurrencySynced);
    return () => {
      window.removeEventListener('dm-currency-synced', handleCurrencySynced);
    };
  }, []);

  const handleAdjustStock = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!stockProduct) return;
    setModalError(null);

    const signedDelta = adjustMode === 'subtract' ? -Math.abs(adjustQty) : Math.abs(adjustQty);

    try {
      setIsAdjusting(true);
      await apiRequest('/stocks/adjust', {
        method: 'POST',
        body: JSON.stringify({
          productId: stockProduct.id,
          quantityDelta: signedDelta,
          note: adjustNote,
        }),
      });
      setStockProduct(null);
      await fetchProducts();
    } catch (err: unknown) {
      setModalError(err instanceof Error ? err.message : 'Error al ajustar stock');
    } finally {
      setIsAdjusting(false);
    }
  };

  const handleImageFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (file.size > 2 * 1024 * 1024) {
        setModalError('La imagen seleccionada es mayor a 2MB. Por favor elija un archivo más liviano.');
        return;
      }
      setModalError(null);
      const reader = new FileReader();
      reader.onload = () => {
        setFormData((prev) => ({ ...prev, imageUrl: reader.result as string }));
      };
      reader.readAsDataURL(file);
    }
  };

  const openCreateModal = () => {
    setModalError(null);
    setFormData({
      code: '',
      name: '',
      description: '',
      packaging: 'Display x 24 u.',
      unitsPerPack: 24,
      price: 15000,
      initialStock: 100,
      onSale: false,
      subCategoryId: subCategories[0]?.id || '',
      imageUrl: '',
    });
    setImageUploadMode('url');
    setShowAddModal(true);
  };

  const handleCreateProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    setModalError(null);
    setSubmitting(true);
    try {
      await apiRequest('/products', {
        method: 'POST',
        body: JSON.stringify({
          code: formData.code.trim().toUpperCase(),
          name: formData.name.trim(),
          description: formData.description.trim(),
          packaging: formData.packaging.trim(),
          unitsPerPack: Number(formData.unitsPerPack),
          price: Number(formData.price),
          initialStock: Number(formData.initialStock),
          onSale: formData.onSale,
          subCategoryId: formData.subCategoryId || null,
          imageUrl: formData.imageUrl.trim() || null,
        }),
      });
      setShowAddModal(false);
      await fetchProducts();
    } catch (err: unknown) {
      setModalError(err instanceof Error ? err.message : 'Error al crear producto');
    } finally {
      setSubmitting(false);
    }
  };

  const openEditModal = (p: ProductItem) => {
    setModalError(null);
    setEditProduct(p);
    setFormData({
      code: p.code,
      name: p.name,
      description: p.description || '',
      packaging: p.packaging,
      unitsPerPack: p.unitsPerPack,
      price: p.currentPrice,
      initialStock: p.currentStock,
      onSale: p.onSale,
      subCategoryId: p.subCategoryId || '',
      imageUrl: p.imageUrl || p.imageBase64 || '',
    });
    setImageUploadMode(p.imageUrl?.startsWith('data:') ? 'file' : 'url');
  };

  const handleUpdateProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editProduct) return;
    setModalError(null);
    setSubmitting(true);
    try {
      await apiRequest(`/products/${editProduct.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          code: formData.code.trim().toUpperCase(),
          name: formData.name.trim(),
          description: formData.description.trim(),
          packaging: formData.packaging.trim(),
          unitsPerPack: Number(formData.unitsPerPack),
          price: Number(formData.price),
          onSale: formData.onSale,
          subCategoryId: formData.subCategoryId || null,
          imageUrl: formData.imageUrl.trim() || null,
        }),
      });
      setEditProduct(null);
      await fetchProducts();
    } catch (err: unknown) {
      setModalError(err instanceof Error ? err.message : 'Error al actualizar producto');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteProduct = async () => {
    if (!deleteProduct) return;
    setModalError(null);
    setSubmitting(true);
    try {
      await apiRequest(`/products/${deleteProduct.id}`, {
        method: 'DELETE',
      });
      setDeleteProduct(null);
      await fetchProducts();
    } catch (err: unknown) {
      setModalError(err instanceof Error ? err.message : 'Error al eliminar producto');
    } finally {
      setSubmitting(false);
    }
  };

  const filteredProducts = products.filter(
    (p) =>
      p.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.code.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (p.categoryName && p.categoryName.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  return (
    <div className="products-view">
      <div className="view-header">
        <div>
          <h2>Control de Depósito y Catálogo Mayorista</h2>
          <p className="text-muted">Gestión integral de unidades, fotos de productos, stock disponible y precios vigentes</p>
        </div>
        <div className="header-actions">
          <button onClick={openCreateModal} className="btn btn-primary">
            <Plus size={16} /> Nuevo Producto
          </button>
          <button onClick={fetchProducts} className="btn btn-secondary">
            <RefreshCw size={16} /> Actualizar
          </button>
        </div>
      </div>

      {/* Search Bar */}
      <div className="toolbar">
        <div className="search-box">
          <Search size={18} className="search-icon" />
          <input
            type="text"
            placeholder="Buscar por código (ej. ARC-), nombre o categoría..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        <div className="stats-pill">
          Mostrando <strong>{filteredProducts.length}</strong> de {products.length} productos
        </div>
      </div>

      {/* Table */}
      {loading ? (
        <div className="view-loading">
          <RefreshCw className="animate-spin" size={24} />
          <p>Cargando catálogo en tiempo real...</p>
        </div>
      ) : (
        <div className="panel">
          <div className="panel-body no-padding table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Foto</th>
                  <th>Código</th>
                  <th>Producto</th>
                  <th>Categoría</th>
                  <th>Presentación</th>
                  <th>Precio Mayorista</th>
                  <th>Stock Depósito</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {filteredProducts.map((p) => {
                  const isLow = p.currentStock <= 20;
                  const photo = p.imageUrl || p.imageBase64;
                  return (
                    <tr key={p.id}>
                      <td>
                        <div
                          style={{
                            width: '44px',
                            height: '44px',
                            borderRadius: '6px',
                            overflow: 'hidden',
                            background: '#f8fafc',
                            border: '1px solid var(--border)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                          }}
                        >
                          {photo ? (
                            <img
                              src={photo}
                              alt={p.name}
                              style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                            />
                          ) : (
                            <Package size={20} className="text-muted" />
                          )}
                        </div>
                      </td>
                      <td className="font-mono font-semibold">{p.code}</td>
                      <td>
                        <div className="product-cell-name">
                          <span className="font-semibold">{p.name}</span>
                          {p.onSale && <span className="tag-sale">OFERTA</span>}
                        </div>
                      </td>
                      <td>
                        <span className="badge badge-neutral">{p.categoryName || 'General'}</span>
                      </td>
                      <td>
                        {p.packaging} ({p.unitsPerPack} u.)
                      </td>
                      <td className="font-semibold text-primary">
                        <div>${p.currentPrice.toLocaleString('es-AR')}</div>
                        {p.basePriceUsd != null && (
                          <div className="text-xs text-muted font-mono">
                            USD ${Number(p.basePriceUsd).toFixed(2)}
                          </div>
                        )}
                      </td>
                      <td>
                        <span className={`badge ${isLow ? 'badge-danger' : 'badge-success'}`}>
                          {p.currentStock} {p.currentStock === 1 ? 'unidad' : 'unidades'}
                        </span>
                      </td>
                      <td>
                        {isLow ? (
                          <span className="status-indicator warning">
                            <AlertCircle size={14} /> Stock Bajo
                          </span>
                        ) : (
                          <span className="status-indicator good">
                            <CheckCircle size={14} /> Normal
                          </span>
                        )}
                      </td>
                      <td>
                        <div className="action-btn-group">
                          <button
                            onClick={() => setViewProduct(p)}
                            className="action-icon-btn view"
                            title="Ver detalles completos"
                          >
                            <Eye size={14} />
                            <span>Ver</span>
                          </button>
                          <button
                            onClick={() => openEditModal(p)}
                            className="action-icon-btn edit"
                            title="Editar datos y foto"
                          >
                            <Edit2 size={14} />
                            <span>Editar</span>
                          </button>
                          <button
                            onClick={() => {
                              setModalError(null);
                              setStockProduct(p);
                              setAdjustMode('add');
                              setAdjustQty(50);
                              setAdjustNote('Ingreso de mercadería desde proveedor');
                            }}
                            className="action-icon-btn"
                            title="Ajustar o reponer stock"
                          >
                            <Package size={14} />
                            <span>Stock</span>
                          </button>
                          <button
                            onClick={() => {
                              setModalError(null);
                              setDeleteProduct(p);
                            }}
                            className="action-icon-btn delete"
                            title="Eliminar producto"
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
          </div>
        </div>
      )}

      {/* Modal: VER DETALLES */}
      {viewProduct && (
        <div className="modal-backdrop" onClick={() => setViewProduct(null)}>
          <div className="modal-card modal-lg" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header-row">
              <h3>Ficha Técnica del Producto</h3>
              <button onClick={() => setViewProduct(null)} className="modal-close-btn">
                <X size={20} />
              </button>
            </div>

            <div style={{ display: 'flex', gap: '1.5rem', marginBottom: '1.25rem', alignItems: 'flex-start' }}>
              <div
                style={{
                  width: '140px',
                  height: '140px',
                  borderRadius: '10px',
                  overflow: 'hidden',
                  background: '#f8fafc',
                  border: '1.5px solid var(--border)',
                  flexShrink: 0,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                {viewProduct.imageUrl || viewProduct.imageBase64 ? (
                  <img
                    src={viewProduct.imageUrl || viewProduct.imageBase64}
                    alt={viewProduct.name}
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  />
                ) : (
                  <Package size={48} className="text-muted" />
                )}
              </div>

              <div style={{ flex: 1 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
                  <span className="font-mono text-sm font-semibold" style={{ background: '#f1f5f9', padding: '0.15rem 0.45rem', borderRadius: '4px' }}>
                    {viewProduct.code}
                  </span>
                  {viewProduct.onSale && <span className="tag-sale">EN OFERTA</span>}
                </div>
                <h2 style={{ fontSize: '1.35rem', color: 'var(--text-main)', marginBottom: '0.35rem' }}>{viewProduct.name}</h2>
                <p className="text-muted" style={{ fontSize: '0.9rem' }}>
                  {viewProduct.description || 'Sin descripción detallada cargada.'}
                </p>
              </div>
            </div>

            <div className="details-grid">
              <div className="detail-item">
                <span className="detail-label">Categoría / Rubro</span>
                <span className="detail-value font-semibold">{viewProduct.categoryName || 'General'}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Subcategoría</span>
                <span className="detail-value">{viewProduct.subCategoryName === 'Unassigned' ? 'Sin subcategoría' : (viewProduct.subCategoryName || 'Sin subcategoría')}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Presentación de Venta</span>
                <span className="detail-value">{viewProduct.packaging}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Unidades por Bulto</span>
                <span className="detail-value font-mono">{viewProduct.unitsPerPack} unidades</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Precio Mayorista Vigente</span>
                <span className="detail-value font-bold text-primary" style={{ fontSize: '1.25rem' }}>
                  ${viewProduct.currentPrice.toLocaleString('es-AR')}
                </span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Stock en Depósito Central</span>
                <span className="detail-value">
                  <span className={`badge ${viewProduct.currentStock <= 20 ? 'badge-danger' : 'badge-success'}`}>
                    {viewProduct.currentStock} {viewProduct.currentStock === 1 ? 'unidad disponible' : 'unidades disponibles'}
                  </span>
                </span>
              </div>
            </div>

            <div className="modal-actions" style={{ marginTop: '1.5rem', borderTop: '1px solid var(--border)', paddingTop: '1rem' }}>
              <button
                onClick={() => {
                  const p = viewProduct;
                  setViewProduct(null);
                  openEditModal(p);
                }}
                className="btn btn-primary"
              >
                <Edit2 size={16} /> Editar Producto
              </button>
              <button onClick={() => setViewProduct(null)} className="btn btn-secondary">
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: CREAR PRODUCTO */}
      {showAddModal && (
        <div className="modal-backdrop" onClick={() => setShowAddModal(false)}>
          <div className="modal-card modal-lg" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header-row">
              <h3>Alta de Nuevo Producto Mayorista</h3>
              <button onClick={() => setShowAddModal(false)} className="modal-close-btn">
                <X size={20} />
              </button>
            </div>

            {modalError && (
              <div className="modal-error-banner">
                <AlertCircle size={18} style={{ flexShrink: 0, marginTop: '1px' }} />
                <span>{modalError}</span>
              </div>
            )}

            <form onSubmit={handleCreateProduct} className="modal-form">
              <div className="grid-cols-2 gap-4">
                <label>
                  Código de Producto (SKU) *
                  <input
                    type="text"
                    value={formData.code}
                    onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                    placeholder="Ej. ARC-BON-02"
                    required
                  />
                </label>

                <label>
                  Nombre Comercial *
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    placeholder="Ej. Bon o Bon Blanco"
                    required
                  />
                </label>
              </div>

              <label>
                Subcategoría / Rubro de Catálogo
                <select
                  value={formData.subCategoryId}
                  onChange={(e) => setFormData({ ...formData, subCategoryId: e.target.value })}
                >
                  <option value="">-- Sin subcategoría específica (General) --</option>
                  {subCategories.map((sc) => (
                    <option key={sc.id} value={sc.id}>
                      {sc.categoryName ? `${sc.categoryName} › ${sc.name}` : sc.name}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Descripción del Producto
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Detalle de sabor, relleno, marca o presentación..."
                  rows={2}
                  style={{ width: '100%', padding: '0.6rem', border: '1px solid var(--border)', borderRadius: '6px' }}
                />
              </label>

              <div className="grid-cols-2 gap-4">
                <label>
                  Presentación de Embalaje *
                  <input
                    type="text"
                    value={formData.packaging}
                    onChange={(e) => setFormData({ ...formData, packaging: e.target.value })}
                    placeholder="Ej. Display x 24 un."
                    required
                  />
                </label>

                <label>
                  Unidades por Bulto *
                  <input
                    type="number"
                    min="1"
                    value={formData.unitsPerPack}
                    onChange={(e) => setFormData({ ...formData, unitsPerPack: Number(e.target.value) })}
                    required
                  />
                </label>
              </div>

              <div className="grid-cols-2 gap-4">
                <label>
                  Precio Mayorista (ARS) *
                  <input
                    type="number"
                    min="1"
                    step="any"
                    value={formData.price}
                    onChange={(e) => setFormData({ ...formData, price: Number(e.target.value) })}
                    required
                  />
                </label>

                <label>
                  Stock Inicial (Unidades) *
                  <input
                    type="number"
                    min="0"
                    value={formData.initialStock}
                    onChange={(e) => setFormData({ ...formData, initialStock: Number(e.target.value) })}
                    required
                  />
                </label>
              </div>

              {/* PHOTO UPLOADER (URL / ARCHIVO) */}
              <div style={{ margin: '0.75rem 0' }}>
                <span className="detail-label" style={{ display: 'block', marginBottom: '0.4rem' }}>
                  Foto del Producto (URL o Archivo)
                </span>
                <div className="image-upload-mode-pills">
                  <button
                    type="button"
                    onClick={() => setImageUploadMode('url')}
                    className={`upload-pill ${imageUploadMode === 'url' ? 'active' : ''}`}
                  >
                    <Link size={13} style={{ display: 'inline', marginRight: '4px' }} />
                    Ingresar URL
                  </button>
                  <button
                    type="button"
                    onClick={() => setImageUploadMode('file')}
                    className={`upload-pill ${imageUploadMode === 'file' ? 'active' : ''}`}
                  >
                    <Upload size={13} style={{ display: 'inline', marginRight: '4px' }} />
                    Subir Archivo
                  </button>
                </div>

                {imageUploadMode === 'url' ? (
                  <input
                    type="url"
                    placeholder="https://ejemplo.com/foto-producto.jpg"
                    value={formData.imageUrl}
                    onChange={(e) => setFormData({ ...formData, imageUrl: e.target.value })}
                    style={{ width: '100%', padding: '0.6rem', border: '1px solid var(--border)', borderRadius: '6px' }}
                  />
                ) : (
                  <label className="image-dropzone" style={{ display: 'block' }}>
                    <Upload size={24} style={{ color: 'var(--text-muted)', margin: '0 auto 0.4rem' }} />
                    <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      Hacé clic para seleccionar una imagen desde tu equipo (JPG, PNG, WEBP)
                    </span>
                    <input
                      type="file"
                      accept="image/*"
                      onChange={handleImageFileChange}
                      style={{ display: 'none' }}
                    />
                  </label>
                )}

                {formData.imageUrl && (
                  <div className="image-preview-container">
                    <div className="image-preview-box">
                      <img src={formData.imageUrl} alt="Vista previa" />
                    </div>
                    <div style={{ flex: 1 }}>
                      <span className="font-semibold text-sm">Vista previa de la foto</span>
                      <p className="text-muted text-xs">La imagen se guardará asociada al producto.</p>
                      <button
                        type="button"
                        onClick={() => setFormData({ ...formData, imageUrl: '' })}
                        className="btn btn-sm btn-danger-outline"
                        style={{ marginTop: '0.4rem' }}
                      >
                        Quitar Foto
                      </button>
                    </div>
                  </div>
                )}
              </div>

              <div style={{ margin: '0.75rem 0' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    checked={formData.onSale}
                    onChange={(e) => setFormData({ ...formData, onSale: e.target.checked })}
                    style={{ width: '18px', height: '18px' }}
                  />
                  <span className="font-semibold">Marcar como Producto en Oferta Mayorista</span>
                </label>
              </div>

              <div className="modal-actions">
                <button type="submit" disabled={submitting} className="btn btn-primary">
                  {submitting ? 'Guardando...' : 'Crear Producto'}
                </button>
                <button type="button" onClick={() => setShowAddModal(false)} className="btn btn-secondary">
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: EDITAR PRODUCTO */}
      {editProduct && (
        <div className="modal-backdrop" onClick={() => setEditProduct(null)}>
          <div className="modal-card modal-lg" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header-row">
              <h3>Editar Producto ({editProduct.code})</h3>
              <button onClick={() => setEditProduct(null)} className="modal-close-btn">
                <X size={20} />
              </button>
            </div>

            {modalError && (
              <div className="modal-error-banner">
                <AlertCircle size={18} style={{ flexShrink: 0, marginTop: '1px' }} />
                <span>{modalError}</span>
              </div>
            )}

            <form onSubmit={handleUpdateProduct} className="modal-form">
              <div className="grid-cols-2 gap-4">
                <label>
                  Código de Producto (SKU)
                  <input type="text" value={formData.code} disabled style={{ background: '#f1f5f9' }} />
                </label>

                <label>
                  Nombre Comercial *
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    required
                  />
                </label>
              </div>

              <label>
                Subcategoría / Rubro de Catálogo
                <select
                  value={formData.subCategoryId}
                  onChange={(e) => setFormData({ ...formData, subCategoryId: e.target.value })}
                >
                  <option value="">-- Sin subcategoría específica (General) --</option>
                  {subCategories.map((sc) => (
                    <option key={sc.id} value={sc.id}>
                      {sc.categoryName ? `${sc.categoryName} › ${sc.name}` : sc.name}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Descripción del Producto
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  rows={2}
                  style={{ width: '100%', padding: '0.6rem', border: '1px solid var(--border)', borderRadius: '6px' }}
                />
              </label>

              <div className="grid-cols-2 gap-4">
                <label>
                  Presentación de Embalaje *
                  <input
                    type="text"
                    value={formData.packaging}
                    onChange={(e) => setFormData({ ...formData, packaging: e.target.value })}
                    required
                  />
                </label>

                <label>
                  Unidades por Bulto *
                  <input
                    type="number"
                    min="1"
                    value={formData.unitsPerPack}
                    onChange={(e) => setFormData({ ...formData, unitsPerPack: Number(e.target.value) })}
                    required
                  />
                </label>
              </div>

              <div className="grid-cols-2 gap-4">
                <label>
                  Precio Mayorista Vigente (ARS) *
                  <input
                    type="number"
                    min="1"
                    step="any"
                    value={formData.price}
                    onChange={(e) => setFormData({ ...formData, price: Number(e.target.value) })}
                    required
                  />
                </label>

                <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={formData.onSale}
                      onChange={(e) => setFormData({ ...formData, onSale: e.target.checked })}
                      style={{ width: '18px', height: '18px' }}
                    />
                    <span className="font-semibold">Marcar como Producto en Oferta</span>
                  </label>
                </div>
              </div>

              {/* PHOTO UPLOADER (URL / ARCHIVO) */}
              <div style={{ margin: '0.75rem 0' }}>
                <span className="detail-label" style={{ display: 'block', marginBottom: '0.4rem' }}>
                  Foto del Producto (URL o Archivo)
                </span>
                <div className="image-upload-mode-pills">
                  <button
                    type="button"
                    onClick={() => setImageUploadMode('url')}
                    className={`upload-pill ${imageUploadMode === 'url' ? 'active' : ''}`}
                  >
                    <Link size={13} style={{ display: 'inline', marginRight: '4px' }} />
                    Ingresar URL
                  </button>
                  <button
                    type="button"
                    onClick={() => setImageUploadMode('file')}
                    className={`upload-pill ${imageUploadMode === 'file' ? 'active' : ''}`}
                  >
                    <Upload size={13} style={{ display: 'inline', marginRight: '4px' }} />
                    Subir Archivo
                  </button>
                </div>

                {imageUploadMode === 'url' ? (
                  <input
                    type="url"
                    placeholder="https://ejemplo.com/foto-producto.jpg"
                    value={formData.imageUrl}
                    onChange={(e) => setFormData({ ...formData, imageUrl: e.target.value })}
                    style={{ width: '100%', padding: '0.6rem', border: '1px solid var(--border)', borderRadius: '6px' }}
                  />
                ) : (
                  <label className="image-dropzone" style={{ display: 'block' }}>
                    <Upload size={24} style={{ color: 'var(--text-muted)', margin: '0 auto 0.4rem' }} />
                    <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      Hacé clic para seleccionar una nueva foto desde tu computadora
                    </span>
                    <input
                      type="file"
                      accept="image/*"
                      onChange={handleImageFileChange}
                      style={{ display: 'none' }}
                    />
                  </label>
                )}

                {formData.imageUrl && (
                  <div className="image-preview-container">
                    <div className="image-preview-box">
                      <img src={formData.imageUrl} alt="Vista previa" />
                    </div>
                    <div style={{ flex: 1 }}>
                      <span className="font-semibold text-sm">Foto actual / seleccionada</span>
                      <p className="text-muted text-xs">Se actualizará al guardar los cambios.</p>
                      <button
                        type="button"
                        onClick={() => setFormData({ ...formData, imageUrl: '' })}
                        className="btn btn-sm btn-danger-outline"
                        style={{ marginTop: '0.4rem' }}
                      >
                        Quitar Foto
                      </button>
                    </div>
                  </div>
                )}
              </div>

              <div className="modal-actions">
                <button type="submit" disabled={submitting} className="btn btn-primary">
                  {submitting ? 'Guardando...' : 'Guardar Cambios'}
                </button>
                <button type="button" onClick={() => setEditProduct(null)} className="btn btn-secondary">
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: ELIMINAR PRODUCTO */}
      {deleteProduct && (
        <div className="modal-backdrop" onClick={() => setDeleteProduct(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1rem', color: 'var(--danger)' }}>
              <AlertTriangle size={28} />
              <h3 style={{ margin: 0, color: 'var(--text-main)' }}>¿Eliminar Producto?</h3>
            </div>

            {modalError && (
              <div className="modal-error-banner">
                <AlertCircle size={18} style={{ flexShrink: 0, marginTop: '1px' }} />
                <span>{modalError}</span>
              </div>
            )}

            <p style={{ marginBottom: '1rem', color: 'var(--text-muted)' }}>
              Estás a punto de dar de baja <strong>{deleteProduct.name}</strong> ({deleteProduct.code}). Esta acción lo removerá del catálogo y de la preventa móvil.
            </p>
            <div className="modal-actions">
              <button
                type="button"
                onClick={handleDeleteProduct}
                disabled={submitting}
                className="btn btn-danger-outline"
                style={{ background: 'var(--danger)', color: '#fff', border: 'none' }}
              >
                {submitting ? 'Eliminando...' : 'Sí, Eliminar Producto'}
              </button>
              <button
                type="button"
                onClick={() => setDeleteProduct(null)}
                className="btn btn-secondary"
              >
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: AJUSTAR / REPONER STOCK */}
      {stockProduct && (
        <div className="modal-backdrop" onClick={() => setStockProduct(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header-row">
              <h3>Ajuste de Stock en Depósito</h3>
              <button onClick={() => setStockProduct(null)} className="modal-close-btn">
                <X size={20} />
              </button>
            </div>

            {modalError && (
              <div className="modal-error-banner">
                <AlertCircle size={18} style={{ flexShrink: 0, marginTop: '1px' }} />
                <span>{modalError}</span>
              </div>
            )}

            <p className="modal-subtitle">
              Producto: <strong>{stockProduct.name}</strong> ({stockProduct.code})
            </p>
            <p style={{ marginBottom: '1rem' }}>
              Stock actual: <strong>{stockProduct.currentStock} {stockProduct.currentStock === 1 ? 'unidad' : 'unidades'}</strong>
            </p>

            <form onSubmit={handleAdjustStock} className="modal-form">
              <label>
                Tipo de Movimiento:
                <select
                  value={adjustMode}
                  onChange={(e) => {
                    const nextMode = e.target.value as 'add' | 'subtract';
                    setAdjustMode(nextMode);
                    setAdjustNote(
                      nextMode === 'add'
                        ? 'Ingreso de mercadería desde proveedor'
                        : 'Ajuste por merma / control de inventario'
                    );
                  }}
                >
                  <option value="add">Ingreso / Reposición (+)</option>
                  <option value="subtract">Merma / Descuento de Inventario (-)</option>
                </select>
              </label>

              <label>
                Cantidad de Unidades ({adjustMode === 'add' ? 'a sumar' : 'a descontar'}):
                <input
                  type="number"
                  min="1"
                  max={adjustMode === 'subtract' ? Math.max(1, stockProduct.currentStock) : 100000}
                  value={adjustQty}
                  onChange={(e) => setAdjustQty(Number(e.target.value))}
                  required
                />
              </label>

              <label>
                Motivo / Remito de Respaldo *:
                <input
                  type="text"
                  value={adjustNote}
                  onChange={(e) => setAdjustNote(e.target.value)}
                  placeholder="Ej: Remito Arcor #94812"
                  required
                />
              </label>

              <div className="modal-actions">
                <button type="submit" disabled={isAdjusting} className="btn btn-primary">
                  {isAdjusting ? 'Actualizando...' : 'Confirmar Ajuste'}
                </button>
                <button
                  type="button"
                  onClick={() => setStockProduct(null)}
                  className="btn btn-secondary"
                >
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

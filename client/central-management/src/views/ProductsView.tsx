import React, { useEffect, useState } from 'react';
import { apiRequest } from '../services/api';
import { Plus, Search, RefreshCw, AlertCircle, CheckCircle, Package } from 'lucide-react';

interface ProductItem {
  id: string;
  code: string;
  name: string;
  description: string;
  packaging: string;
  unitsPerPack: number;
  onSale: boolean;
  subCategoryName: string;
  categoryName: string;
  currentPrice: number;
  currentStock: number;
}

export const ProductsView: React.FC = () => {
  const [products, setProducts] = useState<ProductItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedProduct, setSelectedProduct] = useState<ProductItem | null>(null);
  const [adjustQty, setAdjustQty] = useState(50);
  const [adjustNote, setAdjustNote] = useState('Ingreso de mercadería desde proveedor');
  const [isAdjusting, setIsAdjusting] = useState(false);
  const [showAddModal, setShowAddModal] = useState(false);

  // New Product Form State
  const [newCode, setNewCode] = useState('');
  const [newName, setNewName] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [newPackaging, setNewPackaging] = useState('Pack x 12 u.');
  const [newUnits, setNewUnits] = useState(12);
  const [newPrice, setNewPrice] = useState(10000);
  const [newInitialStock, setNewInitialStock] = useState(100);
  const [newOnSale, setNewOnSale] = useState(false);

  const fetchProducts = async () => {
    try {
      setLoading(true);
      const data = await apiRequest<ProductItem[]>('/products');
      setProducts(data);
    } catch (err) {
      console.error('Failed to load products', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
  }, []);

  const handleAdjustStock = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProduct) return;

    try {
      setIsAdjusting(true);
      await apiRequest('/stocks/adjust', {
        method: 'POST',
        body: JSON.stringify({
          productId: selectedProduct.id,
          quantityDelta: adjustQty,
          note: adjustNote,
        }),
      });
      setSelectedProduct(null);
      await fetchProducts();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Error adjusting stock');
    } finally {
      setIsAdjusting(false);
    }
  };

  const handleCreateProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await apiRequest('/products', {
        method: 'POST',
        body: JSON.stringify({
          code: newCode,
          name: newName,
          description: newDesc,
          packaging: newPackaging,
          unitsPerPack: newUnits,
          price: newPrice,
          initialStock: newInitialStock,
          onSale: newOnSale,
        }),
      });
      setShowAddModal(false);
      // Reset form
      setNewCode('');
      setNewName('');
      setNewDesc('');
      await fetchProducts();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Error creating product');
    }
  };

  const filteredProducts = products.filter(
    (p) =>
      p.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.code.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.categoryName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="products-view">
      <div className="view-header">
        <div>
          <h2>Control de Depósito y Catálogo Mayorista</h2>
          <p className="text-muted">Gestión de bultos, stock disponible y precios vigentes</p>
        </div>
        <div className="header-actions">
          <button onClick={() => setShowAddModal(true)} className="btn btn-primary">
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
            placeholder="Buscar por código (ej. ALF-), nombre o categoría..."
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
          <p>Cargando catálogo...</p>
        </div>
      ) : (
        <div className="panel">
          <div className="panel-body no-padding">
            <table className="data-table">
              <thead>
                <tr>
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
                  return (
                    <tr key={p.id}>
                      <td className="font-mono font-semibold">{p.code}</td>
                      <td>
                        <div className="product-cell-name">
                          <Package size={16} className="text-muted" />
                          <span>{p.name}</span>
                          {p.onSale && <span className="tag-sale">OFERTA</span>}
                        </div>
                      </td>
                      <td>
                        <span className="badge badge-neutral">{p.categoryName}</span>
                      </td>
                      <td>
                        {p.packaging} ({p.unitsPerPack} u/bulto)
                      </td>
                      <td className="font-semibold text-primary">
                        ${p.currentPrice.toLocaleString('es-AR')}
                      </td>
                      <td>
                        <span className={`badge ${isLow ? 'badge-danger' : 'badge-success'}`}>
                          {p.currentStock} bultos
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
                        <button
                          onClick={() => {
                            setSelectedProduct(p);
                            setAdjustQty(50);
                          }}
                          className="btn btn-sm btn-outline"
                        >
                          Reponer Stock
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal Reponer Stock */}
      {selectedProduct && (
        <div className="modal-backdrop">
          <div className="modal-card">
            <h3>Reponer Stock en Depósito</h3>
            <p className="modal-subtitle">
              Producto: <strong>{selectedProduct.name}</strong> ({selectedProduct.code})
            </p>
            <p>
              Stock actual registrado: <strong>{selectedProduct.currentStock} unidades</strong>
            </p>

            <form onSubmit={handleAdjustStock} className="modal-form">
              <label>
                Cantidad a Ingresar / Ajustar (unidades):
                <input
                  type="number"
                  min="1"
                  value={adjustQty}
                  onChange={(e) => setAdjustQty(Number(e.target.value))}
                  required
                />
              </label>

              <label>
                Motivo / Nota de Ajuste:
                <input
                  type="text"
                  value={adjustNote}
                  onChange={(e) => setAdjustNote(e.target.value)}
                  placeholder="Ej: Llegada de camión de fábrica"
                  required
                />
              </label>

              <div className="modal-actions">
                <button
                  type="button"
                  onClick={() => setSelectedProduct(null)}
                  className="btn btn-secondary"
                >
                  Cancelar
                </button>
                <button type="submit" disabled={isAdjusting} className="btn btn-primary">
                  {isAdjusting ? 'Guardando...' : 'Confirmar Reposición'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Nuevo Producto */}
      {showAddModal && (
        <div className="modal-backdrop">
          <div className="modal-card wide">
            <h3>Registrar Nuevo Producto en Catálogo</h3>
            <p className="modal-subtitle">
              Se creará el registro con precio inicial y stock en el depósito central
            </p>

            <form onSubmit={handleCreateProduct} className="modal-form grid-2">
              <label>
                Código Único:
                <input
                  type="text"
                  placeholder="ej. ALF-NEGRO-70"
                  value={newCode}
                  onChange={(e) => setNewCode(e.target.value)}
                  required
                />
              </label>

              <label>
                Nombre Comercial:
                <input
                  type="text"
                  placeholder="ej. Alfajor Chocolate 70% Cacao"
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  required
                />
              </label>

              <label className="col-span-2">
                Descripción Comercial:
                <input
                  type="text"
                  placeholder="Alfajor artesanal relleno de dulce de leche bañado en chocolate amargo"
                  value={newDesc}
                  onChange={(e) => setNewDesc(e.target.value)}
                />
              </label>

              <label>
                Presentación / Empaque:
                <input
                  type="text"
                  placeholder="Pack x 12 u."
                  value={newPackaging}
                  onChange={(e) => setNewPackaging(e.target.value)}
                  required
                />
              </label>

              <label>
                Unidades por Bulto:
                <input
                  type="number"
                  min="1"
                  value={newUnits}
                  onChange={(e) => setNewUnits(Number(e.target.value))}
                  required
                />
              </label>

              <label>
                Precio Mayorista Inicial ($ ARS):
                <input
                  type="number"
                  min="0"
                  step="100"
                  value={newPrice}
                  onChange={(e) => setNewPrice(Number(e.target.value))}
                  required
                />
              </label>

              <label>
                Stock Inicial en Depósito:
                <input
                  type="number"
                  min="0"
                  value={newInitialStock}
                  onChange={(e) => setNewInitialStock(Number(e.target.value))}
                  required
                />
              </label>

              <div className="col-span-2 checkbox-group">
                <input
                  type="checkbox"
                  id="onSale"
                  checked={newOnSale}
                  onChange={(e) => setNewOnSale(e.target.checked)}
                />
                <label htmlFor="onSale">Marcar como Producto en Oferta Destacada</label>
              </div>

              <div className="col-span-2 modal-actions">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="btn btn-secondary"
                >
                  Cancelar
                </button>
                <button type="submit" className="btn btn-primary">
                  Guardar Producto
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

import React, { useState, useEffect } from 'react';
import { fetchProducts, fetchPointsOfSale, submitOrderToBackend } from '../services/api';
import type { PointOfSale, OrderItem, Product } from '../types/models';
import { PaymentType } from '../types/enums';
import { useAuth } from '../context/AuthContext';
import {
  Search,
  Plus,
  Minus,
  Check,
  ArrowRight,
  Store,
  X,
  Loader2,
} from 'lucide-react';

interface CatalogOrderViewProps {
  initialPOS?: PointOfSale | null;
  onOrderSuccess?: () => void;
}

export const CatalogOrderView: React.FC<CatalogOrderViewProps> = ({
  initialPOS,
  onOrderSuccess,
}) => {
  const { user } = useAuth();
  const [products, setProducts] = useState<Product[]>([]);
  const [allPOS, setAllPOS] = useState<PointOfSale[]>([]);
  const [selectedPOS, setSelectedPOS] = useState<PointOfSale | null>(initialPOS || null);
  const [loading, setLoading] = useState(true);

  const [selectedBrand, setSelectedBrand] = useState<string>('TODOS');
  const [searchQuery, setSearchQuery] = useState('');
  const [cart, setCart] = useState<{ [productId: string]: number }>({});
  const [isCartOpen, setIsCartOpen] = useState(false);
  const [couponCode, setCouponCode] = useState('');
  const [discountPercent, setDiscountPercent] = useState(0);
  const [couponMsg, setCouponMsg] = useState<{ text: string; ok: boolean } | null>(null);
  const [paymentType, setPaymentType] = useState<PaymentType>(PaymentType.CASH);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [orderPlacedMsg, setOrderPlacedMsg] = useState<string | null>(null);

  useEffect(() => {
    async function loadData() {
      setLoading(true);
      try {
        const [loadedProds, loadedPos] = await Promise.all([
          fetchProducts(),
          fetchPointsOfSale(),
        ]);
        setProducts(loadedProds);
        setAllPOS(loadedPos);
        if (!selectedPOS && loadedPos.length > 0) {
          setSelectedPOS(loadedPos[0]);
        }
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, []);

  const brands = ['TODOS', 'Arcor', 'Bagley', 'Mondelez', 'Terrabusi'];

  const filteredProducts = products.filter((prod) => {
    const matchesBrand = selectedBrand === 'TODOS' || prod.brand === selectedBrand;
    const matchesSearch =
      prod.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      prod.category.toLowerCase().includes(searchQuery.toLowerCase()) ||
      prod.code.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesBrand && matchesSearch;
  });

  const updateQuantity = (productId: string, delta: number) => {
    setCart((prev) => {
      const current = prev[productId] || 0;
      const updated = Math.max(0, current + delta);
      if (updated === 0) {
        const copy = { ...prev };
        delete copy[productId];
        return copy;
      }
      return { ...prev, [productId]: updated };
    });
  };

  const productMap: { [id: string]: Product } = {};
  products.forEach((p) => {
    productMap[p.id] = p;
  });

  const cartItems: OrderItem[] = Object.entries(cart)
    .map(([prodId, qty]) => {
      const product = productMap[prodId];
      if (!product || qty <= 0) return null;
      return {
        product,
        quantityPacks: qty,
        subtotal: product.bulkPrice * qty,
      };
    })
    .filter((item): item is OrderItem => item !== null);

  const totalPacks = cartItems.reduce((acc, item) => acc + item.quantityPacks, 0);
  const rawSubtotal = cartItems.reduce((acc, item) => acc + item.subtotal, 0);
  const discountAmount = (rawSubtotal * discountPercent) / 100;
  const finalTotal = rawSubtotal - discountAmount;

  const handleApplyCoupon = () => {
    const code = couponCode.trim().toUpperCase();
    if (code === 'BIENVENIDA' || code === 'DULCE10' || code === 'MAYORISTA10') {
      setDiscountPercent(10);
      setCouponMsg({ text: 'Cupón del 10% aplicado con éxito', ok: true });
    } else if (code === 'ALFAJORFEST') {
      setDiscountPercent(15);
      setCouponMsg({ text: 'Cupón especial 15% aplicado', ok: true });
    } else if (code === 'MAYORISTA2026' || code === 'PROMO20') {
      setDiscountPercent(20);
      setCouponMsg({ text: 'Cupón especial 20% aplicado', ok: true });
    } else {
      setCouponMsg({ text: 'Cupón inválido o sin vigencia', ok: false });
    }
  };

  const handleConfirmOrder = async () => {
    if (cartItems.length === 0 || !selectedPOS) return;

    setIsSubmitting(true);
    try {
      const orderItems = Object.entries(cart).map(([productId, quantity]) => ({
        productId,
        quantity,
      }));

      const created = await submitOrderToBackend({
        pointOfSaleId: selectedPOS.id,
        couponCode: discountPercent > 0 ? couponCode : undefined,
        paymentType,
        items: orderItems,
        pointOfSale: selectedPOS,
        agentName: user?.fullName || 'Juan Manuel Torres',
        cartProducts: productMap,
      });

      setCart({});
      setIsCartOpen(false);
      setOrderPlacedMsg(`¡Pedido ${created.orderNumber} registrado exitosamente para ${selectedPOS.name}!`);

      setTimeout(() => {
        setOrderPlacedMsg(null);
        if (onOrderSuccess) onOrderSuccess();
      }, 2500);
    } catch (err) {
      console.error(err);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="view-container" style={{ textAlign: 'center', padding: '3rem' }}>
        <Loader2 className="animate-spin" size={36} style={{ color: 'var(--dm-primary-orange)', margin: '0 auto 1rem' }} />
        <p style={{ color: 'var(--dm-text-subtle)' }}>Cargando catálogo en tiempo real desde la base de datos...</p>
      </div>
    );
  }

  return (
    <div className="view-container">
      {/* Customer Header Selector */}
      {selectedPOS && (
        <div className="order-target-card">
          <div className="target-label">
            <Store size={15} />
            <span>Comercio Destino:</span>
          </div>
          <select
            value={selectedPOS.id}
            onChange={(e) => {
              const found = allPOS.find((p) => p.id === e.target.value);
              if (found) setSelectedPOS(found);
            }}
            className="pos-dropdown"
          >
            {allPOS.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} ({p.neighborhood}, {p.city})
              </option>
            ))}
          </select>
        </div>
      )}

      {/* Success Banner */}
      {orderPlacedMsg && (
        <div className="toast-banner success">
          <Check size={18} />
          <span>{orderPlacedMsg}</span>
        </div>
      )}

      {/* Search Bar */}
      <div className="search-box">
        <Search size={18} className="search-icon" />
        <input
          type="text"
          placeholder="Buscar producto por nombre o código..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="search-input"
        />
        {searchQuery && (
          <button onClick={() => setSearchQuery('')} className="search-clear">
            <X size={16} />
          </button>
        )}
      </div>

      {/* Brand Filters */}
      <div className="brand-pills-scroll">
        {brands.map((brand) => (
          <button
            key={brand}
            onClick={() => setSelectedBrand(brand)}
            className={`brand-pill ${selectedBrand === brand ? 'active' : ''}`}
          >
            {brand}
          </button>
        ))}
      </div>

      {/* Products Grid */}
      <div className="products-grid">
        {filteredProducts.map((prod) => {
          const inCartQty = cart[prod.id] || 0;
          return (
            <div key={prod.id} className="product-card">
              <div className="product-card-header">
                <span className="brand-tag">{prod.brand}</span>
                {prod.isOffer && <span className="offer-tag">OFERTA</span>}
              </div>

              <div className="product-info">
                <h4 className="product-title">{prod.name}</h4>
                <div className="product-meta">
                  <span>{prod.packaging}</span>
                  <span className="meta-dot">•</span>
                  <span>{prod.code}</span>
                </div>
              </div>

              <div className="product-pricing">
                <div className="bulk-price">${prod.bulkPrice.toLocaleString('es-AR')}</div>
                <div className="unit-price-hint">
                  ${prod.unitPrice.toLocaleString('es-AR')} / u.
                </div>
              </div>

              <div className="product-actions">
                {inCartQty === 0 ? (
                  <button
                    onClick={() => updateQuantity(prod.id, 1)}
                    className="btn btn-sm btn-outline-primary w-full"
                  >
                    <Plus size={16} />
                    <span>Agregar bulto</span>
                  </button>
                ) : (
                  <div className="qty-stepper">
                    <button
                      onClick={() => updateQuantity(prod.id, -1)}
                      className="stepper-btn minus"
                    >
                      <Minus size={14} />
                    </button>
                    <span className="stepper-value">{inCartQty}</span>
                    <button
                      onClick={() => updateQuantity(prod.id, 1)}
                      className="stepper-btn plus"
                    >
                      <Plus size={14} />
                    </button>
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Floating Bottom Cart Bar */}
      {totalPacks > 0 && !isCartOpen && (
        <div className="floating-cart-bar">
          <div className="cart-summary-pill" onClick={() => setIsCartOpen(true)}>
            <div className="cart-count-badge">{totalPacks}</div>
            <div className="cart-price-info">
              <span className="cart-label">Total estimado</span>
              <span className="cart-amount">${rawSubtotal.toLocaleString('es-AR')}</span>
            </div>
            <button className="cart-open-btn">
              <span>Ver pedido</span>
              <ArrowRight size={16} />
            </button>
          </div>
        </div>
      )}

      {/* Cart Drawer Modal */}
      {isCartOpen && (
        <div className="drawer-overlay" onClick={() => setIsCartOpen(false)}>
          <div className="drawer-panel" onClick={(e) => e.stopPropagation()}>
            <div className="drawer-header">
              <div>
                <h3>Resumen de Pedido</h3>
                <p className="drawer-subtitle">{selectedPOS?.name}</p>
              </div>
              <button onClick={() => setIsCartOpen(false)} className="drawer-close">
                <X size={20} />
              </button>
            </div>

            <div className="drawer-items">
              {cartItems.map((item) => (
                <div key={item.product.id} className="drawer-item-row">
                  <div className="item-details">
                    <span className="item-name">{item.product.name}</span>
                    <span className="item-sub">
                      {item.product.packaging} • ${item.product.bulkPrice.toLocaleString('es-AR')}
                    </span>
                  </div>
                  <div className="qty-stepper compact">
                    <button
                      onClick={() => updateQuantity(item.product.id, -1)}
                      className="stepper-btn"
                    >
                      <Minus size={12} />
                    </button>
                    <span className="stepper-value">{item.quantityPacks}</span>
                    <button
                      onClick={() => updateQuantity(item.product.id, 1)}
                      className="stepper-btn"
                    >
                      <Plus size={12} />
                    </button>
                  </div>
                  <div className="item-total">
                    ${item.subtotal.toLocaleString('es-AR')}
                  </div>
                </div>
              ))}
            </div>

            <div className="drawer-footer">
              {/* Coupon input */}
              <div className="coupon-box">
                <input
                  type="text"
                  placeholder="Cupón (ej. BIENVENIDA, ALFAJORFEST)"
                  value={couponCode}
                  onChange={(e) => setCouponCode(e.target.value)}
                  className="coupon-input"
                />
                <button onClick={handleApplyCoupon} className="btn btn-sm btn-secondary">
                  Aplicar
                </button>
              </div>
              {couponMsg && (
                <div className={`coupon-feedback ${couponMsg.ok ? 'ok' : 'err'}`}>
                  {couponMsg.text}
                </div>
              )}

              {/* Payment selector */}
              <div className="payment-select-row">
                <label>Forma de pago:</label>
                <div className="payment-pills">
                  <button
                    onClick={() => setPaymentType(PaymentType.CASH)}
                    className={`pay-pill ${paymentType === PaymentType.CASH ? 'active' : ''}`}
                  >
                    Efectivo
                  </button>
                  <button
                    onClick={() => setPaymentType(PaymentType.BANK_TRANSFER)}
                    className={`pay-pill ${paymentType === PaymentType.BANK_TRANSFER ? 'active' : ''}`}
                  >
                    Transferencia
                  </button>
                  <button
                    onClick={() => setPaymentType(PaymentType.DIGITAL_WALLET)}
                    className={`pay-pill ${paymentType === PaymentType.DIGITAL_WALLET ? 'active' : ''}`}
                  >
                    Billetera Virtual
                  </button>
                </div>
              </div>

              {/* Totals */}
              <div className="summary-breakdown">
                <div className="summary-line">
                  <span>Subtotal ({totalPacks} bultos)</span>
                  <span>${rawSubtotal.toLocaleString('es-AR')}</span>
                </div>
                {discountPercent > 0 && (
                  <div className="summary-line discount">
                    <span>Descuento ({discountPercent}%)</span>
                    <span>-${discountAmount.toLocaleString('es-AR')}</span>
                  </div>
                )}
                <div className="summary-line total">
                  <span>Total Final</span>
                  <span>${finalTotal.toLocaleString('es-AR')}</span>
                </div>
              </div>

              <button
                onClick={handleConfirmOrder}
                disabled={isSubmitting}
                className="btn btn-primary btn-block confirm-btn"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="animate-spin" size={18} />
                    <span>Guardando en Servidor...</span>
                  </>
                ) : (
                  <>
                    <Check size={18} />
                    <span>Confirmar y Enviar Pedido</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

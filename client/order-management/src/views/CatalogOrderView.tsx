import React, { useState } from 'react';
import { MOCK_PRODUCTS, getStoredPOS, saveStoredOrder } from '../services/api';
import type { PointOfSale, OrderItem, Order } from '../types/models';
import { OrderStatus, PaymentType } from '../types/enums';
import { useAuth } from '../context/AuthContext';
import {
  Search,
  Plus,
  Minus,
  Check,
  ArrowRight,
  Store,
  X,
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
  const allPOS = getStoredPOS();
  const [selectedPOS, setSelectedPOS] = useState<PointOfSale>(
    initialPOS || allPOS[0]
  );
  const [selectedBrand, setSelectedBrand] = useState<string>('TODOS');
  const [searchQuery, setSearchQuery] = useState('');
  const [cart, setCart] = useState<{ [productId: string]: number }>({});
  const [isCartOpen, setIsCartOpen] = useState(false);
  const [couponCode, setCouponCode] = useState('');
  const [discountPercent, setDiscountPercent] = useState(0);
  const [couponMsg, setCouponMsg] = useState<{ text: string; ok: boolean } | null>(null);
  const [paymentType, setPaymentType] = useState<PaymentType>(PaymentType.CASH);
  const [orderPlacedMsg, setOrderPlacedMsg] = useState<string | null>(null);

  const brands = ['TODOS', 'Arcor', 'Bagley', 'Mondelez', 'Terrabusi'];

  const filteredProducts = MOCK_PRODUCTS.filter((prod) => {
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

  const cartItems: OrderItem[] = Object.entries(cart)
    .map(([prodId, qty]) => {
      const product = MOCK_PRODUCTS.find((p) => p.id === prodId);
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
    if (code === 'DULCE10' || code === 'MAYORISTA10') {
      setDiscountPercent(10);
      setCouponMsg({ text: 'Cupón del 10% aplicado con éxito', ok: true });
    } else if (code === 'PROMO20') {
      setDiscountPercent(20);
      setCouponMsg({ text: 'Cupón especial 20% aplicado', ok: true });
    } else {
      setCouponMsg({ text: 'Cupón inválido o expirado', ok: false });
    }
  };

  const handleConfirmOrder = () => {
    if (cartItems.length === 0) return;

    const newOrder: Order = {
      id: `ord-${Date.now()}`,
      orderNumber: `ORD-${Math.floor(1000 + Math.random() * 9000)}`,
      date: new Date().toISOString().split('T')[0],
      pointOfSale: selectedPOS,
      items: cartItems,
      subtotal: rawSubtotal,
      discountPercentage: discountPercent,
      couponCode: discountPercent > 0 ? couponCode : undefined,
      total: finalTotal,
      status: OrderStatus.LOADED,
      paymentType,
      presaleAgentName: user?.fullName || 'Juan Manuel Torres',
    };

    saveStoredOrder(newOrder);
    setCart({});
    setIsCartOpen(false);
    setOrderPlacedMsg(`¡Pedido ${newOrder.orderNumber} cargado para ${selectedPOS.name}!`);

    setTimeout(() => {
      setOrderPlacedMsg(null);
      if (onOrderSuccess) onOrderSuccess();
    }, 3000);
  };

  return (
    <div className="view-container">
      {/* Customer Header Selector */}
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
              {p.name} — {p.address} ({p.neighborhood})
            </option>
          ))}
        </select>
      </div>

      {/* Confirmation Toast */}
      {orderPlacedMsg && (
        <div className="order-toast-success">
          <Check size={18} />
          <span>{orderPlacedMsg}</span>
        </div>
      )}

      {/* Search and Brand Filters */}
      <div className="catalog-filters">
        <div className="search-box">
          <Search size={16} className="search-icon" />
          <input
            type="text"
            placeholder="Buscar golosina o código..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="search-input"
          />
        </div>

        <div className="brand-chips-row">
          {brands.map((b) => (
            <button
              key={b}
              onClick={() => setSelectedBrand(b)}
              className={`brand-chip ${selectedBrand === b ? 'active' : ''}`}
            >
              {b}
            </button>
          ))}
        </div>
      </div>

      {/* Products Grid */}
      <div className="mobile-product-grid">
        {filteredProducts.map((prod) => {
          const qtyInCart = cart[prod.id] || 0;
          return (
            <div key={prod.id} className="mobile-product-card">
              <div className="prod-badge-row">
                <span className="packaging-tag">{prod.packaging}</span>
                {prod.isOffer && <span className="offer-tag">Oferta</span>}
              </div>

              <h4 className="prod-name">{prod.name}</h4>
              <span className="prod-brand-code">
                {prod.brand} • {prod.code}
              </span>

              <div className="prod-price-box">
                <div className="price-bulk">
                  ${prod.bulkPrice.toLocaleString('es-AR')}
                  <small className="per-pack"> /bulto</small>
                </div>
                <div className="price-unit">
                  (${prod.unitPrice.toFixed(0)} c/u)
                </div>
              </div>

              <div className="stock-info-row">
                <span className="stock-dot">●</span>
                <span className="stock-text">{prod.stockPacks} bultos en almacén</span>
              </div>

              {/* Quantity Picker */}
              <div className="card-cart-controls">
                {qtyInCart > 0 ? (
                  <div className="stepper-group">
                    <button
                      onClick={() => updateQuantity(prod.id, -1)}
                      className="stepper-btn"
                    >
                      <Minus size={14} />
                    </button>
                    <span className="stepper-val">{qtyInCart}</span>
                    <button
                      onClick={() => updateQuantity(prod.id, 1)}
                      className="stepper-btn"
                    >
                      <Plus size={14} />
                    </button>
                  </div>
                ) : (
                  <button
                    onClick={() => updateQuantity(prod.id, 1)}
                    className="btn-add-cart"
                  >
                    <Plus size={15} />
                    <span>Agregar</span>
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Persistent Bottom Bar when Cart has items */}
      {totalPacks > 0 && (
        <div className="floating-cart-bar">
          <div className="floating-cart-summary">
            <span className="floating-packs">{totalPacks} bultos</span>
            <span className="floating-total">${finalTotal.toLocaleString('es-AR')}</span>
          </div>
          <button onClick={() => setIsCartOpen(true)} className="btn-view-order">
            <span>Ver Pedido</span>
            <ArrowRight size={16} />
          </button>
        </div>
      )}

      {/* Cart Drawer Modal */}
      {isCartOpen && (
        <div className="cart-modal-backdrop" onClick={() => setIsCartOpen(false)}>
          <div className="cart-drawer" onClick={(e) => e.stopPropagation()}>
            <div className="drawer-header">
              <div>
                <h3 className="drawer-title">Resumen de Pedido</h3>
                <span className="drawer-sub">{selectedPOS.name}</span>
              </div>
              <button onClick={() => setIsCartOpen(false)} className="close-btn">
                <X size={18} />
              </button>
            </div>

            <div className="drawer-body">
              {cartItems.map((item) => (
                <div key={item.product.id} className="drawer-item-row">
                  <div className="item-info">
                    <span className="item-name">{item.product.name}</span>
                    <span className="item-pack-desc">
                      {item.quantityPacks} x ${item.product.bulkPrice.toLocaleString('es-AR')}
                    </span>
                  </div>
                  <div className="item-actions">
                    <button
                      onClick={() => updateQuantity(item.product.id, -1)}
                      className="stepper-btn-sm"
                    >
                      -
                    </button>
                    <span className="item-qty">{item.quantityPacks}</span>
                    <button
                      onClick={() => updateQuantity(item.product.id, 1)}
                      className="stepper-btn-sm"
                    >
                      +
                    </button>
                    <span className="item-subtotal">
                      ${item.subtotal.toLocaleString('es-AR')}
                    </span>
                  </div>
                </div>
              ))}

              {/* Coupon Section */}
              <div className="coupon-box">
                <div className="coupon-input-group">
                  <input
                    type="text"
                    placeholder="Código de cupón (ej: DULCE10)"
                    value={couponCode}
                    onChange={(e) => setCouponCode(e.target.value)}
                    className="coupon-input"
                  />
                  <button onClick={handleApplyCoupon} className="coupon-btn">
                    Aplicar
                  </button>
                </div>
                {couponMsg && (
                  <span className={`coupon-feedback ${couponMsg.ok ? 'success' : 'error'}`}>
                    {couponMsg.text}
                  </span>
                )}
              </div>

              {/* Payment Type */}
              <div className="payment-select-group">
                <label className="section-label">Medio de Pago Acordado:</label>
                <div className="payment-pills">
                  <button
                    type="button"
                    className={`pay-pill ${paymentType === PaymentType.CASH ? 'active' : ''}`}
                    onClick={() => setPaymentType(PaymentType.CASH)}
                  >
                    Efectivo
                  </button>
                  <button
                    type="button"
                    className={`pay-pill ${paymentType === PaymentType.BANK_TRANSFER ? 'active' : ''}`}
                    onClick={() => setPaymentType(PaymentType.BANK_TRANSFER)}
                  >
                    Transferencia
                  </button>
                  <button
                    type="button"
                    className={`pay-pill ${paymentType === PaymentType.DIGITAL_WALLET ? 'active' : ''}`}
                    onClick={() => setPaymentType(PaymentType.DIGITAL_WALLET)}
                  >
                    Billetera QR
                  </button>
                </div>
              </div>

              {/* Pricing Totals */}
              <div className="totals-breakdown">
                <div className="total-row">
                  <span>Subtotal:</span>
                  <span>${rawSubtotal.toLocaleString('es-AR')}</span>
                </div>
                {discountPercent > 0 && (
                  <div className="total-row discount">
                    <span>Descuento ({discountPercent}%):</span>
                    <span>-${discountAmount.toLocaleString('es-AR')}</span>
                  </div>
                )}
                <div className="total-row grand-total">
                  <span>Total Final:</span>
                  <span>${finalTotal.toLocaleString('es-AR')}</span>
                </div>
              </div>
            </div>

            <div className="drawer-footer">
              <button onClick={handleConfirmOrder} className="btn-primary btn-block">
                Confirmar y Transmitir Pedido
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

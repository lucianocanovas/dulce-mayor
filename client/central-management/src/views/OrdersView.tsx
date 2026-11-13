import React, { useEffect, useState } from 'react';
import { apiRequest, downloadOrderInvoicePdf } from '../services/api';
import { OrderStatus, PaymentType } from '../types/enums';
import './orders-badges.css';
import {
  RefreshCw,
  FileText,
  Download,
  Eye,
  Trash2,
  Edit2,
  X,
  AlertTriangle,
  Store,
  User,
  ShoppingBag,
  CheckCircle2,
  CreditCard,
  AlertCircle,
} from 'lucide-react';

const ORDER_STATUS_LABELS: Record<string, string> = {
  [OrderStatus.PENDING_PAYMENT]: 'Pendiente de Pago',
  [OrderStatus.PAID]: 'Pagado',
  [OrderStatus.LOADED]: 'Cargado',
  [OrderStatus.INVOICED]: 'Facturado',
  [OrderStatus.IN_DELIVERY]: 'En Reparto',
  [OrderStatus.DELIVERED]: 'Entregado',
  [OrderStatus.CANCELLED]: 'Cancelado',
};

const formatOrderStatus = (status: string): string =>
  ORDER_STATUS_LABELS[status] || status;

interface OrderDetailItem {
  id: string;
  productId: string;
  productCode: string;
  productName: string;
  packaging: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

interface OrderItem {
  id: string;
  orderNumber: number;
  orderDateTime: string;
  subtotal: number;
  discount: number;
  total: number;
  status: OrderStatus;
  storeName: string;
  retailerName: string;
  salesRepName: string;
  couponCode?: string;
  itemCount: number;
  items?: OrderDetailItem[];
  paymentType?: PaymentType | string;
  sourceChannel?: string;
  mpPaymentId?: string;
  mpPaymentStatus?: string;
  invoiceNumber?: number;
  readyForDispatch?: boolean;
  bankAlias?: string;
  deliveryAddress?: string;
}

export const OrdersView: React.FC = () => {
  const [orders, setOrders] = useState<OrderItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [modalError, setModalError] = useState<string | null>(null);

  // Modals state
  const [viewOrder, setViewOrder] = useState<OrderItem | null>(null);
  const [editStatusOrder, setEditStatusOrder] = useState<OrderItem | null>(null);
  const [newStatus, setNewStatus] = useState<OrderStatus>(OrderStatus.INVOICED);
  const [statusNote, setStatusNote] = useState('');
  const [confirmPayOrder, setConfirmPayOrder] = useState<OrderItem | null>(null);
  const [payRefInput, setPayRefInput] = useState('');
  const [payNoteInput, setPayNoteInput] = useState('');
  const [deleteOrder, setDeleteOrder] = useState<OrderItem | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [downloadingOrderId, setDownloadingOrderId] = useState<string | null>(null);

  const isOrderInvoiced = (o: OrderItem): boolean =>
    Boolean(o.invoiceNumber) ||
    o.status === OrderStatus.PAID ||
    o.status === OrderStatus.INVOICED ||
    o.status === OrderStatus.IN_DELIVERY ||
    o.status === OrderStatus.DELIVERED;

  const handleDownloadInvoice = async (o: OrderItem) => {
    try {
      setDownloadingOrderId(o.id);
      setModalError(null);
      await downloadOrderInvoicePdf(o.id, o.orderNumber, o.invoiceNumber);
    } catch (err: unknown) {
      setModalError(err instanceof Error ? err.message : 'No se pudo descargar la factura en PDF');
    } finally {
      setDownloadingOrderId(null);
    }
  };

  const fetchOrders = async () => {
    try {
      setLoading(true);
      const data = await apiRequest<OrderItem[]>('/orders');
      setOrders(data);
    } catch (err) {
      console.error('Failed to load orders', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, []);

  const handleUpdateStatus = async (orderId: string, status: OrderStatus, note?: string) => {
    try {
      setSubmitting(true);
      setModalError(null);
      await apiRequest(`/orders/${orderId}/status`, {
        method: 'PATCH',
        body: JSON.stringify({
          newStatus: status,
          note: note || `Estado actualizado a ${formatOrderStatus(status)} desde el panel de control`,
        }),
      });
      setEditStatusOrder(null);
      await fetchOrders();
    } catch (err: unknown) {
      setModalError(err instanceof Error ? err.message : 'Error al actualizar estado del pedido');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteOrder = async () => {
    if (!deleteOrder) return;
    try {
      setSubmitting(true);
      setModalError(null);
      await apiRequest(`/orders/${deleteOrder.id}`, {
        method: 'DELETE',
      });
      setDeleteOrder(null);
      await fetchOrders();
    } catch (err: unknown) {
      setModalError(err instanceof Error ? err.message : 'Error al cancelar pedido');
    } finally {
      setSubmitting(false);
    }
  };

  const handleConfirmManualPayment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!confirmPayOrder) return;
    try {
      setSubmitting(true);
      setModalError(null);
      await apiRequest(`/payments/orders/${confirmPayOrder.id}/confirm-manual`, {
        method: 'POST',
        body: JSON.stringify({
          paymentType: confirmPayOrder.paymentType || PaymentType.BANK_TRANSFER,
          reference: payRefInput.trim() || `TRF-${confirmPayOrder.orderNumber}`,
          note: payNoteInput.trim() || 'Confirmación administrativa de transferencia/pago',
        }),
      });
      setConfirmPayOrder(null);
      setPayRefInput('');
      setPayNoteInput('');
      await fetchOrders();
    } catch (err: unknown) {
      setModalError(err instanceof Error ? err.message : 'Error al confirmar el pago');
    } finally {
      setSubmitting(false);
    }
  };

  const formatPaymentLabel = (type?: string) => {
    switch (type) {
      case PaymentType.MERCADO_PAGO:
        return 'Mercado Pago';
      case PaymentType.DIGITAL_WALLET:
        return 'Billetera QR (MP)';
      case PaymentType.BANK_TRANSFER:
        return 'Transferencia';
      case PaymentType.CASH:
      default:
        return 'Efectivo';
    }
  };

  const filteredOrders = orders.filter(
    (o) => selectedStatus === 'ALL' || o.status === selectedStatus
  );

  return (
    <div className="orders-view">
      <div className="view-header">
        <div>
          <h2>Gestión de Pedidos, Entregas y Facturación</h2>
          <p className="text-muted">
            Supervisión integral de pedidos captados por preventistas, detalle de ítems y despacho
          </p>
        </div>
        <button onClick={fetchOrders} className="btn btn-secondary">
          <RefreshCw size={16} /> Actualizar
        </button>
      </div>

      {/* Status Filter Tabs */}
      <div className="filter-tabs">
        {[
          'ALL',
          OrderStatus.PENDING_PAYMENT,
          OrderStatus.PAID,
          OrderStatus.LOADED,
          OrderStatus.INVOICED,
          OrderStatus.IN_DELIVERY,
          OrderStatus.DELIVERED,
          OrderStatus.CANCELLED,
        ].map((st) => (
          <button
            key={st}
            className={`filter-tab-btn ${selectedStatus === st ? 'active' : ''}`}
            onClick={() => setSelectedStatus(st)}
          >
            {st === 'ALL' ? 'Todos los Pedidos' : formatOrderStatus(st)}
          </button>
        ))}
      </div>

      {/* Orders Table */}
      {loading ? (
        <div className="view-loading">
          <RefreshCw className="animate-spin" size={24} />
          <p>Cargando lista de pedidos...</p>
        </div>
      ) : (
        <div className="panel">
          <div className="panel-body no-padding table-responsive">
            {filteredOrders.length === 0 ? (
              <p className="text-muted text-center py-8">
                No hay pedidos en la categoría seleccionada.
              </p>
            ) : (
              <table className="data-table">
                <thead>
                  <tr>
                    <th># Pedido</th>
                    <th>Fecha / Hora</th>
                    <th>Comercio / Canal</th>
                    <th>Preventor</th>
                    <th>Pago / Factura</th>
                    <th>Ítems</th>
                    <th>Total</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredOrders.map((o) => (
                    <tr key={o.id}>
                      <td className="font-mono font-bold text-primary">#{o.orderNumber}</td>
                      <td>{new Date(o.orderDateTime).toLocaleString('es-AR')}</td>
                      <td>
                        <strong>{o.storeName}</strong>
                        <div className="text-xs text-muted">
                          {o.retailerName} • {o.sourceChannel === 'LANDING' ? 'Tienda Web' : 'Preventa'}
                        </div>
                      </td>
                      <td>{o.salesRepName}</td>
                      <td>
                        <div className="text-xs font-semibold">{formatPaymentLabel(o.paymentType)}</div>
                        <div className="text-xs text-muted">
                          {o.invoiceNumber
                            ? `Factura #${o.invoiceNumber}`
                            : o.paymentType === PaymentType.BANK_TRANSFER && o.bankAlias
                            ? `Alias: ${o.bankAlias}`
                            : o.readyForDispatch
                            ? 'Listo despacho'
                            : 'Sin facturar'}
                        </div>
                      </td>
                      <td>
                        <span className="badge badge-neutral">
                          {o.itemCount} {o.itemCount === 1 ? 'unidad' : 'unidades'}
                        </span>
                      </td>
                      <td className="font-bold text-lg">${o.total.toLocaleString('es-AR')}</td>
                      <td>
                        <span className={`badge badge-${o.status.toLowerCase()}`}>
                          {formatOrderStatus(o.status)}
                        </span>
                      </td>
                      <td>
                        <div className="action-btn-group">
                          <button
                            onClick={() => {
                              setModalError(null);
                              setViewOrder(o);
                            }}
                            className="action-icon-btn view"
                            title="Ver detalles completos del pedido"
                          >
                            <Eye size={14} />
                            <span>Ver</span>
                          </button>

                          {isOrderInvoiced(o) && (
                            <button
                              onClick={() => handleDownloadInvoice(o)}
                              disabled={downloadingOrderId === o.id}
                              className="action-icon-btn view"
                              title="Descargar Factura Electrónica (PDF)"
                            >
                              <Download size={14} />
                              <span>{downloadingOrderId === o.id ? 'PDF...' : 'Factura'}</span>
                            </button>
                          )}

                          {o.status === OrderStatus.PENDING_PAYMENT && (
                            <button
                              onClick={() => {
                                setModalError(null);
                                setConfirmPayOrder(o);
                                setPayRefInput(`TRF-${o.orderNumber}`);
                                setPayNoteInput('');
                              }}
                              className="action-icon-btn edit"
                              title="Confirmar acreditación manual (Transferencia / Pago)"
                            >
                              <CheckCircle2 size={14} />
                              <span>Confirmar</span>
                            </button>
                          )}

                          {o.status !== OrderStatus.CANCELLED && (
                            <button
                              onClick={() => {
                                setModalError(null);
                                setEditStatusOrder(o);
                                setNewStatus(o.status);
                                setStatusNote('');
                              }}
                              className="action-icon-btn edit"
                              title="Cambiar estado del pedido"
                            >
                              <Edit2 size={14} />
                              <span>Estado</span>
                            </button>
                          )}

                          {o.status !== OrderStatus.CANCELLED && (
                            <button
                              onClick={() => {
                                setModalError(null);
                                setDeleteOrder(o);
                              }}
                              className="action-icon-btn delete"
                              title="Anular y eliminar pedido"
                            >
                              <Trash2 size={14} />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      )}

      {/* Modal: VER DETALLE DEL PEDIDO */}
      {viewOrder && (
        <div className="modal-backdrop" onClick={() => setViewOrder(null)}>
          <div className="modal-card modal-lg" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header-row">
              <div>
                <h3 style={{ margin: 0 }}>Detalle del Pedido #{viewOrder.orderNumber}</h3>
                <span className="text-muted text-xs">
                  Registrado el {new Date(viewOrder.orderDateTime).toLocaleString('es-AR')}
                </span>
              </div>
              <button onClick={() => setViewOrder(null)} className="modal-close-btn">
                <X size={20} />
              </button>
            </div>

            <div className="details-grid">
              <div className="detail-item">
                <span className="detail-label">Punto de Venta / Comercio</span>
                <span className="detail-value font-semibold flex items-center gap-1">
                  <Store size={15} className="text-primary" /> {viewOrder.storeName}
                </span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Titular / Cliente</span>
                <span className="detail-value flex items-center gap-1">
                  <User size={15} className="text-muted" /> {viewOrder.retailerName}
                </span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Preventista Asignado</span>
                <span className="detail-value">{viewOrder.salesRepName}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Estado Actual</span>
                <span className="detail-value">
                  <span className={`badge badge-${viewOrder.status.toLowerCase()}`}>
                    {formatOrderStatus(viewOrder.status)}
                  </span>
                </span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Medio de Pago / Canal</span>
                <span className="detail-value">
                  {formatPaymentLabel(viewOrder.paymentType)} • {viewOrder.sourceChannel === 'LANDING' ? 'Tienda Web' : 'Preventa Campo'}
                </span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Comprobante / Ref.</span>
                <span className="detail-value">
                  {viewOrder.invoiceNumber ? `Factura #${viewOrder.invoiceNumber}` : 'Pendiente'}{' '}
                  {viewOrder.mpPaymentId ? `(Ref: ${viewOrder.mpPaymentId})` : ''}
                </span>
              </div>
            </div>

            {/* Items Table */}
            <h4 style={{ margin: '1rem 0 0.5rem', display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.95rem' }}>
              <ShoppingBag size={16} /> Productos incluidos en la orden
            </h4>

            <div className="items-table-container">
              <table className="items-subtable">
                <thead>
                  <tr>
                    <th>Código</th>
                    <th>Producto</th>
                    <th>Presentación</th>
                    <th>Cantidad</th>
                    <th>Precio Unitario</th>
                    <th>Subtotal</th>
                  </tr>
                </thead>
                <tbody>
                  {viewOrder.items && viewOrder.items.length > 0 ? (
                    viewOrder.items.map((item) => (
                      <tr key={item.id}>
                        <td className="font-mono text-xs">{item.productCode}</td>
                        <td className="font-semibold">{item.productName}</td>
                        <td className="text-muted">{item.packaging || 'Unidad'}</td>
                        <td className="font-bold">{item.quantity} u.</td>
                        <td>${item.unitPrice.toLocaleString('es-AR')}</td>
                        <td className="font-semibold">${item.subtotal.toLocaleString('es-AR')}</td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={6} style={{ textAlign: 'center', padding: '1.5rem', color: 'var(--text-muted)' }}>
                        Pedido mayorista global ({viewOrder.itemCount} {viewOrder.itemCount === 1 ? 'unidad' : 'unidades'} en total).
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>

            {/* Financial Summary */}
            <div style={{ background: 'var(--bg-main)', padding: '1rem', borderRadius: '8px', border: '1px solid var(--border)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.35rem', fontSize: '0.9rem' }}>
                <span className="text-muted">Subtotal Bruto:</span>
                <span>${viewOrder.subtotal.toLocaleString('es-AR')}</span>
              </div>
              {viewOrder.discount > 0 && (
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.35rem', color: 'var(--success)', fontSize: '0.9rem', fontWeight: 600 }}>
                  <span>Descuento aplicado ({viewOrder.couponCode || 'Cupón'}):</span>
                  <span>-${viewOrder.discount.toLocaleString('es-AR')}</span>
                </div>
              )}
              <div style={{ display: 'flex', justifyContent: 'space-between', borderTop: '1px solid var(--border)', paddingTop: '0.5rem', fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-main)' }}>
                <span>Total Facturado:</span>
                <span className="text-primary">${viewOrder.total.toLocaleString('es-AR')}</span>
              </div>
            </div>

            {modalError && (
              <div className="modal-error-banner" style={{ marginTop: '0.75rem' }}>
                <AlertCircle size={18} style={{ flexShrink: 0, marginTop: '1px' }} />
                <span>{modalError}</span>
              </div>
            )}

            <div className="modal-actions" style={{ marginTop: '1.25rem' }}>
              {isOrderInvoiced(viewOrder) && (
                <button
                  type="button"
                  onClick={() => handleDownloadInvoice(viewOrder)}
                  disabled={downloadingOrderId === viewOrder.id}
                  className="btn btn-primary"
                >
                  <Download size={16} />{' '}
                  {downloadingOrderId === viewOrder.id
                    ? 'Generando Factura PDF...'
                    : `Descargar Factura PDF${viewOrder.invoiceNumber ? ` (#${viewOrder.invoiceNumber})` : ''}`}
                </button>
              )}
              {viewOrder.status === OrderStatus.PENDING_PAYMENT && (
                <button
                  onClick={() => {
                    setModalError(null);
                    setConfirmPayOrder(viewOrder);
                    setPayRefInput(`TRF-${viewOrder.orderNumber}`);
                    setPayNoteInput('');
                    setViewOrder(null);
                  }}
                  className="btn btn-primary"
                >
                  <CreditCard size={16} /> Confirmar Pago y Facturar
                </button>
              )}
              {viewOrder.status === OrderStatus.LOADED && (
                <button
                  onClick={() => {
                    setModalError(null);
                    setEditStatusOrder(viewOrder);
                    setNewStatus(OrderStatus.INVOICED);
                    setStatusNote('Facturado desde visualización de detalles');
                    setViewOrder(null);
                  }}
                  className="btn btn-primary"
                >
                  <FileText size={16} /> Facturar Pedido
                </button>
              )}
              <button onClick={() => setViewOrder(null)} className="btn btn-secondary">
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: CONFIRMAR PAGO MANUAL (TRANSFERENCIA / ADMIN) */}
      {confirmPayOrder && (
        <div className="modal-backdrop" onClick={() => setConfirmPayOrder(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header-row">
              <h3>Confirmar Pago Pedido #{confirmPayOrder.orderNumber}</h3>
              <button onClick={() => setConfirmPayOrder(null)} className="modal-close-btn">
                <X size={20} />
              </button>
            </div>

            {modalError && (
              <div className="modal-error-banner">
                <AlertCircle size={18} style={{ flexShrink: 0, marginTop: '1px' }} />
                <span>{modalError}</span>
              </div>
            )}

            <p className="text-muted" style={{ fontSize: '0.88rem', marginBottom: '0.9rem' }}>
              Al confirmar el pago de <strong>${confirmPayOrder.total.toLocaleString('es-AR')}</strong> ({formatPaymentLabel(confirmPayOrder.paymentType)}), se descontará el stock, se emitirá la factura electrónica y el pedido pasará a <strong>Distribución</strong>.
            </p>
            <form onSubmit={handleConfirmManualPayment} className="modal-form">
              <label>
                Referencia / Comprobante Bancario *:
                <input
                  type="text"
                  value={payRefInput}
                  onChange={(e) => setPayRefInput(e.target.value)}
                  placeholder="Ej. TRF-00491823"
                  required
                />
              </label>
              <label>
                Observación Administrativa:
                <input
                  type="text"
                  value={payNoteInput}
                  onChange={(e) => setPayNoteInput(e.target.value)}
                  placeholder="Transferencia acreditada en cuenta corriente"
                />
              </label>
              <div className="modal-actions">
                <button type="submit" disabled={submitting} className="btn btn-primary">
                  {submitting ? 'Acreditando...' : 'Acreditar, Facturar y Despachar'}
                </button>
                <button
                  type="button"
                  onClick={() => setConfirmPayOrder(null)}
                  className="btn btn-secondary"
                >
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: EDITAR ESTADO */}
      {editStatusOrder && (
        <div className="modal-backdrop" onClick={() => setEditStatusOrder(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header-row">
              <h3>Actualizar Estado del Pedido #{editStatusOrder.orderNumber}</h3>
              <button onClick={() => setEditStatusOrder(null)} className="modal-close-btn">
                <X size={20} />
              </button>
            </div>

            {modalError && (
              <div className="modal-error-banner">
                <AlertCircle size={18} style={{ flexShrink: 0, marginTop: '1px' }} />
                <span>{modalError}</span>
              </div>
            )}

            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleUpdateStatus(editStatusOrder.id, newStatus, statusNote);
              }}
              className="modal-form"
            >
              <label>
                Nuevo Estado Operativo:
                <select
                  value={newStatus}
                  onChange={(e) => setNewStatus(e.target.value as OrderStatus)}
                  style={{ width: '100%', padding: '0.65rem', borderRadius: '6px', border: '1px solid var(--border)' }}
                >
                  <option value={OrderStatus.PENDING_PAYMENT}>Pendiente de Pago (Esperando Acreditación)</option>
                  <option value={OrderStatus.PAID}>Pagado (Pago Acreditado)</option>
                  <option value={OrderStatus.LOADED}>Cargado (Captado en Preventa)</option>
                  <option value={OrderStatus.INVOICED}>Facturado (Deducción de Stock)</option>
                  <option value={OrderStatus.IN_DELIVERY}>En Reparto (En Camión de Distribución)</option>
                  <option value={OrderStatus.DELIVERED}>Entregado (Entregado y Cobrado)</option>
                  <option value={OrderStatus.CANCELLED}>Cancelado (Anulado)</option>
                </select>
              </label>

              <label>
                Nota de Auditoría / Motivo:
                <input
                  type="text"
                  value={statusNote}
                  onChange={(e) => setStatusNote(e.target.value)}
                  placeholder="Ej. Factura A emitida por administración"
                />
              </label>

              <div className="modal-actions">
                <button type="submit" disabled={submitting} className="btn btn-primary">
                  {submitting ? 'Guardando...' : 'Actualizar Estado'}
                </button>
                <button
                  type="button"
                  onClick={() => setEditStatusOrder(null)}
                  className="btn btn-secondary"
                >
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: ELIMINAR / ANULAR PEDIDO */}
      {deleteOrder && (
        <div className="modal-backdrop" onClick={() => setDeleteOrder(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1rem', color: 'var(--danger)' }}>
              <AlertTriangle size={28} />
              <h3 style={{ margin: 0, color: 'var(--text-main)' }}>¿Anular Pedido #{deleteOrder.orderNumber}?</h3>
            </div>

            {modalError && (
              <div className="modal-error-banner">
                <AlertCircle size={18} style={{ flexShrink: 0, marginTop: '1px' }} />
                <span>{modalError}</span>
              </div>
            )}

            <p style={{ marginBottom: '1rem', color: 'var(--text-muted)' }}>
              Esta acción marcará el pedido como cancelado y lo removerá del listado operativo de preventa y logística.
            </p>
            <div className="modal-actions">
              <button
                type="button"
                onClick={handleDeleteOrder}
                disabled={submitting}
                className="btn btn-danger-outline"
                style={{ background: 'var(--danger)', color: '#fff', border: 'none' }}
              >
                {submitting ? 'Anulando...' : 'Sí, Anular Pedido'}
              </button>
              <button
                type="button"
                onClick={() => setDeleteOrder(null)}
                className="btn btn-secondary"
              >
                Volver
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

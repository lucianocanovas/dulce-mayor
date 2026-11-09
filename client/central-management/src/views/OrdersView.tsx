import React, { useEffect, useState } from 'react';
import { apiRequest } from '../services/api';
import { OrderStatus } from '../types/enums';
import { RefreshCw, FileText, CheckCircle2, XCircle } from 'lucide-react';

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
}

export const OrdersView: React.FC = () => {
  const [orders, setOrders] = useState<OrderItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');

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

  const handleUpdateStatus = async (orderId: string, newStatus: OrderStatus) => {
    try {
      await apiRequest(`/orders/${orderId}/status`, {
        method: 'PATCH',
        body: JSON.stringify({
          newStatus,
          note: `Actualizado desde Dashboard Central a ${newStatus}`,
        }),
      });
      await fetchOrders();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Error updating status');
    }
  };

  const filteredOrders = orders.filter(
    (o) => selectedStatus === 'ALL' || o.status === selectedStatus
  );

  return (
    <div className="orders-view">
      <div className="view-header">
        <div>
          <h2>Gestión de Pedidos y Facturación</h2>
          <p className="text-muted">
            Supervisión de pedidos captados por preventistas y facturación automática
          </p>
        </div>
        <button onClick={fetchOrders} className="btn btn-secondary">
          <RefreshCw size={16} /> Actualizar
        </button>
      </div>

      {/* Status Filter Tabs */}
      <div className="filter-tabs">
        {['ALL', OrderStatus.LOADED, OrderStatus.INVOICED, OrderStatus.IN_DELIVERY, OrderStatus.DELIVERED, OrderStatus.CANCELLED].map((st) => (
          <button
            key={st}
            className={`filter-tab-btn ${selectedStatus === st ? 'active' : ''}`}
            onClick={() => setSelectedStatus(st)}
          >
            {st === 'ALL' ? 'Todos los Pedidos' : st}
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
          <div className="panel-body no-padding">
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
                    <th>Comercio (Punto de Venta)</th>
                    <th>Preventor</th>
                    <th>Subtotal</th>
                    <th>Descuento</th>
                    <th>Total Facturado</th>
                    <th>Estado</th>
                    <th>Acciones Administrativas</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredOrders.map((o) => (
                    <tr key={o.id}>
                      <td className="font-mono font-bold text-primary">#{o.orderNumber}</td>
                      <td>{new Date(o.orderDateTime).toLocaleString('es-AR')}</td>
                      <td>
                        <strong>{o.storeName}</strong>
                        <div className="text-xs text-muted">Cliente: {o.retailerName}</div>
                      </td>
                      <td>{o.salesRepName}</td>
                      <td>${o.subtotal.toLocaleString('es-AR')}</td>
                      <td>
                        {o.discount > 0 ? (
                          <span className="text-success font-semibold">
                            -${o.discount.toLocaleString('es-AR')} ({o.couponCode})
                          </span>
                        ) : (
                          '-'
                        )}
                      </td>
                      <td className="font-bold text-lg">${o.total.toLocaleString('es-AR')}</td>
                      <td>
                        <span className={`badge badge-${o.status.toLowerCase()}`}>
                          {o.status}
                        </span>
                      </td>
                      <td>
                        <div className="order-actions-cell">
                          {o.status === OrderStatus.LOADED && (
                            <button
                              onClick={() => handleUpdateStatus(o.id, OrderStatus.INVOICED)}
                              className="btn btn-sm btn-primary"
                              title="Generar factura y descontar stock del depósito"
                            >
                              <FileText size={14} /> Facturar
                            </button>
                          )}

                          {o.status === OrderStatus.INVOICED && (
                            <button
                              onClick={() => handleUpdateStatus(o.id, OrderStatus.IN_DELIVERY)}
                              className="btn btn-sm btn-outline"
                            >
                              <CheckCircle2 size={14} /> Despachar
                            </button>
                          )}

                          {o.status !== OrderStatus.CANCELLED && o.status !== OrderStatus.DELIVERED && (
                            <button
                              onClick={() => handleUpdateStatus(o.id, OrderStatus.CANCELLED)}
                              className="btn btn-sm btn-danger-outline"
                              title="Anular pedido"
                            >
                              <XCircle size={14} />
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
    </div>
  );
};

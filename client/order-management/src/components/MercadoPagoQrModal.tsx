import React, { useEffect, useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { CheckCircle2, ExternalLink, Loader2, QrCode, RefreshCw, X } from 'lucide-react';
import type { OrderPaymentStatus } from '../types/models';
import { OrderStatus } from '../types/enums';
import { pollOrderPaymentStatus } from '../services/api';
import './payments.css';

interface MercadoPagoQrModalProps {
  orderId: string;
  orderNumber: string | number;
  storeName: string;
  total: number;
  initPoint?: string;
  qrImageBase64?: string;
  onClose: () => void;
  onPaymentAccredited?: (status: OrderPaymentStatus) => void;
}

export const MercadoPagoQrModal: React.FC<MercadoPagoQrModalProps> = ({
  orderId,
  orderNumber,
  storeName,
  total,
  initPoint,
  qrImageBase64,
  onClose,
  onPaymentAccredited,
}) => {
  const [paymentStatus, setPaymentStatus] = useState<OrderPaymentStatus | null>(null);
  const [isChecking, setIsChecking] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [notifiedAccredited, setNotifiedAccredited] = useState(false);

  const effectiveInitPoint = paymentStatus?.mpInitPoint || initPoint;
  const isPaid = Boolean(
    paymentStatus?.paid ||
      paymentStatus?.orderStatus === OrderStatus.PAID ||
      paymentStatus?.orderStatus === OrderStatus.INVOICED
  );

  const checkStatus = async (explicitTrigger = false) => {
    setIsChecking(true);
    if (explicitTrigger) {
      setErrorMsg(null);
    }
    try {
      const status = await pollOrderPaymentStatus(orderId);
      setPaymentStatus(status);
      if (status.paid && !notifiedAccredited) {
        setNotifiedAccredited(true);
        if (onPaymentAccredited) {
          onPaymentAccredited(status);
        }
      }
    } catch (err: unknown) {
      if (explicitTrigger) {
        const msg = err instanceof Error ? err.message : 'Error al consultar el estado del pago.';
        setErrorMsg(msg);
      } else {
        console.warn('Error polling MP status:', err);
      }
    } finally {
      setIsChecking(false);
    }
  };

  useEffect(() => {
    checkStatus(false);
    const interval = setInterval(() => {
      checkStatus(false);
    }, 3500);
    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orderId]);

  return (
    <div className="mp-modal-backdrop" role="dialog" aria-modal="true" aria-label="Cobro QR Mercado Pago">
      <div className="mp-modal-card">
        <div className="mp-modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <QrCode size={20} style={{ color: '#F58721' }} />
            <h3 className="mp-modal-title">Cobro QR Mercado Pago</h3>
          </div>
          <button type="button" onClick={onClose} className="mp-modal-close" aria-label="Cerrar">
            <X size={20} />
          </button>
        </div>

        <div className="mp-modal-body">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div style={{ fontSize: '0.82rem', color: '#7A6254' }}>
                Pedido #{String(orderNumber).replace('ORD-', '')}
              </div>
              <div style={{ fontWeight: 700, color: '#2B1810' }}>{storeName}</div>
            </div>
            <div className="mp-amount-badge">${total.toLocaleString('es-AR')}</div>
          </div>

          {isPaid ? (
            <div className="mp-paid-box">
              <CheckCircle2 size={42} style={{ margin: '0 auto 0.45rem', color: '#059669' }} />
              <div style={{ fontSize: '1.15rem', fontWeight: 800 }}>¡Pago Acreditado!</div>
              <p style={{ fontSize: '0.86rem', margin: '0.35rem 0 0.65rem' }}>
                El cobro fue verificado en Mercado Pago, la factura fue emitida y la orden ya figura en{' '}
                <strong>Distribución / Reparto</strong>.
              </p>
              <div style={{ fontSize: '0.84rem', display: 'flex', flexDirection: 'column', gap: '0.2rem' }}>
                {paymentStatus?.invoiceNumber && (
                  <span>
                    <strong>Factura Electrónica:</strong> #{paymentStatus.invoiceNumber}
                  </span>
                )}
                {paymentStatus?.mpPaymentId && (
                  <span>
                    <strong>Referencia MP:</strong> {paymentStatus.mpPaymentId}
                  </span>
                )}
                <span>
                  <strong>Stock descontado:</strong> {paymentStatus?.stockDeducted ? 'Sí' : 'Procesado'}
                </span>
              </div>
              <button
                type="button"
                onClick={onClose}
                className="btn btn-primary btn-block"
                style={{ marginTop: '0.9rem' }}
              >
                Continuar
              </button>
            </div>
          ) : (
            <div className="mp-qr-frame">
              {effectiveInitPoint ? (
                <>
                  <div style={{ background: '#fff', padding: '0.75rem', borderRadius: '10px' }}>
                    <QRCodeSVG value={effectiveInitPoint} size={210} level="M" />
                  </div>
                  <div style={{ fontSize: '0.8rem', color: '#5C4033', textAlign: 'center' }}>
                    El cliente debe escanear este QR con su celular o billetera de Mercado Pago para abonar desde su dispositivo.
                  </div>
                  <a
                    href={effectiveInitPoint}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="btn btn-sm btn-outline-secondary"
                    style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}
                  >
                    <ExternalLink size={14} />
                    <span>Abrir enlace Checkout Pro en nueva pestaña</span>
                  </a>
                </>
              ) : qrImageBase64 ? (
                <img src={qrImageBase64} alt="QR Mercado Pago" style={{ width: 210, height: 210 }} />
              ) : (
                <div style={{ textAlign: 'center', fontSize: '0.85rem', color: '#991B1B', padding: '0.75rem' }}>
                  No se obtuvo enlace <code>init_point</code> desde la API de Mercado Pago.
                </div>
              )}

              <div className="mp-polling-pill">
                {isChecking ? <Loader2 size={14} className="animate-spin" /> : <RefreshCw size={14} />}
                <span>Verificando acreditación automáticamente cada 3.5s...</span>
              </div>

              <button
                type="button"
                onClick={() => checkStatus(true)}
                disabled={isChecking}
                className="btn btn-sm btn-ghost"
                style={{ fontSize: '0.78rem' }}
              >
                Consultar estado ahora en Mercado Pago
              </button>

              {errorMsg && (
                <div style={{ color: '#DC2626', fontSize: '0.8rem', fontWeight: 600, textAlign: 'center' }}>
                  {errorMsg}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

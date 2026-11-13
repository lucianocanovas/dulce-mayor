const API_BASE_URL = (import.meta.env.VITE_API_URL as string) || 'http://localhost:8080/api';

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
}

export async function apiRequest<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('dulcemayor_token');
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });
  } catch (err: unknown) {
    throw new Error('No se pudo conectar con el servidor backend (puerto 8080).');
  }

  let json: ApiResponse<T>;
  try {
    json = await response.json();
  } catch {
    throw new Error(`Respuesta inválida del servidor (Status ${response.status})`);
  }

  if (!response.ok || !json.success) {
    throw new Error(json.message || `Error en la solicitud (Status ${response.status})`);
  }

  return json.data;
}

export async function downloadOrderInvoicePdf(
  orderId: string,
  orderNumber: number,
  invoiceNumber?: number
): Promise<void> {
  const token = localStorage.getItem('dulcemayor_token');
  const headers: Record<string, string> = {};
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}/payments/orders/${encodeURIComponent(orderId)}/invoice/pdf`, {
      method: 'GET',
      headers,
    });
  } catch {
    throw new Error('No se pudo conectar con el servidor backend para descargar la factura.');
  }

  if (!response.ok) {
    let errorMsg = `No se pudo descargar la factura (Status ${response.status})`;
    try {
      const errJson = await response.json();
      if (errJson && errJson.message) {
        errorMsg = errJson.message;
      }
    } catch {
      // Ignore JSON parse error
    }
    throw new Error(errorMsg);
  }

  const blob = await response.blob();
  const disposition = response.headers.get('Content-Disposition');
  let filename = invoiceNumber
    ? `Factura-00004-${String(invoiceNumber).padStart(8, '0')}.pdf`
    : `Factura-Pedido-${orderNumber}.pdf`;

  if (disposition) {
    const match = disposition.match(/filename="?([^";]+)"?/i);
    if (match && match[1]) {
      filename = match[1];
    }
  }

  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}

import type {
  DailyRoute,
  Order,
  PointOfSale,
  Product,
  RouteStop,
  PaymentConfig,
  OrderPaymentStatus,
  MercadoPagoQrResponse,
  BackendOrderSummary,
} from '../types/models';
import { RouteStatus, UserRole, OrderStatus, PaymentType } from '../types/enums';

const API_BASE_URL = (import.meta.env.VITE_API_URL as string) || 'http://localhost:8080/api';

// Helper to deduce brand from product code or name
function deduceBrand(name: string, code: string): string {
  if (code.startsWith('ARC') || name.toLowerCase().includes('arcor') || name.toLowerCase().includes('bon o bon') || name.toLowerCase().includes('mogul') || name.toLowerCase().includes('topline') || name.toLowerCase().includes('cofler') || name.toLowerCase().includes('rocklets')) {
    return 'Arcor';
  }
  if (code.startsWith('BAG') || name.toLowerCase().includes('bagley') || name.toLowerCase().includes('chocolinas') || name.toLowerCase().includes('sonrisas') || name.toLowerCase().includes('criollitas')) {
    return 'Bagley';
  }
  if (code.startsWith('MDZ') || name.toLowerCase().includes('mondelez') || name.toLowerCase().includes('oreo') || name.toLowerCase().includes('milka') || name.toLowerCase().includes('beldent')) {
    return 'Mondelez';
  }
  if (code.startsWith('TER') || name.toLowerCase().includes('terrabusi') || name.toLowerCase().includes('rhodesia') || name.toLowerCase().includes('tita')) {
    return 'Terrabusi';
  }
  return 'Arcor';
}

// Initial Fallback Mock Points of Sale in Greater Mendoza
export const MOCK_POINTS_OF_SALE: PointOfSale[] = [
  {
    id: 'pos-1',
    name: 'Kiosco El Trébol',
    ownerName: 'Roberto Gómez',
    taxIdOrDni: '20-28491823-4',
    phone: '261-4239841',
    address: 'Av. San Martín 1420',
    neighborhood: 'Centro',
    city: 'Mendoza Capital',
    latitude: -32.8895,
    longitude: -68.8458,
    hasActiveOrder: true,
  },
  {
    id: 'pos-2',
    name: 'Almacén Dulce Hogar',
    ownerName: 'Marta Villalobos',
    taxIdOrDni: '27-31298451-2',
    phone: '261-5128794',
    address: 'Belgrano 840',
    neighborhood: 'Bombal',
    city: 'Godoy Cruz',
    latitude: -32.9052,
    longitude: -68.8431,
    hasActiveOrder: true,
  },
  {
    id: 'pos-3',
    name: 'Golosinas & Kiosco Central',
    ownerName: 'Esteban Paz',
    taxIdOrDni: '20-33418729-1',
    phone: '261-6981240',
    address: 'Bandera de los Andes 2150',
    neighborhood: 'San José',
    city: 'Guaymallén',
    latitude: -32.8942,
    longitude: -68.8189,
    hasActiveOrder: true,
  },
  {
    id: 'pos-4',
    name: 'Kiosco 24 Horas La Plaza',
    ownerName: 'Carla Ferreyra',
    taxIdOrDni: '27-35619482-9',
    phone: '261-4820193',
    address: 'Pescara 350',
    neighborhood: 'Centro',
    city: 'Maipú',
    latitude: -32.9814,
    longitude: -68.7845,
    hasActiveOrder: true,
  },
  {
    id: 'pos-5',
    name: 'Kiosco El Chiche',
    ownerName: 'Luciano Rossi',
    taxIdOrDni: '20-29841720-3',
    phone: '261-5412980',
    address: 'San Miguel 1120',
    neighborhood: 'Centro',
    city: 'Las Heras',
    latitude: -32.8521,
    longitude: -68.8312,
    hasActiveOrder: false,
  },
];

// Initial Fallback Mock Products
export const MOCK_PRODUCTS: Product[] = [
  {
    id: 'prod-1',
    code: 'ARC-BON-01',
    name: 'Bon o Bon Leche',
    brand: 'Arcor',
    category: 'Bombones & Chocolates',
    packaging: 'Display x 24 un.',
    unitsPerPack: 24,
    bulkPrice: 14200,
    unitPrice: 591.66,
    stockPacks: 180,
    isOffer: true,
  },
  {
    id: 'prod-2',
    code: 'BAG-SUR-02',
    name: 'Surtido Bagley Clásico',
    brand: 'Bagley',
    category: 'Galletitas',
    packaging: 'Caja x 15 paquetes (390g)',
    unitsPerPack: 15,
    bulkPrice: 22500,
    unitPrice: 1500,
    stockPacks: 95,
    isOffer: false,
  },
  {
    id: 'prod-3',
    code: 'MDZ-ORE-03',
    name: 'Oreo Original Regular',
    brand: 'Mondelez',
    category: 'Galletitas Rellenas',
    packaging: 'Caja x 28 paquetes (118g)',
    unitsPerPack: 28,
    bulkPrice: 33600,
    unitPrice: 1200,
    stockPacks: 140,
    isOffer: true,
  },
  {
    id: 'prod-4',
    code: 'ARC-MOG-04',
    name: 'Mogul Gomitas Frutales',
    brand: 'Arcor',
    category: 'Gomitas & Caramelos',
    packaging: 'Display x 12 bolsas (150g)',
    unitsPerPack: 12,
    bulkPrice: 12800,
    unitPrice: 1066.66,
    stockPacks: 210,
    isOffer: false,
  },
  {
    id: 'prod-5',
    code: 'MDZ-MIL-05',
    name: 'Milka Chocolate con Leche 100g',
    brand: 'Mondelez',
    category: 'Chocolates Finos',
    packaging: 'Display x 16 tabletas',
    unitsPerPack: 16,
    bulkPrice: 28800,
    unitPrice: 1800,
    stockPacks: 65,
    isOffer: true,
  },
  {
    id: 'prod-6',
    code: 'TER-RHOD-06',
    name: 'Rhodesia Oblea Bañada',
    brand: 'Terrabusi',
    category: 'Golosinas Clásicas',
    packaging: 'Display x 36 un.',
    unitsPerPack: 36,
    bulkPrice: 19800,
    unitPrice: 550,
    stockPacks: 120,
    isOffer: false,
  },
  {
    id: 'prod-7',
    code: 'ARC-TOP-07',
    name: 'Topline Menta Fuerte',
    brand: 'Arcor',
    category: 'Chicles',
    packaging: 'Display x 20 paquetes',
    unitsPerPack: 20,
    bulkPrice: 11000,
    unitPrice: 550,
    stockPacks: 340,
    isOffer: false,
  },
];

// Initial Fallback Mock Route
export const MOCK_DAILY_ROUTE: DailyRoute = {
  id: 'route-today',
  routeCode: 'RUT-MZA-CENTRO-01',
  date: new Date().toISOString().split('T')[0],
  assignedAgentName: 'Juan Manuel Torres',
  role: UserRole.PRESALE_AGENT,
  status: RouteStatus.IN_PROGRESS,
  stops: [
    {
      id: 'stop-1',
      orderIndex: 1,
      pointOfSale: MOCK_POINTS_OF_SALE[0],
      status: 'DELIVERED',
      orderId: 'ORD-101',
      orderTotal: 47800,
      estimatedArrival: '09:00',
      deliveredAt: '09:25',
      notes: 'Pedido entregado conforme con pago en efectivo.',
    },
    {
      id: 'stop-2',
      orderIndex: 2,
      pointOfSale: MOCK_POINTS_OF_SALE[1],
      status: 'PENDING',
      orderId: 'ORD-102',
      orderTotal: 33600,
      estimatedArrival: '10:15',
      notes: 'Solicitó factura A.',
    },
    {
      id: 'stop-3',
      orderIndex: 3,
      pointOfSale: MOCK_POINTS_OF_SALE[2],
      status: 'PENDING',
      orderId: 'ORD-103',
      orderTotal: 27000,
      estimatedArrival: '11:30',
      notes: 'Descuento cupón BIENVENIDA aplicado.',
    },
    {
      id: 'stop-4',
      orderIndex: 4,
      pointOfSale: MOCK_POINTS_OF_SALE[3],
      status: 'PENDING',
      orderId: 'ORD-104',
      orderTotal: 71000,
      estimatedArrival: '13:00',
      notes: 'Bultos de chocolates refrigerados.',
    },
    {
      id: 'stop-5',
      orderIndex: 5,
      pointOfSale: MOCK_POINTS_OF_SALE[4],
      status: 'PENDING',
      estimatedArrival: '14:15',
      notes: 'Visita de preventa y reposición.',
    },
  ],
};

// Storage keys for offline cache
const STOPS_STORAGE_KEY = 'dm_order_management_stops';
const POS_STORAGE_KEY = 'dm_order_management_pos';
const ORDERS_STORAGE_KEY = 'dm_order_management_orders';
const PRODUCTS_STORAGE_KEY = 'dm_order_management_products';

export const getStoredStops = (): RouteStop[] => {
  try {
    const raw = localStorage.getItem(STOPS_STORAGE_KEY);
    return raw ? JSON.parse(raw) : MOCK_DAILY_ROUTE.stops;
  } catch {
    return MOCK_DAILY_ROUTE.stops;
  }
};

export const saveStoredStops = (stops: RouteStop[]) => {
  localStorage.setItem(STOPS_STORAGE_KEY, JSON.stringify(stops));
};

export const getStoredPOS = (): PointOfSale[] => {
  try {
    const raw = localStorage.getItem(POS_STORAGE_KEY);
    return raw ? JSON.parse(raw) : MOCK_POINTS_OF_SALE;
  } catch {
    return MOCK_POINTS_OF_SALE;
  }
};

export const saveStoredPOS = (list: PointOfSale[]) => {
  localStorage.setItem(POS_STORAGE_KEY, JSON.stringify(list));
};

export const getStoredOrders = (): Order[] => {
  try {
    const raw = localStorage.getItem(ORDERS_STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
};

export const saveStoredOrder = (order: Order) => {
  const current = getStoredOrders();
  const updated = [order, ...current];
  localStorage.setItem(ORDERS_STORAGE_KEY, JSON.stringify(updated));
};

// --- REAL BACKEND API CALLS ---

interface BackendProductDTO {
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
  imageUrl?: string;
  imageBase64?: string;
}

interface BackendPointOfSaleDTO {
  id: string;
  storeName: string;
  ownerName: string;
  taxIdOrDni: string;
  phone: string;
  address: string;
  neighborhood: string;
  city: string;
  latitude: number;
  longitude: number;
  salesRepId: string;
  salesRepName: string;
  retailerId: string;
  hasActiveOrder: boolean;
}

interface BackendDailyRouteDTO {
  id: string;
  routeCode: string;
  date: string;
  assignedAgentName: string;
  role: UserRole;
  status: RouteStatus;
  stops: Array<{
    id: string;
    orderIndex: number;
    pointOfSale: BackendPointOfSaleDTO;
    status: 'PENDING' | 'PENDING_PAYMENT' | 'VISITED' | 'DELIVERED' | 'SKIPPED';
    orderId?: string;
    orderNumber?: number;
    orderTotal?: number;
    estimatedArrival?: string;
    deliveredAt?: string;
    notes?: string;
    paymentType?: string;
    paid?: boolean;
    readyForDispatch?: boolean;
    invoiceNumber?: number;
    sourceChannel?: string;
    customerName?: string;
    mpInitPoint?: string;
  }>;
}

function mapProductDTO(p: BackendProductDTO): Product {
  const brand = deduceBrand(p.name, p.code);
  const units = p.unitsPerPack > 0 ? p.unitsPerPack : 1;
  return {
    id: p.id,
    code: p.code,
    name: p.name,
    brand,
    category: p.categoryName || p.subCategoryName || 'Golosinas',
    packaging: p.packaging || 'Display',
    unitsPerPack: units,
    bulkPrice: p.currentPrice,
    unitPrice: Math.round((p.currentPrice / units) * 100) / 100,
    stockPacks: p.currentStock,
    isOffer: p.onSale,
    imageUrl: p.imageUrl || p.imageBase64,
  };
}

function mapPointOfSaleDTO(pos: BackendPointOfSaleDTO): PointOfSale {
  return {
    id: pos.id,
    name: pos.storeName,
    ownerName: pos.ownerName,
    taxIdOrDni: pos.taxIdOrDni,
    phone: pos.phone,
    address: pos.address,
    neighborhood: pos.neighborhood,
    city: pos.city,
    latitude: pos.latitude,
    longitude: pos.longitude,
    hasActiveOrder: pos.hasActiveOrder,
  };
}

export async function fetchProducts(): Promise<Product[]> {
  try {
    const res = await fetch(`${API_BASE_URL}/products`);
    if (res.ok) {
      const json = await res.json();
      if (json.success && Array.isArray(json.data) && json.data.length > 0) {
        const mapped = json.data.map(mapProductDTO);
        localStorage.setItem(PRODUCTS_STORAGE_KEY, JSON.stringify(mapped));
        return mapped;
      }
    }
  } catch (err) {
    console.warn('Backend /products not reachable, falling back to local cache/mock:', err);
  }

  // Fallback to cached or mock
  try {
    const cached = localStorage.getItem(PRODUCTS_STORAGE_KEY);
    if (cached) return JSON.parse(cached);
  } catch {}
  return MOCK_PRODUCTS;
}

export async function fetchPointsOfSale(): Promise<PointOfSale[]> {
  try {
    const res = await fetch(`${API_BASE_URL}/points-of-sale`);
    if (res.ok) {
      const json = await res.json();
      if (json.success && Array.isArray(json.data) && json.data.length > 0) {
        const mapped = json.data.map(mapPointOfSaleDTO);
        saveStoredPOS(mapped);
        return mapped;
      }
    }
  } catch (err) {
    console.warn('Backend /points-of-sale not reachable, falling back to local cache/mock:', err);
  }

  return getStoredPOS();
}

export async function createPointOfSaleOnBackend(data: {
  storeName: string;
  ownerName: string;
  taxIdOrDni: string;
  phone: string;
  address: string;
  neighborhood: string;
  city: string;
  latitude: number;
  longitude: number;
}): Promise<PointOfSale> {
  try {
    const res = await fetch(`${API_BASE_URL}/points-of-sale`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (res.ok) {
      const json = await res.json();
      if (json.success && json.data) {
        const savedPos = mapPointOfSaleDTO(json.data);
        const current = getStoredPOS();
        saveStoredPOS([savedPos, ...current]);
        return savedPos;
      }
    }
  } catch (err) {
    console.warn('Backend POST /points-of-sale failed, saving locally:', err);
  }

  // Local fallback
  const fallbackPos: PointOfSale = {
    id: 'pos-' + Date.now(),
    name: data.storeName,
    ownerName: data.ownerName,
    taxIdOrDni: data.taxIdOrDni,
    phone: data.phone,
    address: data.address,
    neighborhood: data.neighborhood,
    city: data.city,
    latitude: data.latitude,
    longitude: data.longitude,
    hasActiveOrder: false,
  };
  const current = getStoredPOS();
  saveStoredPOS([fallbackPos, ...current]);
  return fallbackPos;
}

export async function fetchDailyRoute(role?: UserRole): Promise<DailyRoute> {
  const roleParam = role === UserRole.DELIVERY_AGENT ? 'DELIVERY_AGENT' : 'PRESALE_AGENT';
  try {
    const res = await fetch(`${API_BASE_URL}/routes/daily?role=${roleParam}`);
    if (res.ok) {
      const json = await res.json();
      if (json.success && json.data) {
        const routeData = json.data as BackendDailyRouteDTO;
        const mappedRoute: DailyRoute = {
          id: routeData.id,
          routeCode: routeData.routeCode,
          date: routeData.date,
          assignedAgentName: routeData.assignedAgentName,
          role: routeData.role,
          status: routeData.status,
          stops: routeData.stops.map((s) => ({
            id: s.id,
            orderIndex: s.orderIndex,
            pointOfSale: mapPointOfSaleDTO(s.pointOfSale),
            status: s.status,
            orderId: s.orderId,
            orderNumber: s.orderNumber,
            orderTotal: s.orderTotal,
            estimatedArrival: s.estimatedArrival,
            deliveredAt: s.deliveredAt,
            notes: s.notes,
            paymentType: s.paymentType,
            paid: s.paid,
            readyForDispatch: s.readyForDispatch,
            invoiceNumber: s.invoiceNumber,
            sourceChannel: s.sourceChannel,
            customerName: s.customerName,
            mpInitPoint: s.mpInitPoint,
          })),
        };
        saveStoredStops(mappedRoute.stops);
        return mappedRoute;
      }
    }
  } catch (err) {
    console.warn('Backend /routes/daily not reachable, using stored stops:', err);
  }

  return {
    ...MOCK_DAILY_ROUTE,
    stops: getStoredStops(),
  };
}

export async function submitOrderToBackend(orderData: {
  pointOfSaleId: string;
  couponCode?: string;
  paymentType: PaymentType;
  items: Array<{ productId: string; quantity: number }>;
  pointOfSale: PointOfSale;
  agentName: string;
  cartProducts: { [id: string]: Product };
}): Promise<Order> {
  let res: Response;
  try {
    res = await fetch(`${API_BASE_URL}/orders`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        pointOfSaleId: orderData.pointOfSaleId,
        couponCode: orderData.couponCode,
        paymentType: orderData.paymentType,
        sourceChannel: 'ORDER_MANAGEMENT',
        items: orderData.items,
      }),
    });
  } catch (err) {
    console.warn('Backend POST /orders unreachable, using offline fallback for non-MP:', err);
    if (orderData.paymentType === PaymentType.MERCADO_PAGO || orderData.paymentType === PaymentType.DIGITAL_WALLET) {
      throw new Error('No se pudo conectar con el servidor para generar el QR de Mercado Pago.');
    }
    let subtotal = 0;
    const items = orderData.items.map((i) => {
      const prod = orderData.cartProducts[i.productId];
      const lineTotal = (prod ? prod.bulkPrice : 10000) * i.quantity;
      subtotal += lineTotal;
      return {
        product: prod,
        quantityPacks: i.quantity,
        subtotal: lineTotal,
      };
    });
    const discount = orderData.couponCode ? subtotal * 0.1 : 0;
    const fallbackOrder: Order = {
      id: 'ord-' + Date.now(),
      orderNumber: 'ORD-' + Math.floor(1000 + Math.random() * 9000),
      date: new Date().toISOString(),
      pointOfSale: orderData.pointOfSale,
      items,
      subtotal,
      discountPercentage: discount > 0 ? 10 : 0,
      couponCode: orderData.couponCode,
      total: subtotal - discount,
      status: OrderStatus.LOADED,
      paymentType: orderData.paymentType,
      presaleAgentName: orderData.agentName,
    };
    saveStoredOrder(fallbackOrder);
    return fallbackOrder;
  }

  const json = await res.json().catch(() => ({}));
  if (!res.ok || !json.success || !json.data) {
    throw new Error(json?.message || `No se pudo registrar el pedido (${res.status})`);
  }

  const backendOrder = json.data;
  const localOrder: Order = {
    id: backendOrder.id,
    orderNumber: 'ORD-' + backendOrder.orderNumber,
    numericOrderNumber: backendOrder.orderNumber,
    date: backendOrder.orderDateTime || new Date().toISOString(),
    pointOfSale: orderData.pointOfSale,
    items: orderData.items.map((i) => ({
      product: orderData.cartProducts[i.productId],
      quantityPacks: i.quantity,
      subtotal: (orderData.cartProducts[i.productId]?.bulkPrice || 0) * i.quantity,
    })),
    subtotal: backendOrder.subtotal,
    discountPercentage: backendOrder.discount > 0 && backendOrder.subtotal > 0
      ? (backendOrder.discount / backendOrder.subtotal) * 100
      : 0,
    couponCode: backendOrder.couponCode,
    total: backendOrder.total,
    status: backendOrder.status || OrderStatus.LOADED,
    paymentType: backendOrder.paymentType || orderData.paymentType,
    presaleAgentName: backendOrder.salesRepName || orderData.agentName,
    mpPreferenceId: backendOrder.mpPreferenceId,
    mpInitPoint: backendOrder.mpInitPoint,
    mpPaymentId: backendOrder.mpPaymentId,
    mpPaymentStatus: backendOrder.mpPaymentStatus,
    invoiceId: backendOrder.invoiceId,
    invoiceNumber: backendOrder.invoiceNumber,
    qrImageBase64: backendOrder.qrImageBase64,
    bankAlias: backendOrder.bankAlias,
    bankCbu: backendOrder.bankCbu,
    bankHolder: backendOrder.bankHolder,
  };
  saveStoredOrder(localOrder);
  return localOrder;
}

export async function fetchPaymentConfig(): Promise<PaymentConfig> {
  try {
    const res = await fetch(`${API_BASE_URL}/payments/config`);
    if (res.ok) {
      const json = await res.json();
      if (json.success && json.data) {
        return json.data as PaymentConfig;
      }
    }
  } catch (err) {
    console.warn('Could not load payment config:', err);
  }
  return {
    mercadoPagoEnabled: false,
    sandboxEnabled: true,
    bankAlias: 'DULCE.MAYOR.MZA',
    bankCbu: '0000003100084920193842',
    bankHolder: 'Dulce Mayor S.A.',
    bankName: 'Banco de la Nación Argentina',
  };
}

export async function fetchOrCreateMercadoPagoQr(orderId: string): Promise<MercadoPagoQrResponse> {
  const res = await fetch(`${API_BASE_URL}/payments/orders/${encodeURIComponent(orderId)}/mp/qr`, {
    method: 'POST',
  });
  const json = await res.json().catch(() => ({}));
  if (!res.ok || !json.success || !json.data) {
    throw new Error(json?.message || 'No se pudo generar el QR de Mercado Pago.');
  }
  return json.data as MercadoPagoQrResponse;
}

export async function pollOrderPaymentStatus(orderId: string): Promise<OrderPaymentStatus> {
  const res = await fetch(`${API_BASE_URL}/payments/mp/status/${encodeURIComponent(orderId)}`);
  const json = await res.json().catch(() => ({}));
  if (!res.ok || !json.success || !json.data) {
    throw new Error(json?.message || 'Error consultando el estado del pago.');
  }
  return json.data as OrderPaymentStatus;
}

export async function confirmManualPayment(
  orderId: string,
  paymentType: string = 'MERCADO_PAGO',
  reference?: string,
  note?: string
): Promise<OrderPaymentStatus> {
  const res = await fetch(`${API_BASE_URL}/payments/orders/${encodeURIComponent(orderId)}/confirm-manual`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ paymentType, reference: reference || 'POS-IN-SITU', note: note || 'Acreditación confirmada por Preventista' }),
  });
  const json = await res.json().catch(() => ({}));
  if (!res.ok || !json.success || !json.data) {
    throw new Error(json?.message || 'No se pudo confirmar el cobro en el servidor.');
  }
  return json.data as OrderPaymentStatus;
}

export async function fetchOrdersFromBackend(): Promise<BackendOrderSummary[]> {
  try {
    const res = await fetch(`${API_BASE_URL}/orders`);
    if (res.ok) {
      const json = await res.json();
      if (json.success && Array.isArray(json.data)) {
        return json.data as BackendOrderSummary[];
      }
    }
  } catch (err) {
    console.warn('Could not load backend orders:', err);
  }
  return [];
}

export async function markDeliveryOnBackend(orderId: string): Promise<void> {
  try {
    await fetch(`${API_BASE_URL}/routes/stops/${orderId}/deliver`, {
      method: 'PATCH',
    });
  } catch (err) {
    console.warn('Backend mark deliver failed:', err);
  }
}

export interface ValidatedPromotion {
  id?: string;
  code: string;
  description: string;
  discountPercentage: number;
  minimumQuantity?: number;
  productId?: string | null;
  productName?: string | null;
  active?: boolean;
}

export async function validatePromotionCode(code: string): Promise<ValidatedPromotion> {
  const cleanCode = code.trim().toUpperCase();
  const res = await fetch(`${API_BASE_URL}/promotions/validate?code=${encodeURIComponent(cleanCode)}`);
  const json = await res.json().catch(() => ({}));
  if (!res.ok || !json.success || !json.data) {
    throw new Error(json?.message || `El cupón '${cleanCode}' no es válido o expiró.`);
  }
  return json.data as ValidatedPromotion;
}

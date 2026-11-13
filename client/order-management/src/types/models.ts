import { OrderStatus, RouteStatus, UserRole, AuthProvider, PaymentType } from './enums';

export interface User {
  id: string;
  username: string;
  fullName: string;
  role: UserRole;
  email: string;
  provider: AuthProvider;
  token?: string;
}

export interface PointOfSale {
  id: string;
  name: string;
  ownerName: string;
  taxIdOrDni: string;
  phone: string;
  address: string;
  neighborhood: string;
  city: string;
  latitude: number;
  longitude: number;
  hasActiveOrder?: boolean;
}

export interface RouteStop {
  id: string;
  orderIndex: number;
  pointOfSale: PointOfSale;
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
}

export interface DailyRoute {
  id: string;
  routeCode: string;
  date: string;
  assignedAgentName: string;
  role: UserRole;
  status: RouteStatus;
  stops: RouteStop[];
}

export interface Product {
  id: string;
  code: string;
  name: string;
  brand: string;
  category: string;
  packaging: string; // e.g. "Display x 24 un."
  unitsPerPack: number;
  bulkPrice: number;
  unitPrice: number;
  stockPacks: number;
  isOffer?: boolean;
  imageUrl?: string;
}

export interface OrderItem {
  product: Product;
  quantityPacks: number;
  subtotal: number;
}

export interface Order {
  id: string;
  orderNumber: string;
  numericOrderNumber?: number;
  date: string;
  pointOfSale: PointOfSale;
  items: OrderItem[];
  subtotal: number;
  discountPercentage: number;
  couponCode?: string;
  total: number;
  status: OrderStatus;
  paymentType: PaymentType;
  presaleAgentName: string;
  mpPreferenceId?: string;
  mpInitPoint?: string;
  mpPaymentId?: string;
  mpPaymentStatus?: string;
  invoiceId?: string;
  invoiceNumber?: number;
  qrImageBase64?: string;
  bankAlias?: string;
  bankCbu?: string;
  bankHolder?: string;
}

export interface PaymentConfig {
  mercadoPagoEnabled: boolean;
  sandboxEnabled: boolean;
  publicKey?: string;
  bankAlias: string;
  bankCbu: string;
  bankHolder: string;
  bankName: string;
}

export interface MercadoPagoQrResponse {
  orderId: string;
  orderNumber: number;
  status: OrderStatus;
  total: number;
  preferenceId?: string;
  initPoint?: string;
  qrPayload?: string;
  qrImageBase64?: string;
}

export interface OrderPaymentStatus {
  orderId: string;
  orderNumber: number;
  orderStatus: OrderStatus;
  paymentType: PaymentType;
  paid: boolean;
  readyForDispatch: boolean;
  stockDeducted: boolean;
  subtotal: number;
  discount: number;
  total: number;
  mpPreferenceId?: string;
  mpInitPoint?: string;
  mpPaymentId?: string;
  mpPaymentStatus?: string;
  paidAt?: string;
  invoiceId?: string;
  invoiceNumber?: number;
  storeName?: string;
  customerName?: string;
  bankAlias?: string;
  bankCbu?: string;
  bankHolder?: string;
  message?: string;
}

export interface BackendOrderSummary {
  id: string;
  orderNumber: number;
  orderDateTime: string;
  total: number;
  status: OrderStatus;
  storeName: string;
  retailerName?: string;
  paymentType?: PaymentType;
  sourceChannel?: string;
  mpPreferenceId?: string;
  mpInitPoint?: string;
  mpPaymentId?: string;
  mpPaymentStatus?: string;
  paidAt?: string;
  invoiceNumber?: number;
  readyForDispatch?: boolean;
}

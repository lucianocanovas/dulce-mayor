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
  status: 'PENDING' | 'VISITED' | 'DELIVERED' | 'SKIPPED';
  orderId?: string;
  orderTotal?: number;
  estimatedArrival?: string;
  deliveredAt?: string;
  notes?: string;
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
}

export interface OrderItem {
  product: Product;
  quantityPacks: number;
  subtotal: number;
}

export interface Order {
  id: string;
  orderNumber: string;
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
}

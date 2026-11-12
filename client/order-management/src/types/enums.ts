export const UserRole = {
  PRESALE_AGENT: 'PRESALE_AGENT',
  DELIVERY_AGENT: 'DELIVERY_AGENT',
  ADMINISTRATIVE: 'ADMINISTRATIVE',
  MANAGER: 'MANAGER',
} as const;
export type UserRole = (typeof UserRole)[keyof typeof UserRole];

export const OrderStatus = {
  PENDING_PAYMENT: 'PENDING_PAYMENT',
  LOADED: 'LOADED',
  PAID: 'PAID',
  OUT_OF_STOCK: 'OUT_OF_STOCK',
  INVOICED: 'INVOICED',
  IN_DELIVERY: 'IN_DELIVERY',
  DELIVERED: 'DELIVERED',
  CANCELLED: 'CANCELLED',
} as const;
export type OrderStatus = (typeof OrderStatus)[keyof typeof OrderStatus];

export const RouteStatus = {
  PENDING: 'PENDING',
  IN_PROGRESS: 'IN_PROGRESS',
  COMPLETED: 'COMPLETED',
} as const;
export type RouteStatus = (typeof RouteStatus)[keyof typeof RouteStatus];

export const AuthProvider = {
  LOCAL: 'LOCAL',
  GOOGLE: 'GOOGLE',
  FACEBOOK: 'FACEBOOK',
  GITHUB: 'GITHUB',
} as const;
export type AuthProvider = (typeof AuthProvider)[keyof typeof AuthProvider];

export const PaymentType = {
  CASH: 'CASH',
  BANK_TRANSFER: 'BANK_TRANSFER',
  DIGITAL_WALLET: 'DIGITAL_WALLET',
  MERCADO_PAGO: 'MERCADO_PAGO',
} as const;
export type PaymentType = (typeof PaymentType)[keyof typeof PaymentType];

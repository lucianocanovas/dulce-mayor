export const CompanyType = {
  HEADQUARTERS: 'HEADQUARTERS',
  BRANCH: 'BRANCH',
} as const;
export type CompanyType = (typeof CompanyType)[keyof typeof CompanyType];

export const ContactType = {
  PERSONAL: 'PERSONAL',
  WORK: 'WORK',
  COMPANY: 'COMPANY',
} as const;
export type ContactType = (typeof ContactType)[keyof typeof ContactType];

export const PhoneType = {
  LANDLINE: 'LANDLINE',
  MOBILE: 'MOBILE',
} as const;
export type PhoneType = (typeof PhoneType)[keyof typeof PhoneType];

export const DocumentType = {
  DNI: 'DNI',
  TAX_ID: 'TAX_ID',
  PASSPORT: 'PASSPORT',
} as const;
export type DocumentType = (typeof DocumentType)[keyof typeof DocumentType];

export const UserRole = {
  ADMINISTRATIVE: 'ADMINISTRATIVE',
  MANAGER: 'MANAGER',
  PRESALE_AGENT: 'PRESALE_AGENT',
  RETAILER: 'RETAILER',
  DELIVERY_AGENT: 'DELIVERY_AGENT',
} as const;
export type UserRole = (typeof UserRole)[keyof typeof UserRole];

export const AuthProvider = {
  LOCAL: 'LOCAL',
  GOOGLE: 'GOOGLE',
  FACEBOOK: 'FACEBOOK',
  GITHUB: 'GITHUB',
} as const;
export type AuthProvider = (typeof AuthProvider)[keyof typeof AuthProvider];

export const ImageType = {
  PRODUCT: 'PRODUCT',
  BRANCH: 'BRANCH',
  COMPANY_LOGO: 'COMPANY_LOGO',
} as const;
export type ImageType = (typeof ImageType)[keyof typeof ImageType];

export const OrderStatus = {
  LOADED: 'LOADED',
  OUT_OF_STOCK: 'OUT_OF_STOCK',
  INVOICED: 'INVOICED',
  IN_DELIVERY: 'IN_DELIVERY',
  DELIVERED: 'DELIVERED',
  CANCELLED: 'CANCELLED',
} as const;
export type OrderStatus = (typeof OrderStatus)[keyof typeof OrderStatus];

export const InvoiceStatus = {
  PENDING_PAYMENT: 'PENDING_PAYMENT',
  PAID: 'PAID',
  CANCELLED: 'CANCELLED',
} as const;
export type InvoiceStatus = (typeof InvoiceStatus)[keyof typeof InvoiceStatus];

export const PaymentType = {
  CASH: 'CASH',
  BANK_TRANSFER: 'BANK_TRANSFER',
  DIGITAL_WALLET: 'DIGITAL_WALLET',
} as const;
export type PaymentType = (typeof PaymentType)[keyof typeof PaymentType];

export const RouteStatus = {
  COMPLETED: 'COMPLETED',
  PENDING: 'PENDING',
  INCOMPLETE: 'INCOMPLETE',
} as const;
export type RouteStatus = (typeof RouteStatus)[keyof typeof RouteStatus];

import {
  AuthProvider,
  CompanyType,
  ContactType,
  DocumentType,
  ImageType,
  InvoiceStatus,
  OrderStatus,
  PaymentType,
  PhoneType,
  RouteStatus,
  UserRole,
} from './enums';

export interface BaseEntity {
  id: string;
  isDeleted: boolean;
}

// 1. Location
export interface Country extends BaseEntity {
  name: string;
}

export interface State extends BaseEntity {
  name: string;
  country: Country;
}

export interface Department extends BaseEntity {
  name: string;
  state: State;
}

export interface City extends BaseEntity {
  name: string;
  postalCode?: string;
  department: Department;
}

export interface Address extends BaseEntity {
  street?: string;
  streetNumber?: string;
  neighborhood?: string;
  blockOrFloor?: string;
  apartmentUnit?: string;
  reference?: string;
  latitude?: number;
  longitude?: number;
  city?: City;
}

// 2. Media
export interface Image extends BaseEntity {
  name?: string;
  mimeType?: string;
  imageType: ImageType;
}

// 3. Org & Contacts
export interface Wholesaler extends BaseEntity {
  businessName: string;
  taxId: string;
  companyType: CompanyType;
  address?: Address;
}

export interface CompanyEmailConfig extends BaseEntity {
  email: string;
  port?: string;
  smtpHost?: string;
  tls: boolean;
  company?: Wholesaler;
}

export interface Contact extends BaseEntity {
  contactType: ContactType;
  note?: string;
  person?: Person;
}

export interface EmailContact extends Contact {
  email: string;
}

export interface PhoneContact extends Contact {
  phoneNumber: string;
  phoneType: PhoneType;
}

// 4. Actor
export interface Person extends BaseEntity {
  firstName: string;
  lastName: string;
  birthDate?: string;
  documentType: DocumentType;
  documentNumber: string;
  contacts?: Contact[];
}

export interface Employee extends Person {
  employeeIdNumber?: string;
}

export interface SalesRepresentative extends Employee {}

export interface DeliveryAgent extends Employee {}

export interface Retailer extends Person {}

export interface User extends BaseEntity {
  username: string;
  role: UserRole;
  isTemporaryPassword: boolean;
  authProvider: AuthProvider;
  person?: Person;
}

// 5. Catalog
export interface Category extends BaseEntity {
  name: string;
}

export interface SubCategory extends BaseEntity {
  name: string;
  category: Category;
}

export interface Product extends BaseEntity {
  code: string;
  name: string;
  description?: string;
  packaging?: string;
  unitsPerPack: number;
  isOnSale: boolean;
  subCategory?: SubCategory;
  image?: Image;
}

export interface PriceValidity extends BaseEntity {
  startDate: string;
  endDate?: string | null;
  price: number;
  product: Product;
}

export interface Stock extends BaseEntity {
  currentQuantity: number;
  note?: string;
  product: Product;
}

// 6. Promotion
export interface Promotion extends BaseEntity {
  code: string;
  description?: string;
  discountPercentage: number;
  minimumQuantity?: number;
  startDate: string;
  endDate: string;
  product?: Product;
}

// 7. Order
export interface PointOfSale extends BaseEntity {
  storeName: string;
  retailer: Retailer;
  address: Address;
  salesRep?: SalesRepresentative;
}

export interface Order extends BaseEntity {
  orderNumber: number;
  orderDateTime: string;
  subtotal: number;
  discount: number;
  total: number;
  status: OrderStatus;
  pointOfSale: PointOfSale;
  salesRep?: SalesRepresentative;
  couponCode?: string;
  orderDetails?: OrderDetail[];
}

export interface OrderDetail extends BaseEntity {
  quantity: number;
  unitPrice: number;
  subtotal: number;
  product: Product;
  order: Order;
}

export interface OrderStatusHistory extends BaseEntity {
  status: OrderStatus;
  dateTime: string;
  note?: string;
  user?: User;
  order: Order;
}

// 8. Billing
export interface Invoice extends BaseEntity {
  invoiceNumber: number;
  invoiceDate: string;
  total: number;
  status: InvoiceStatus;
  order: Order;
  invoiceDetails?: InvoiceDetail[];
}

export interface InvoiceDetail extends BaseEntity {
  quantity: number;
  unitPrice: number;
  subtotal: number;
  invoice: Invoice;
}

export interface PaymentMethod extends BaseEntity {
  paymentType: PaymentType;
  note?: string;
}

export interface Payment extends BaseEntity {
  amount: number;
  paymentDate: string;
  reference?: string;
  note?: string;
  invoice: Invoice;
  paymentMethod: PaymentMethod;
}

// 9. Logistics
export interface Route extends BaseEntity {
  date: string;
  status: RouteStatus;
  currentStop?: PointOfSale;
}

export interface SalesRoute extends Route {
  salesRep: SalesRepresentative;
}

export interface DeliveryRoute extends Route {
  deliveryAgent: DeliveryAgent;
}

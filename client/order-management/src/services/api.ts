import type { DailyRoute, Order, PointOfSale, Product } from '../types/models';
import { RouteStatus, UserRole } from '../types/enums';

// Initial Mock Points of Sale in Greater Mendoza
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
    hasActiveOrder: false,
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

// Initial Mock Products (Wholesale Confectionery: Arcor, Bagley, Mondelez, Terrabusi)
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

// Initial Mock Route for Today
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
      status: 'VISITED',
      orderId: 'ORD-101',
      orderTotal: 47800,
      estimatedArrival: '09:00',
      deliveredAt: '09:25',
      notes: 'Pedido tomado con pago en efectivo.',
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
      estimatedArrival: '11:30',
      notes: 'Posible incorporación de nueva línea Arcor.',
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
  ],
};

// Storage keys
const STOPS_STORAGE_KEY = 'dm_order_management_stops';
const POS_STORAGE_KEY = 'dm_order_management_pos';
const ORDERS_STORAGE_KEY = 'dm_order_management_orders';

export const getStoredStops = (): typeof MOCK_DAILY_ROUTE.stops => {
  try {
    const raw = localStorage.getItem(STOPS_STORAGE_KEY);
    return raw ? JSON.parse(raw) : MOCK_DAILY_ROUTE.stops;
  } catch {
    return MOCK_DAILY_ROUTE.stops;
  }
};

export const saveStoredStops = (stops: typeof MOCK_DAILY_ROUTE.stops) => {
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

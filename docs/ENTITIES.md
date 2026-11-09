# System Entities - Dulce Mayor

Reference document for the data model, domain entities, and persistence architecture of the project, standardized in English based on the official UML class diagram.

All entities inherit common auditing and soft-delete fields from a base entity (`id: String`, `isDeleted: boolean`).

---

## 1. Entities Organized by Module

- **location** (Geographic Location)
  - `Country`
  - `State` / `Province`
  - `Department`
  - `City` / `Locality`
  - `Address`

- **company** (Wholesale Organization & Configuration)
  - `Wholesaler`
  - `CompanyEmailConfig`
  - `Contact` *(Abstract Base Class)*
    - `EmailContact`
    - `PhoneContact`
  - `Image`

- **actor** (Persons, Users & Roles)
  - `Person` *(Base Class)*
    - `Employee` *(Base Class)*
      - `SalesRepresentative` *(Preventor)*
      - `DeliveryAgent` *(Distribuidor / Repartidor)*
    - `Retailer` *(Minorista / Customer)*
  - `User`

- **catalog** (Catalog, Products & Inventory)
  - `Category`
  - `SubCategory`
  - `Product`
  - `PriceValidity` *(Price History)*
  - `Stock`
  - `Promotion`

- **order** (Sales & Order Processing)
  - `PointOfSale`
  - `Order`
  - `OrderDetail`
  - `OrderStatusHistory`

- **billing** (Invoicing & Payments)
  - `Invoice`
  - `InvoiceDetail`
  - `Payment`
  - `PaymentMethod`

- **logistics** (Route Planning & Distribution)
  - `Route` *(Base Class)*
    - `SalesRoute` *(RutaPreventor)*
    - `DeliveryRoute` *(RutaDistribuidor)*

---

## 2. Entity Details, Attributes & Relations

### 2.1. Location

- **`Country`** (`Pais`): `id`, `name`, `isDeleted`.
- **`State`** (`Provincia`): `id`, `name`, `country`, `isDeleted`.
- **`Department`** (`Departamento`): `id`, `name`, `state`, `isDeleted`.
- **`City`** (`Localidad`): `id`, `name`, `postalCode`, `department`, `isDeleted`.
- **`Address`** (`Direccion`): `id`, `street`, `streetNumber`, `neighborhood`, `blockOrFloor`, `apartmentUnit`, `reference`, `latitude`, `longitude`, `city`, `isDeleted`.

### 2.2. Company

- **`Wholesaler`** (`Mayorista`): `id`, `businessName`, `taxId` (CUIT), `companyType` (`CompanyType`), `address`, `isDeleted`.
- **`CompanyEmailConfig`** (`ConfiguracionCorreoEmpresa`): `id`, `email`, `password`, `port`, `smtpHost`, `tls`, `company`, `isDeleted`.
- **`Contact`** (`Contacto`) *(Abstract)*: `id`, `contactType` (`ContactType`), `note`, `isDeleted`.
  - **`EmailContact`** (`ContactoCorreoElectronico`): `email`.
  - **`PhoneContact`** (`ContactoTelefonico`): `phoneNumber`, `phoneType` (`PhoneType`).
- **`Image`** (`Imagen`): `id`, `name`, `mimeType`, `content` (`byte[]`), `imageType` (`ImageType`), `isDeleted`.

### 2.3. Actor (Users, Staff & Customers)

- **`Person`** (`Persona`): `id`, `firstName`, `lastName`, `birthDate`, `documentType` (`DocumentType`), `documentNumber`, `isDeleted`.
  - **`Employee`** (`Empleado`): `employeeIdNumber` (Legajo).
    - **`SalesRepresentative`** (`Preventor`): Field sales agent visiting stores and capturing mobile orders.
    - **`DeliveryAgent`** (`Distribuidor`): Logistics courier completing deliveries and updating route stops.
  - **`Retailer`** (`Minorista`): Commercial client who owns stores/kiosks, tracks orders, and pays invoices.
- **`User`** (`Usuario`): `id`, `username`, `password` (encrypted), `role` (`UserRole`), `isTemporaryPassword` (`boolean`), `authProvider` (`AuthProvider`), `person`, `isDeleted`.

### 2.4. Catalog

- **`Category`** (`Categoria`): `id`, `name`, `isDeleted`.
- **`SubCategory`** (`SubCategoria`): `id`, `name`, `category`, `isDeleted`.
- **`Product`** (`Producto`): `id`, `code`, `name`, `description`, `packaging` (e.g. *Pack x12*), `unitsPerPack`, `isOnSale`, `subCategory`, `image`, `isDeleted`.
- **`PriceValidity`** (`VigenciaPrecio`): `id`, `startDate`, `endDate` *(if `null`, this is the active current price)*, `price`, `product`, `isDeleted`.
- **`Stock`** (`Stock`): `id`, `currentQuantity`, `note`, `product`, `isDeleted`.
- **`Promotion`** (`Promocion`): `id`, `code`, `description`, `discountPercentage`, `minimumQuantity` *(volume discount)*, `startDate`, `endDate`, `product`, `isDeleted`.

### 2.5. Order

- **`PointOfSale`** (`PuntoDeVenta`): `id`, `storeName`, `retailer` (`Retailer`), `address` (`Address` with GPS coordinates), `salesRep` (`SalesRepresentative`), `isDeleted`.
- **`Order`** (`Pedido`): `id`, `orderNumber`, `orderDateTime`, `subtotal`, `discount`, `total`, `status` (`OrderStatus`), `pointOfSale`, `salesRep`, `couponCode`, `isDeleted`.
- **`OrderDetail`** (`DetallePedido`): `id`, `quantity`, `unitPrice`, `subtotal`, `product`, `order`, `isDeleted`.
- **`OrderStatusHistory`** (`HistorialEstadoPedido`): `id`, `status` (`OrderStatus`), `dateTime`, `note`, `user`, `order`.

### 2.6. Billing

- **`Invoice`** (`Factura`): `id`, `invoiceNumber`, `invoiceDate`, `total`, `status` (`InvoiceStatus`), `order`, `isDeleted`.
- **`InvoiceDetail`** (`DetalleFactura`): `id`, `quantity`, `unitPrice`, `subtotal`, `invoice`, `isDeleted`.
- **`PaymentMethod`** (`FormaDePago`): `id`, `paymentType` (`PaymentType`), `note` (e.g., *Bank Alias*), `isDeleted`.
- **`Payment`** (`Pago`): `id`, `amount`, `paymentDate`, `reference` (Bank alias receipt or QR payload), `note`, `invoice`, `paymentMethod`, `isDeleted`.

### 2.7. Logistics

- **`Route`** (`Ruta`): `id`, `date`, `status` (`RouteStatus`), `currentStop` (`PointOfSale`), `isDeleted`.
  - **`SalesRoute`** (`RutaPreventor`): Daily optimized sales route for field order taking.
  - **`DeliveryRoute`** (`RutaDistribuidor`): Daily delivery route guiding dispatch couriers.

---

## 3. Key Business Rules

1. **Stock Deduction:** Inventory is not deducted upon order creation; it is automatically subtracted when the `Invoice` is generated.
2. **Catalog Visibility:** Products with `currentQuantity <= 0` are automatically excluded from the public storefront catalog.
3. **Current Active Price:** The effective price for any `Product` is retrieved from the `PriceValidity` record where `endDate IS NULL`.
4. **First Login Password Policy:** Newly created users are assigned `isTemporaryPassword = true` and default password `dulcemayor`. The API forces a password change before granting access to operational endpoints.
5. **Order Audit Traceability:** Every change to `OrderStatus` creates an immutable `OrderStatusHistory` entry recording the timestamp and operator.

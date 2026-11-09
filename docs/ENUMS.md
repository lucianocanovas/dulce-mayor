# Domain Enumerations

| Enumeration | Allowed Values | Description |
| :--- | :--- | :--- |
| **`CompanyType`** | `HEADQUARTERS`, `BRANCH` | Corporate headquarters vs. local branches |
| **`ContactType`** | `PERSONAL`, `WORK`, `COMPANY` | Scope of the contact information |
| **`PhoneType`** | `LANDLINE`, `MOBILE` | Landline phone or cellular |
| **`DocumentType`** | `DNI`, `TAX_ID`, `PASSPORT` | Government identification type |
| **`UserRole`** | `ADMINISTRATIVE`, `MANAGER`, `PRESALE_AGENT`, `RETAILER`, `DELIVERY_AGENT` | System authorization roles |
| **`AuthProvider`** | `LOCAL`, `GOOGLE`, `FACEBOOK` | Authentication source provider |
| **`ImageType`** | `PRODUCT`, `BRANCH`, `COMPANY_LOGO` | Storage category of image files |
| **`OrderStatus`** | `LOADED`, `OUT_OF_STOCK`, `INVOICED`, `IN_DELIVERY`, `DELIVERED`, `CANCELLED` | Order lifecycle stage |
| **`InvoiceStatus`** | `PENDING_PAYMENT`, `PAID`, `CANCELLED` | Billing settlement status |
| **`PaymentType`** | `CASH`, `BANK_TRANSFER`, `DIGITAL_WALLET` | Accepted payment methods |
| **`RouteStatus`** | `COMPLETED`, `PENDING`, `INCOMPLETE` | Status of daily logistics route |

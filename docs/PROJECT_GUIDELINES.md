# Project Guidelines & Architecture - Dulce Mayor

> **Audience:** Human Developers and AI Coding Agents.  
> **Purpose:** Canonical blueprint of architectural constraints, coding standards, business rules, design patterns, and tech stack for the **Dulce Mayor** wholesale platform.

---

## 1. Project Overview & Business Domain

**Dulce Mayor** is a confectionery and candy wholesale distributor located in Mendoza, Argentina. The system is an end-to-end management, sales, and logistics ecosystem consisting of:

1. **Public Web Storefront & Customer Portal (`client/landing` - Java Spring Boot + Thymeleaf):**
   * Standalone Java client application built with **Spring Boot & Java Thymeleaf** (located in `client/landing`, running on port `8081`).
   * Operates as a dedicated view consumer: communicates directly with the backend REST API (`server`) via `RestClient`.
   * Public product catalog (photos, pack descriptions, real-time live stock).
   * **AI Chatbot powered by Spring AI** (orchestrating Google Gemini API on the backend via a lightweight vanilla JS client) for interactive automated quotation building.
   * Real-time USD exchange rate integration (e.g., via DolarApi).
   * Retailer self-service area: order history, tracking status, PDF invoice downloads, and digital wallet payment QR display.
   * Authentication: Standard email/password + Social Login (OAuth2).

2. **Field Operations Mobile Web / PWA (`client/order-management` - React 19 + Vite):**
   * Responsive Single Page Application (SPA) built with **React 19 + TypeScript + Vite**, optimized for mobile devices in the field.
   * **OpenStreetMap (OSM) + Leaflet** integration (`react-leaflet`) for interactive routing, store markers, and real-time itinerary tracking without full-page reloads.
   * Presale agents (Preventores): store onboarding, catalog browsing with live stock, mobile order entry with coupon code discounts, next optimal stop recommendations.
   * Delivery couriers (Distribuidores): daily route stops, delivery marking ("DELIVERED"), and automated email triggers to the next stop.
   * Authentication: Dual support for standard credentials and **OAuth2 (Google + GitHub/Facebook)**.

3. **Central Management Backoffice (`client/central-management` - React 19 + Vite):**
   * High-interactivity Single Page Application (SPA) built with **React 19 + TypeScript + Vite**.
   * Modular dashboard for Warehouse Personnel, Administrative Staff, and Executive Management (Jefe Administrativo).
   * Inventory control, real-time stock replenishment via dynamic modals, automated billing upon stock verification.
   * Promotion code management (volume & highlighted product discounts).
   * User management with automated assignment of temporary password (`dulcemayor`).
   * Management reporting: export sales lists to Excel (by department and presale agent) and PDF invoices/summaries.
   * Analytics & Charts: interactive bar charts (orders per presale agent per day) and pie charts (product stock distribution).
   * Authentication: Dual support for standard credentials and **OAuth2 (Google + GitHub/Facebook)**.

4. **Central REST API Server (`server`):**
   * Pure backend REST API engine (no frontend templates or view renderers).
   * Multi-tier Spring Boot engine enforcing domain invariants, transaction boundaries, security policies, and third-party integrations.

---

## 2. Definitive Tech Stack & Specifications

| Layer / Concern | Technology | Specification / Library |
| :--- | :--- | :--- |
| **Backend Runtime** | Java JDK | **Java 26** (`26.0.2.1+`) |
| **Backend Framework** | Spring Boot | **4.1.x** (Spring Framework 7.x) |
| **Persistence / ORM** | Spring Data JPA / Hibernate | Hibernate 7.4.x |
| **Local / Test Database** | H2 Database | In-memory for fast unit/integration tests |
| **Production Database** | **PostgreSQL 17+** | Containerized via Docker (`postgres:latest`) |
| **Security & Auth** | Spring Security + JWT + OAuth2 Client | `spring-boot-starter-security`, `spring-boot-starter-oauth2-client`, `jjwt` |
| **OAuth2 Providers** | **Google, GitHub & Facebook** | Configured in `central-management`, `order-management`, and `server` |
| **Frontend SPAs (Rich Clients)** | **React 19 + TypeScript + Vite** | Modular component architecture for `central-management` (Backoffice) & `order-management` (Mobile Field Operations) |
| **Frontend SSR (Public Storefront)** | **Java Thymeleaf (Spring Boot)** | Standalone Java Web client application for `landing` (Customer Portal & Public Catalog) located in `client/landing` (Port 8081) |
| **Maps & Routing** | **OpenStreetMap + Leaflet** | `leaflet`, `react-leaflet` (No paid API keys or billing required) |
| **AI Framework & Model** | **Spring AI + Google Gemini** | `spring-ai-starter-model-gemini` (`ChatClient`) orchestrating Gemini on the backend |
| **Document Generation** | Apache POI & iText / OpenPDF | Excel export (`.xlsx`) and PDF reports (`.pdf`) |
| **DevOps & Containers** | Docker, Docker Compose, Jenkins | Multi-container composition, automated CI/CD pipeline |
| **Code Standard Language** | **English** | Strictly English for all code, entities, DTOs, tests, and comments. |

---

## 3. Repository Directory Structure

```text
dulce-mayor/
│
├── docs/                                # Canonical project documentation
│   ├── ENTITIES.md                      # Data model & entity specifications
│   ├── ENUMS.md                         # Complete enum glossary
│   ├── PROJECT_GUIDELINES.md            # This document
│   └── TRABAJO PRÁCTICO INTEGRADOR N2.pdf # Academic specification
│
├── server/                              # Backend API (Spring Boot - Java 26)
│   ├── src/main/java/ing/software/dulcemayor/
│   │   ├── config/                      # SecurityConfig, CorsConfig, OAuth2Config, SpringAiConfig
│   │   ├── controller/                  # REST Controllers (@RestController)
│   │   │   ├── base/                    # BaseController (Template Method pattern)
│   │   │   └── ai/                      # AiQuoteController (POST /api/ai/quote endpoint)
│   │   ├── service/                     # Business Services (@Service)
│   │   │   ├── base/                    # BaseService (Template Method pattern)
│   │   │   ├── actor/                   # AuthService, UserService
│   │   │   ├── catalog/                 # ProductService, StockService
│   │   │   ├── order/                   # OrderService
│   │   │   ├── dashboard/               # DashboardService
│   │   │   └── ai/                      # AiQuoteService (uses Spring AI ChatClient grounded with Product Catalog)
│   │   ├── repository/                  # Spring Data JPA Repositories
│   │   │   ├── base/                    # BaseRepository (with soft-delete findByIsDeletedFalse)
│   │   │   ├── actor/                   # UserRepository, PersonRepository, EmployeeRepository, etc.
│   │   │   ├── billing/                 # InvoiceRepository, PaymentRepository, etc.
│   │   │   ├── catalog/                 # ProductRepository, StockRepository, PriceValidityRepository, etc.
│   │   │   ├── location/                # CountryRepository, CityRepository, AddressRepository, etc.
│   │   │   ├── logistics/               # RouteRepository, SalesRouteRepository, DeliveryRouteRepository
│   │   │   ├── media/                   # ImageRepository
│   │   │   ├── order/                   # OrderRepository, OrderDetailRepository, PointOfSaleRepository, etc.
│   │   │   └── org/                     # WholesalerRepository, CompanyEmailConfigRepository, ContactRepository
│   │   ├── entity/                      # JPA Entities (audited with id, isDeleted)
│   │   │   ├── actor/                   # Person, Employee, SalesRepresentative, DeliveryAgent, Retailer, User
│   │   │   ├── billing/                 # Invoice, InvoiceDetail, Payment, PaymentMethod
│   │   │   ├── catalog/                 # Product, Category, SubCategory, Stock, PriceValidity, Promotion
│   │   │   ├── location/                # Country, State, Department, City, Address
│   │   │   ├── logistics/               # Route, SalesRoute, DeliveryRoute
│   │   │   ├── media/                   # Image
│   │   │   ├── order/                   # PointOfSale, Order, OrderDetail, OrderStatusHistory
│   │   │   └── org/                     # Wholesaler, CompanyEmailConfig, Contact
│   │   ├── dto/                         # Request and Response DTOs
│   │   └── enums/                       # Domain enumerations (see ENUMS.md)
│   ├── src/main/resources/
│   │   ├── application.properties       # Base settings
│   │   ├── application-dev.properties   # H2 configuration
│   │   └── application-prod.properties  # PostgreSQL configuration
│   ├── pom.xml                          # Maven build file (Java 26 release)
│   └── Dockerfile                       # Multi-stage Java build
│
├── client/                              # Frontend Client Applications
│   ├── landing/                         # Public Storefront & Customer Portal (Java Spring Boot + Thymeleaf - Port 8081)
│   │   ├── src/main/java/.../landing/   # HomeController, CatalogController, BackendApiClient, DTOs
│   │   ├── src/main/resources/
│   │   │   ├── templates/               # Thymeleaf HTML views (index, catalog, contact)
│   │   │   ├── static/                  # CSS & static assets
│   │   │   └── application.properties   # Configured for Port 8081 & Backend URL (8080)
│   │   └── pom.xml                      # Maven build file
│   │
│   ├── central-management/              # Admin, Warehouse & Executive Dashboard (React 19 + TypeScript + Vite)
│   └── order-management/                # Field Sales & Delivery App (React 19 + TypeScript + Leaflet + Vite)
│
├── docker-compose.yml                   # Orchestrates PostgreSQL, Server, and Clients
└── README.md
```

---

## 4. Architectural Rules & Design Patterns

### 4.1. Strict 3-Tier Layered Architecture

1. **Controller Layer:** Only handles HTTP requests, validates incoming DTOs, and delegates immediately to services. No repository or database logic allowed here.
2. **Service Layer:** Contains 100% of the business logic, transaction handling (`@Transactional`), security checks, and entity-to-DTO conversion.
3. **Repository Layer:** Extends Spring Data `JpaRepository` or `JpaSpecificationExecutor`.

### 4.2. Mandatory Design Patterns

* **Template Method Pattern:**
  * Base controller: defines standard REST flow (`getAll`, `getById`, `create`, `update`, `delete`).
  * Base service: defines algorithm templates (`validate()`, `beforeSave()`, `executeSave()`, `afterSave()`).
* **DTO Pattern:**
  * `@Entity` classes must **never** leave the service layer.
  * Controllers receive `*RequestDTO` and return `*ResponseDTO`.
* **Strategy Pattern:**
  * Implement for payment processing (`CashPaymentStrategy`, `TransferPaymentStrategy`, `DigitalWalletPaymentStrategy`).
* **Factory / Builder Pattern:**
  * Used for complex invoice generation and order lifecycle events.
* **Observer Pattern:**
  * Event-driven triggers for sending transactional emails (order confirmation, next delivery point alert).

### 4.3. Soft Delete (`isDeleted`)

* Physical SQL deletions (`DELETE FROM`) are forbidden.
* All entities extend a common base with `id: String` (UUID) and `isDeleted: boolean = false`.
* Queries retrieve only active records (`findByIsDeletedFalse`).

---

## 5. Security & Authentication Architecture

1. **Dual Authentication Engine:**
   * **Local Authentication:** Username/email + encrypted password (BCrypt), issuing signed JWT tokens.
   * **OAuth2 Social Login:** Supported providers include **Google**, **GitHub**, and **Facebook**. Configured on both client portals (`central-management` and `order-management`) and validated by the backend.
2. **Mandatory First Login Password Change:**
   * When an administrative user registers an employee, the system sets `isTemporaryPassword = true` and the initial password `dulcemayor`.
   * On login, if `isTemporaryPassword == true`, the API returns a flag/token restricting access strictly to the password reset endpoint (`/api/auth/change-password`).
3. **Role-Based Access Control (RBAC):**
   * Endpoints are protected with `@PreAuthorize("hasRole('...')")`:
     * `ADMINISTRATIVE`: Manage stock, product catalog, review orders, execute billing.
     * `MANAGER`: Administrative privileges + user creation, executive sales metrics, Excel/PDF downloads.
     * `PRESALE_AGENT`: Mobile access, store registration, field order entry with promo discounts.
     * `DELIVERY_AGENT`: Delivery route navigation, delivery status updates.
     * `RETAILER`: Public portal self-service, order status inspection, invoice download, QR code payments.

---

## 6. Maps & Geolocation Guidelines (`order-management`)

1. **Library & Provider:**
   * Use **OpenStreetMap (OSM) tiles** rendered with **Leaflet** (`react-leaflet`).
2. **Coordinate Standard:**
   * Geographic coordinates are stored as `double latitude` and `double longitude` in `Address`.
3. **Route & Pinpoint Display:**
   * Display store pins with clear visual distinction (e.g., visited vs. pending).
   * Presale Route: Display current route with marker sequences and recommended next stop.
   * Delivery Route: Show current delivery point. Once marked as `DELIVERED`, automatically highlight the next stop and trigger the notification service.

---

## 7. AI Chatbot Architecture & Guidelines (`Spring AI`)

1. **Server-Side Orchestration (No Direct Client-to-Gemini Calls):**
   * The frontend (`landing-page`) communicates exclusively through the backend endpoint: `POST /api/ai/quote`.
   * **Security:** The Google Gemini API key (`spring.ai.gemini.api-key`) resides strictly on the server in environment variables; it is never exposed in client JavaScript bundles.

2. **Technology & Framework:**
   * **Spring AI** (`spring-ai-starter-model-gemini` / `spring-ai-google-genai`).
   * Uses Spring AI's fluent `ChatClient` builder (`ChatClient.builder(chatModel).build()`) injected into `AiQuoteService`.

3. **Catalog Grounding (Retrieval-Augmented Prompting):**
   * Before sending user inquiries to the model, `AiQuoteService` queries `ProductRepository` and `StockRepository` for active products (`Stock.currentQuantity > 0`) and their active price (`PriceValidity.endDate IS NULL`).
   * The active catalog data is injected as context into the System Prompt or via Spring AI Advisors/Tools. This guarantees the AI never invents candy products, prices, or discounts that do not exist in the warehouse.

4. **Functional Scope & Output:**
   * Understands customer wholesale requirements (e.g., *"Quiero armar un surtido de alfajores y chocolates para un kiosco escolar por $50,000"*).
   * Calculates subtotal estimates based on wholesale pack sizes (`unitsPerPack`).
   * Returns a structured response DTO (`QuoteResponseDTO` containing conversational advice, recommended items with unit/pack prices, and estimated grand total).
   * Appends an explicit disclaimer: *"This estimate is informative and subject to final order processing and invoice issuance."*

---

## 8. Instructions for AI Coding Agents

When editing or generating code in this repository:

* **Language:** Write code, comments, DTO names, entity classes, and git commits in **English**.
* **No Direct DB Mutations in Controllers:** Always enforce Controller $\rightarrow$ Service $\rightarrow$ Repository.
* **DTO Safety:** Never expose password hashes, internal IDs of soft-deleted records, or sensitive customer payment secrets in response DTOs.
* **Test Integrity:** Every new service method requires corresponding unit tests with JUnit 5 and Mockito.
* **Java 26 Compatibility:** Keep `pom.xml` target at release 26.

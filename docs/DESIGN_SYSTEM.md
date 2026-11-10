# Manual de Diseño & Sistema Visual — Dulce Mayor
>
> **Documento Canónico de Diseño Frontend**  
> **Alineación Visual para las 3 Aplicaciones del Ecosistema:**  
>
> 1. `client/landing` (Storefront Público & Portal Minorista — Spring Boot / Thymeleaf)  
> 2. `client/order-management` (Operaciones de Campo PWA / Móvil — React 19 + TypeScript + Leaflet)  
> 3. `client/central-management` (Backoffice Administrativo & Almacén — React 19 + TypeScript)  
>  
> **Ubicación de Assets Vectoriales:**  
>
> - Isotipo Vectorial Principal: [`docs/assets/logo.svg`](file:///c:/Users/Lucho/Desktop/dulce-mayor/docs/assets/logo.svg)  
> - Isotipo + Logotipo Horizontal: [`docs/assets/logo-horizontal.svg`](file:///c:/Users/Lucho/Desktop/dulce-mayor/docs/assets/logo-horizontal.svg)

---

## 1. Filosofía de Diseño: Dulzura Clásica, Eficiencia Real

Dulce Mayor es una distribuidora mayorista de golosinas ubicada en Mendoza, Argentina. Nuestra plataforma gestiona desde la venta pública y el pedido minorista hasta la preventa barrial en tiempo real y la logística de almacén.

### Referentes de Marca

El estilo visual toma inspiración directa de las grandes compañías alimenticias y golosineras tradicionales del mercado argentino y regional: **Arcor, Bagley, Mondelez y Terrabusi**.

- **Sobrio y Confiable:** La operación mayorista se fundamenta en números claros, stock real, precios por bulto/pack y estados de entrega precisos. No hay lugar para ambigüedades.
- **Cálido y Cercano:** La golosina es gratificación, color y optimismo. Usamos tonos miel, caramelo y vainilla que despiertan apetito visual sin saturar la pantalla.
- **Divertido con Moderación:** Detalles redondeados, botones amigables, micro-acentos de espiral y corona, pero siempre conservando la legibilidad de un software de trabajo diario.

---

## 2. Manifiesto Anti "AI Slop": Menos Humo, Más Claridad

> [!CAUTION]
> **Prohibido el "AI Slop" tanto en Textos como en Componentes Visuales.**  
> Los diseños generados por modelos de inteligencia artificial suelen caer en clichés vacíos: gradientes violetas/neón, sombras difuminadas ilegibles, textos hinchados que no dicen nada y tarjetas de vidrio borroso (*glassmorphism*) que hacen inusable el sistema.

### A. Reglas de Redacción (Copywriting)

| Elemento | ❌ Lo que es "AI Slop" (PROHIBIDO) | ✅ Lo que es Dulce Mayor (OBLIGATORIO) |
| :--- | :--- | :--- |
| **Hero de Landing** | *"En un ecosistema donde la sinergia convergen con la pasión por el azúcar, empoderamos su rentabilidad comercial con soluciones disruptivas de punta a punta."* | **"Golosinas directo de fábrica al mejor precio mayorista de Mendoza."** |
| **Botón de Compra** | *"Iniciar trayectoria de adquisición automatizada"* | **"Agregar al pedido"** o **"Comprar bulto"** |
| **Estado de Pedido** | *"Su itinerario logístico se encuentra experimentando una fase de preparación avanzada hacia el destino seleccionado."* | **"En preparación"** (Tiempo est. 24 hs) |
| **Reponer Stock** | *"Incentivar flujo de reabastecimiento proactivo de inventario en almacenes nodales."* | **"Ingresar mercadería"** (+ campo numérico de unidades) |
| **Badges de IA** | *"✨ Impulsado por IA cuántica para maximizar tu visión holística"* | **"Cotizador automático"** |

**Principios de texto:**

1. **Puntual:** Si una frase puede decirse en 4 palabras, no uses 12.
2. **Accionable:** Cada botón y etiqueta debe decir exactamente lo que hace (`Descargar PDF`, `Confirmar Entrega`, `Filtrar por Marca`).
3. **Humano y Argentino/Regional:** Utilizar términos naturales del rubro golosinero mayorista (`Bulto`, `Display`, `Unidad`, `Preventista`, `Repartidor`, `Lista de Precios`).

### B. Reglas de Componentes Visuales

- **❌ Nada de gradientes púrpura/magenta tipo Cyberpunk:** Dulce Mayor no es una crypto-wallet ni un SaaS de Silicon Valley. Los colores son golosineros: naranja, miel, chocolate y vainilla.
- **❌ Nada de Glassmorphism innecesario:** Fondos translúcidos con `backdrop-blur-xl` reducen el contraste, consumen batería en el celular del preventista bajo el sol y tornan las tablas de stock imposibles de leer.
- **❌ Nada de sombras flotantes exageradas:** Evitar `box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25)`. Usar sombras limpias, sutiles y cálidas (`box-shadow: 0 2px 8px rgba(43, 24, 16, 0.08)`).
- **❌ Nada de héroes gigantes vacíos:** En las 3 aplicaciones, el usuario busca ver productos, precios, rutas o stock de forma inmediata. No colocar pantallas de bienvenida que ocupen el 90% del viewport sin datos útiles.

---

## 3. Identidad de Marca: Isologo Vectorial

El imagotipo oficial combina dos conceptos:

1. **El Caramelo Espiral (Swirl):** Pinwheel de 12 aspas curvas en espiral áurea (6 Naranja Caramelo alternadas con 6 Amarillo Miel), rematado con aro terracota. Simboliza dinamismo, sabor y alegría.
2. **La Corona Real (Crown):** Corona heráldica de 3 puntas con orbes esféricos y pliegue facetado (luz dorada a la izquierda, sombra ámbar a la derecha). Simboliza liderazgo de volumen, calidad mayorista ("Mayor").

### Ubicación de los Archivos en el Repositorio

| Asset | Ruta de Archivo | Aplicación / Uso |
| :--- | :--- | :--- |
| **Logo Canónico (SVG)** | [`docs/assets/logo.svg`](file:///c:/Users/Lucho/Desktop/dulce-mayor/docs/assets/logo.svg) | Manual de marca, referencia de diseño |
| **Logo Horizontal (SVG)** | [`docs/assets/logo-horizontal.svg`](file:///c:/Users/Lucho/Desktop/dulce-mayor/docs/assets/logo-horizontal.svg) | Cabeceras, navbars, pantallas de login |
| **Landing Storefront** | [`client/landing/src/main/resources/static/images/logo.svg`](file:///c:/Users/Lucho/Desktop/dulce-mayor/client/landing/src/main/resources/static/images/logo.svg) | Header de Thymeleaf y favicon |
| **Order Management (Móvil)** | [`client/order-management/public/logo.svg`](file:///c:/Users/Lucho/Desktop/dulce-mayor/client/order-management/public/logo.svg) | App PWA, Navbar móvil, splash |
| **Central Management** | [`client/central-management/public/logo.svg`](file:///c:/Users/Lucho/Desktop/dulce-mayor/client/central-management/public/logo.svg) | Sidebar de backoffice y login |

```xml
<!-- Inclusión directa en HTML/Thymeleaf -->
<img src="/images/logo-horizontal.svg" alt="Dulce Mayor" height="42" />

<!-- Inclusión en React (Vite) -->
<img src="/logo-horizontal.svg" alt="Dulce Mayor" className="h-10 w-auto" />
```

---

## 4. Paleta de Colores Oficial

La paleta se aleja intencionalmente de la frialdad corporativa estándar (grises metálicos o azules bancarios) para abrazar la calidez del rubro golosinero y la confitería tradicional.

```text
       #F58721            #FAC933            #2B1810            #FAF6F0
 ┌─────────────────┬─────────────────┬─────────────────┬─────────────────┐
 │                 │                 │                 │                 │
 │ Caramel Orange  │   Golden Honey  │  Dark Chocolate │  Vanilla Cream  │
 │  (Brand Accent) │  (Brand Gold)   │   (Body Text)   │    (Canvas)     │
 │                 │                 │                 │                 │
 └─────────────────┴─────────────────┴─────────────────┴─────────────────┘
```

### Tabla de Códigos de Color

| Rol de Diseño | Nombre | Hex | RGB | Uso Recomendado |
| :--- | :--- | :--- | :--- | :--- |
| **Brand Primary** | **Caramel Orange** | `#F58721` | `rgb(245, 135, 33)` | Botones principales (CTA), links activos, isotipo |
| **Brand Secondary** | **Golden Honey** | `#FAC933` | `rgb(250, 201, 51)` | Badges de promociones, estrellas, detalles de corona |
| **Brand Dark Rust** | **Terracotta / Rust** | `#D85817` | `rgb(216, 88, 23)` | Bordes activos, hover de botones, textos sobre fondos claros |
| **Brand Gold Ochre** | **Amber Ochre** | `#C0831F` | `rgb(192, 131, 31)` | Delineados de coronas, detalles VIP, acentos menores |
| **Canvas Background** | **Vanilla Cream** | `#FAF6F0` | `rgb(250, 246, 240)` | Fondo general de páginas (`<body>`), descansa la vista |
| **Surface Card** | **Warm White** | `#FFFFFF` | `rgb(255, 255, 255)` | Tarjetas de producto, modales, paneles de formulario |
| **Text Primary** | **Dark Chocolate** | `#2B1810` | `rgb(43, 24, 16)` | Encabezados, títulos, precios destacados (legibilidad alta) |
| **Text Secondary** | **Cocoa Muted** | `#665349` | `rgb(102, 83, 73)` | Subtítulos, labels de formulario, descripción de packs |
| **Borders & Dividers** | **Toffee Border** | `#EADFCF` | `rgb(234, 223, 207)` | Líneas divisorias de tablas, bordes de inputs y cards |
| **Success** | **Mint Green** | `#28A745` | `rgb(40, 167, 69)` | Pedido "Entregado", Stock disponible, Pago exitoso |
| **Warning** | **Caramel Amber** | `#E69500` | `rgb(230, 149, 0)` | Pedido "Pendiente", Stock bajo, Oferta temporal |
| **Danger** | **Cherry Berry** | `#DC3545` | `rgb(220, 53, 69)` | Pedido "Cancelado", Sin stock, Eliminar usuario |
| **Info / Logistic** | **Marine Sugar** | `#2B6CB0` | `rgb(43, 108, 176)` | Pedido "En Ruta", Distribución, Información de ruta |

### Variables CSS Oficiales (`design-tokens.css`)

```css
:root {
  /* Brand */
  --dm-primary: #F58721;
  --dm-primary-hover: #E07312;
  --dm-primary-light: #FDE8D4;
  --dm-secondary: #FAC933;
  --dm-secondary-light: #FEF6D7;
  --dm-rust: #D85817;
  --dm-gold: #C0831F;

  /* Neutrals */
  --dm-bg-canvas: #FAF6F0;
  --dm-bg-surface: #FFFFFF;
  --dm-bg-subtle: #F5EEE4;
  --dm-border: #EADFCF;
  --dm-border-strong: #D5C2AA;

  /* Typography */
  --dm-text-primary: #2B1810;
  --dm-text-secondary: #665349;
  --dm-text-muted: #8F7B70;
  --dm-text-inverse: #FFFFFF;

  /* Semantic */
  --dm-success: #28A745;
  --dm-success-bg: #E8F5E9;
  --dm-warning: #E69500;
  --dm-warning-bg: #FFF8E1;
  --dm-danger: #DC3545;
  --dm-danger-bg: #FFEBEE;
  --dm-info: #2B6CB0;
  --dm-info-bg: #EBF4FC;

  /* Shadows */
  --dm-shadow-sm: 0 1px 3px rgba(43, 24, 16, 0.06);
  --dm-shadow-md: 0 4px 12px rgba(43, 24, 16, 0.08);
  --dm-shadow-lg: 0 10px 24px rgba(43, 24, 16, 0.12);

  /* Radius */
  --dm-radius-sm: 6px;
  --dm-radius-md: 10px;
  --dm-radius-lg: 16px;
  --dm-radius-full: 9999px;
}
```

---

## 5. Tipografía y Jerarquía

Utilizamos fuentes sans-serif modernas, legibles a cualquier resolución y con trazos amables:

- **Pila tipográfica primaria:** `system-ui, -apple-system, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif`  
- Si se incorpora Google Fonts: **`Plus Jakarta Sans`** o **`Inter`**.

### Escala de Tamaños y Pesos

| Nivel | Tamaño | Peso | Interlineado | Caso de Uso |
| :--- | :--- | :--- | :--- | :--- |
| **H1 (Display)** | `32px` (`2rem`) | Bold (`700`) | `1.2` | Título de landing, títulos principales de pantalla |
| **H2 (Sección)** | `24px` (`1.5rem`) | SemiBold (`600`) | `1.25` | Categorías de catálogo, títulos de módulos |
| **H3 (Tarjeta)** | `18px` (`1.125rem`) | SemiBold (`600`) | `1.3` | Nombres de productos, cabeceras de tablas |
| **Body (Cuerpo)** | `15px` (`0.9375rem`) | Regular (`400`) | `1.5` | Datos de producto, descripción de packs, celdas |
| **Small / Badges** | `12px` (`0.75rem`) | Medium (`500`) | `1.4` | Etiquetas de stock, bultos, códigos SKU |

---

## 6. Componentes Estandarizados

### A. Botones

```html
<!-- Primario (Acción principal) -->
<button class="btn-primary">Agregar al pedido</button>

<!-- Secundario (Filtros, acciones complementarias) -->
<button class="btn-secondary">Ver detalle</button>

<!-- Outline (Volver, cancelar) -->
<button class="btn-outline">Cancelar</button>
```

```css
.btn-primary {
  background-color: var(--dm-primary);
  color: var(--dm-text-inverse);
  font-weight: 600;
  font-size: 14px;
  padding: 10px 18px;
  border-radius: var(--dm-radius-md);
  border: 1px solid var(--dm-rust);
  box-shadow: 0 2px 4px rgba(216, 88, 23, 0.2);
  cursor: pointer;
  transition: background-color 0.15s ease, transform 0.05s ease;
}

.btn-primary:hover {
  background-color: var(--dm-primary-hover);
}

.btn-primary:active {
  transform: translateY(1px);
}

.btn-secondary {
  background-color: var(--dm-secondary-light);
  color: var(--dm-rust);
  font-weight: 600;
  font-size: 14px;
  padding: 10px 18px;
  border-radius: var(--dm-radius-md);
  border: 1px solid var(--dm-secondary);
  cursor: pointer;
}

.btn-secondary:hover {
  background-color: #FDEDB8;
}

.btn-outline {
  background-color: transparent;
  color: var(--dm-text-secondary);
  font-weight: 500;
  font-size: 14px;
  padding: 10px 18px;
  border-radius: var(--dm-radius-md);
  border: 1px solid var(--dm-border-strong);
  cursor: pointer;
}

.btn-outline:hover {
  background-color: var(--dm-bg-subtle);
  color: var(--dm-text-primary);
}
```

### B. Tarjeta de Producto Mayorista

Cada tarjeta de producto debe mostrar inmediatamente:

1. Foto del producto sobre fondo neutro/blanco.
2. Badge de presentación (ej. `Display x 24 un.` o `Caja x 12 displays`).
3. Nombre claro y marca (ej. `Bon o Bon Leche`).
4. Precio por bulto y precio unitario calculado en ARS / USD.
5. Indicador de stock en tiempo real (`En stock`, `Pocas unidades`, `Agotado`).
6. Selector numérico de cantidad + Botón `+`.

```html
<article class="dm-card">
  <div class="dm-card-badge">Display x 24 u.</div>
  <img src="/images/products/bonobon.jpg" alt="Bon o Bon Leche" class="dm-card-img" />
  <div class="dm-card-body">
    <span class="dm-card-brand">Arcor</span>
    <h3 class="dm-card-title">Bon o Bon Leche</h3>
    <div class="dm-card-prices">
      <span class="dm-price-bulk">$14.200 <small>/display</small></span>
      <span class="dm-price-unit">($591,66 c/u)</span>
    </div>
    <div class="dm-stock-pill in-stock">● En stock (140 disp.)</div>
    <div class="dm-card-actions">
      <div class="dm-qty-picker">
        <button>-</button>
        <input type="number" value="1" min="1" />
        <button>+</button>
      </div>
      <button class="btn-primary">Agregar</button>
    </div>
  </div>
</article>
```

### C. Badges de Estado de Pedidos (Unificados en las 3 Apps)

Para garantizar consistencia entre el cliente web, la app del preventista y el backoffice:

| Estado Backend | Texto en UI | Color Fondo | Color Texto | Ícono Sugerido |
| :--- | :--- | :--- | :--- | :--- |
| `PENDING` | **Pendiente de confirmación** | `#FFF8E1` | `#B7791F` | ⏳ Reloj |
| `IN_PREPARATION` | **En preparación en almacén** | `#EBF4FC` | `#2B6CB0` | 📦 Caja |
| `ON_ROUTE` | **En camino con distribuidor** | `#E1F5FE` | `#0277BD` | 🚚 Camión |
| `DELIVERED` | **Entregado al comercio** | `#E8F5E9` | `#28A745` | ✔ Check |
| `CANCELLED` | **Cancelado** | `#FFEBEE` | `#C62828` | ✖ Cruz |

### D. Tablas de Gestión (Central Management & Reportes)

- Fondo de cabecera: `#FAF0E2` con texto `#2B1810` en negrita (`font-weight: 600`).
- Filas alternadas o hover suave: `#FFFBF6`.
- Bordes tenues: `1px solid #EADFCF`.
- Paginación limpia: Números de página y selector de elementos por página (`10`, `25`, `50`), sin animaciones que traben el scroll.

### E. Diálogos Modales (Modal Dialogs)

Los modales se utilizan para visualización de detalles, formularios de edición rápida, reabastecimiento de stock y confirmaciones destructivas:

| Tamaño | Ancho Máximo | Caso de Uso Típico |
| :--- | :--- | :--- |
| **Standard** | `max-w-lg` (`512px` / `500px`) | Modales de confirmación, reposición de stock, cambio de estado de pedido |
| **Large** | `max-w-2xl` (`672px` / `680px`) | Creación y edición de productos, detalle completo de pedidos con desglose de ítems |

**Directrices de Anatomía Modal:**
- **Overlay / Backdrop:** `rgba(43, 24, 16, 0.5)` (Cálido chocolate semitransparente con `backdrop-blur-sm`). Cero fondos negros azulados fríos.
- **Contenedor:** Fondo blanco `#FFFFFF`, bordes redondeados `12px` a `16px`, borde suave `1px solid var(--dm-border)`.
- **Altura Máxima:** `max-h-[90vh]` con scrollbar vertical limpio (`overflow-y-auto`) para no cortar contenido en pantallas compactas o laptops.
- **Cabecera (Header):** Título descriptivo con ícono de sección (ej. paquete para producto, carrito para pedido), subtítulo breve en `--dm-text-secondary`, y botón de cierre (`✕`) accesible en la esquina superior derecha.
- **Pie de Acciones (Footer):** Alineado a la derecha con botón secundario/outline ("Cancelar") a la izquierda y botón de acción principal ("Guardar Cambios", "Confirmar") a la derecha.

### F. Tratamiento y Carga de Imágenes de Producto (Dual-Mode Image Upload)

Para garantizar flexibilidad operativa y persistencia confiable sin requerir servicios de almacenamiento externo tipo S3 durante el desarrollo, la carga de fotos de producto soporta **dos modos intercambiables**:

```text
┌────────────────────────────────────────────────────────┐
│  Foto del Producto                                     │
│  [  Enlace web (URL)  ]   [  Subir archivo local  ]    │
│  ┌──────────────────────────────────────────────────┐  │
│  │ https://images.unsplash.com/...                  │  │
│  └──────────────────────────────────────────────────┘  │
│  Vista previa: [ 📷 Miniatura 80x80 ]                  │
└────────────────────────────────────────────────────────┘
```

1. **Modo URL Directa:**
   - Permite enlazar fotos alojadas en CDN, sitios de fabricantes (Arcor, Mondelez) o Unsplash.
   - Validación de formato: URLs que inicien con `http://` o `https://`.
2. **Modo Archivo Local (File Upload con Base64):**
   - El usuario selecciona un archivo desde su equipo (`.jpg`, `.jpeg`, `.png`, `.webp`).
   - La aplicación cliente convierte inmediatamente el archivo a un Data URL Base64 (`data:image/jpeg;base64,...`) mediante `FileReader.readAsDataURL()`.
   - El backend almacena el Data URL en la columna `@Lob image_url` de la entidad `Product`.
   - Límite recomendado: Máximo **2 MB** por imagen.
3. **Especificaciones de Display de Imagen:**
   - **Aspect Ratio:** 1:1 (cuadrado) en miniaturas de tablas (`w-12 h-12`), 4:3 o 1:1 en tarjetas de catálogo y modales de detalle (`w-full h-44 object-cover`).
   - **Fondo de Contenedor:** `#FAF6F0` (Vanilla Cream) para armonizar con envases que tengan fondo blanco o transparente.
   - **Fallback Placeholder:** Si el producto no dispone de imagen (`imageUrl == null`), se muestra un contenedor neutro con el ícono de paquete (`Package`) en tono toffee `#D5C2AA`.

### G. Grupos de Botones de Acción (Action Button Groups)

En tablas y listados administrativos, las acciones por fila deben ser compactas, claras y con semántica visual inequívoca:

| Acción | Icono | Estilo de Botón | Color Texto / Borde | Color Fondo Hover |
| :--- | :--- | :--- | :--- | :--- |
| **Ver Detalle** | `Eye` | Micro-pill con borde | `#2B6CB0` (Marine Sugar) | `#EBF4FC` |
| **Editar** | `Edit2` | Micro-pill con borde | `#D85817` (Terracotta) | `#FDE8D4` |
| **Stock** | `PackagePlus` | Micro-pill con borde | `#28A745` (Mint Green) | `#E8F5E9` |
| **Eliminar** | `Trash2` | Micro-pill con borde | `#DC3545` (Cherry Berry) | `#FFEBEE` |

- Toda acción destructiva (**Eliminar producto**, **Cancelar pedido**, **Dar de baja usuario**) debe abrir obligatoriamente un modal de confirmación con botón rojo `--dm-danger` antes de ejecutar la mutación contra la API.

### H. Tablas de Desglose de Pedidos (Order Details Breakdown)

El modal de detalle de pedidos estandariza la presentación financiera para preventistas y operadores de almacén:

```text
┌───────────────────────────────────────────────────────────────────┐
│  Ítems del Pedido (#ORD-001)                                      │
├───────────────────────┬────────────┬─────────────┬────────────────┤
│ Producto              │ Cantidad   │ Precio Unit.│ Subtotal       │
├───────────────────────┼────────────┼─────────────┼────────────────┤
│ Bon o Bon Leche       │ 3 bultos   │ $14.200     │ $42.600        │
│ Galletitas Oreo 118g  │ 5 bultos   │ $16.500     │ $82.500        │
├───────────────────────┴────────────┴─────────────┼────────────────┤
│ Subtotal:                                        │ $125.100       │
│ Cupón Dulce Mayor (BIENVENIDA10 -10%):           │ -$12.510       │
│ TOTAL A COBRAR:                                  │ $112.590       │
└──────────────────────────────────────────────────┴────────────────┘
```

- Las líneas de descuento / cupones promocionales se destacan en verde `--dm-success`.
- El total final se enfatiza en negrita tamaño `1.25rem` con el color corporativo Dark Chocolate `#2B1810`.

---

## 7. Directrices por Aplicación

### 1. `client/landing` — Storefront Público & Portal Minorista

- **Objetivo:** Facilitar la compra rápida y el reordenamiento de los kioscos y almacenes minoristas.
- **Header:** Barra blanca con acento naranja superior, logotipo horizontal visible, buscador de golosinas directo (por marca/tipo), selector de cotización USD/ARS y acceso al carrito flotante.
- **Chatbot Asistente (Gemini):** Modal limpio en la esquina inferior derecha con encabezado `#F58721`, burbujas en blanco y crema. El bot responde con tablas cortas y sugerencias directas con botón "Agregar al pedido", sin explicaciones teóricas largas.
- **Historial de Pedidos:** Tabla sencilla con botón para descargar factura PDF y ver código QR de billetera virtual.

### 2. `client/order-management` — Operaciones Móviles de Campo (PWA)

- **Objetivo:** Eficiencia máxima bajo luz solar directa en la calle para Preventores y Distribuidores.
- **Layout:** Mobile-First con barra de navegación inferior fija (Tabs: `Ruta de Hoy`, `Mapa`, `Nuevo Pedido`, `Clientes`).
- **Mapa Leaflet:** Marcadores circulares con el color del estado del comercio (verde: visitado/entregado, naranja: próxima parada sugerida, gris: pendiente).
- **Botón de Acción Rápida (FAB):** Botón flotante naranja grande en la esquina inferior derecha para marcar "ENTREGADO" o "INICIAR PEDIDO" en 1 toque.

### 3. `client/central-management` — Backoffice Administrativo y Almacén

- **Objetivo:** Control total de inventario, facturación, precios y usuarios sin distracciones.
- **Sidebar:** Barra lateral izquierda fija en tono `#2B1810` (Chocolate oscuro corporativo) con acentos dorados `#FAC933` en la opción activa. Brinda contraste profesional y descansa la vista en jornadas de 8 horas.
- **Gráficos (Chart.js o Recharts):** Barras y tortas utilizando estrictamente los colores de la paleta (Naranja, Miel, Caramelo, Menta, Chocolate).
- **Modales de Reposición:** Formularios directos: `Producto`, `Cantidad de bultos a ingresar`, `Proveedor`, botón `Confirmar Reposición`.

---

## 8. Checklist de Control de Calidad (Pre-Deploy)

Antes de considerar terminada cualquier vista o componente en cualquiera de las 3 apps, verificar:

- [ ] **¿El logo utilizado es el vector SVG oficial?** (`logo.svg` o `logo-horizontal.svg`).
- [ ] **¿Los textos son directos y concisos?** (Cero párrafos de relleno o frases tipo chatbot genérico).
- [ ] **¿Los colores respetan la paleta cálida de Dulce Mayor?** (No hay morados fluorescentes ni grises fríos).
- [ ] **¿Las tarjetas de producto muestran precio claro por bulto y unidad?**
- [ ] **¿Los estados de pedido usan los nombres y colores unificados?** (`PENDING`, `IN_PREPARATION`, etc.).
- [ ] **¿La vista es usable en pantalla táctil/móvil?** (Botones de al menos 44px de alto para preventistas en la calle).
- [ ] **¿Se evitaron sombras y desenfoques (*blur*) innecesarios?**

---

## 9. Auditoría Automatizada de Paridad Visual con Playwright

Para garantizar que el manual de diseño no sea meramente declarativo, se implementó y ejecutó una suite de inspección visual mediante **Playwright** que navega e inspecciona los 3 frontends en simultáneo:

| Aplicación | Entorno / Stack | URL Auditada | Muestreo de Pantallas |
| :--- | :--- | :--- | :--- |
| `client/landing` | Spring Boot / Thymeleaf | `http://localhost:8081` | Home, Catálogo Minorista, Carrito, Contacto |
| `client/central-management` | React 19 / Vite / Tailwind | `http://localhost:5173` | Login, Dashboard, Productos, Modales CRUD, Pedidos, Promociones, Usuarios |
| `client/order-management` | React 19 / PWA Mobile | `http://localhost:5174` | Login Móvil, Rutas de Hoy, Catálogo Preventa, Comercios |

### A. Resultados de la Verificación de Tokens CSS Computados

La auditoría extrajo directamente los estilos computados del DOM en cada aplicación, validando una paridad del 100% en los tokens esenciales:

```json
{
  "canvasBackground": "rgb(250, 246, 240)",    /* #FAF6F0 - Vanilla Cream idéntico en las 3 apps */
  "bodyTextColor": "rgb(43, 24, 16)",          /* #2B1810 - Dark Chocolate idéntico */
  "primaryAccent": "rgb(245, 135, 33)",        /* #F58721 - Caramel Orange idéntico */
  "secondaryPills": "rgb(254, 246, 215)",      /* #FEF6D7 - Light Honey idéntico */
  "fontFamily": "system-ui, -apple-system, Segoe UI, Roboto",
  "borderRadius": "8px a 16px (desktop) / 9999px (pills)"
}
```

### B. Repositorio de Capturas de Pantalla de la Auditoría

Todas las capturas de alta resolución (desktop `1440x900` y mobile viewport `390x844`) se encuentran archivadas en [`docs/review_screenshots/`](file:///c:/Users/Lucho/Desktop/dulce-mayor/docs/review_screenshots/):

1. `01_landing_home.png` — Hero sobrio, logo SVG horizontal, buscador y promociones destacadas.
2. `02_landing_catalog.png` — Grid de productos con precios mayoristas y selector de bultos.
3. `03_landing_contact.png` — Información de distribuidora en Mendoza y formulario sin rodeos.
4. `04_central_login.png` — Autenticación limpia con Auth0 y credenciales corporativas.
5. `05_central_dashboard.png` — KPIs de stock, pedidos del día y atajos rápidos de gestión.
6. `06_central_products.png` — Tabla con miniaturas fotográficas y botones de acción unificados.
7. `07_central_product_modal.png` — Modal `max-w-2xl` con uploader dual de fotos (URL y archivo).
8. `08_central_orders.png` — Listado de pedidos con badges semánticos de estado.
9. `09_central_order_modal.png` — Modal de desglose de ítems con tabla de productos y cupón.
10. `10_central_promotions.png` — Administración de códigos de descuento activos.
11. `11_central_users.png` — Gestión de usuarios con roles y confirmación de baja.
12. `12_order_login.png` — Login táctil adaptado para repartidores y preventistas.
13. `13_order_routes.png` — Vista de ruta con paradas, comercios de Mendoza y estado de visita.
14. `14_order_catalog.png` — Catálogo táctil optimizado para toma rápida de pedidos.
15. `15_order_retailers.png` — Directorio de puntos de venta con coordenadas y teléfono directo.

# Manual de Diseño & Estilo Visual — Dulce Mayor

> **Guía Ejecutiva y Sistema de Diseño Unificado para Frontend**  
> Documento detallado canónico: 👉 **[`docs/DESIGN_SYSTEM.md`](file:///c:/Users/Lucho/Desktop/dulce-mayor/docs/DESIGN_SYSTEM.md)**

---

## 1. Identidad de Marca y Filosofía

Dulce Mayor es una distribuidora mayorista de golosinas fundada en Mendoza, Argentina (**Est. 1926**). Su identidad visual toma inspiración directa del legado histórico de las grandes fábricas y confiterías patrimoniales argentinas: **Bagley, Arcor, Terrabusi y Suchard**.

- **Tradición y Prestigio:** Sello circular clásico de confitería con bombón central y corona de honor.
- **Sobrio y Funcional:** Sin distracciones visuales; datos de stock, bultos y precios con máxima legibilidad.
- **Zero "AI Slop":** Cero textos hinchados, frases artificiales, gradientes morados/neón, sombras borrosas o vidrios transparentes (*glassmorphism*).

---

## 2. Paleta de Colores Oficial (Azul Real & Oro Caramelo)

| Token / Color | Hex | RGB | Función |
| :--- | :--- | :--- | :--- |
| **Royal Blue** (Primario) | `#1B449C` | `rgb(27, 68, 156)` | Botones de acción principal (CTA), enlaces destacados, isotipo |
| **Golden Caramel** (Secundario) | `#C88D33` | `rgb(200, 141, 51)` | Badges de promociones, coronas de honor, estrellas, "EST. 1926" |
| **Deep Royal Hover** | `#15367D` | `rgb(21, 54, 125)` | Estado hover de botones primarios |
| **Deep Navy** (Texto Principal) | `#0F1D38` | `rgb(15, 29, 56)` | Encabezados, títulos, precios, texto de alta legibilidad |
| **Slate Navy** (Secundario) | `#475569` | `rgb(71, 85, 105)` | Subtítulos, labels de formulario, información de packaging |
| **Vanilla Cream** (Fondo) | `#FAF5E9` | `rgb(250, 245, 233)` | Fondo general de páginas (`<body>`), descansa la vista |
| **Warm White** (Superficie) | `#FFFFFF` | `rgb(255, 255, 255)` | Tarjetas, modales, formularios, dropdowns |
| **Sand Border** | `#E5DCC9` | `rgb(229, 220, 201)` | Líneas divisorias, bordes de inputs y tablas |

### Colores Semánticos de Estado
- **Emerald Mint** (`#16A34A` / Fondo `#F0FDF4`): Pedido "Entregado", Stock disponible, Cupón aplicado.
- **Caramel Amber** (`#D97706` / Fondo `#FFFBEB`): Pedido "Pendiente", Stock bajo.
- **Cobalt Route** (`#2563EB` / Fondo `#EFF6FF`): Pedido "En preparación", Ruta de distribución.
- **Crimson Berry** (`#DC2626` / Fondo `#FEF2F2`): Pedido "Cancelado", Sin stock, Acción de eliminar.

---

## 3. Identidad Vectorial (SVG Oficial)

Los logotipos vectorizados oficiales se ubican en:
- **Sello Canónico:** [`docs/assets/logo.svg`](file:///c:/Users/Lucho/Desktop/dulce-mayor/docs/assets/logo.svg)
- **Sello Fondo Transparente:** [`docs/assets/logo-transparent.svg`](file:///c:/Users/Lucho/Desktop/dulce-mayor/docs/assets/logo-transparent.svg)
- **Logotipo Horizontal:** [`docs/assets/logo-horizontal.svg`](file:///c:/Users/Lucho/Desktop/dulce-mayor/docs/assets/logo-horizontal.svg)
- **Logotipo Horizontal Blanco:** [`docs/assets/logo-horizontal-white.svg`](file:///c:/Users/Lucho/Desktop/dulce-mayor/docs/assets/logo-horizontal-white.svg)
- **PNG Alta Resolución (512x512):** [`docs/assets/logo.png`](file:///c:/Users/Lucho/Desktop/dulce-mayor/docs/assets/logo.png)
- **Favicon (64x64):** [`docs/assets/favicon.png`](file:///c:/Users/Lucho/Desktop/dulce-mayor/docs/assets/favicon.png)

Distribuido a los 3 frontends en:
- `client/landing/src/main/resources/static/images/`
- `client/central-management/public/`
- `client/order-management/public/`

---

## 4. Estandarización de Componentes Clave

### A. Diálogos Modales
- **Backdrop:** `rgba(15, 29, 56, 0.45)` con suave difuminado (`backdrop-blur-sm`).
- **Ancho:** Standard (`500px`) para confirmaciones y stock; Large (`680px`) para edición de productos y detalle de pedidos.
- **Altura:** `max-h-[90vh]` con scroll vertical (`overflow-y-auto`).
- **Cabecera:** Título claro en negrita + ícono temático + botón de cierre (`✕`).
- **Footer:** Botón cancelar a la izquierda, acción principal o confirmación destructiva a la derecha.

### B. Carga Dual de Fotos de Producto
- **Modo URL Directa:** Vínculo web a CDN o sitios de fabricantes (`https://...`).
- **Modo Archivo Local:** Convierte imágenes (`.jpg`, `.jpeg`, `.png`, `.webp` hasta 2MB) a Data URL Base64 para almacenamiento en base de datos.
- **Vista Previa en Vivo:** Cuadrada 1:1 o 4:3 con botón para remover la foto.
- **Placeholder:** Si no existe foto, contenedor `#FAF5E9` con ícono de paquete `#D4C6AB`.

### C. Botones de Acción en Listados (Action Pills)
- `Ver`: `--dm-info` (`#2563EB`)
- `Editar`: `--dm-primary` (`#1B449C`)
- `Stock`: `--dm-success` (`#16A34A`)
- `Eliminar`: `--dm-danger` (`#DC2626`) con modal de confirmación obligatorio.

### D. Desglose Financiero de Pedidos
- Tabla con columnas: Producto (con SKU y packaging), Cantidad, Precio Unitario ($ ARS), Subtotal.
- Fila de Cupón/Descuento destacada en verde `--dm-success`.
- Fila de Total a Cobrar en negrita `1.25rem` `#0F1D38`.

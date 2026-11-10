# Manual de Diseño & Estilo Visual — Dulce Mayor

> **Guía Ejecutiva y Sistema de Diseño Unificado para Frontend**  
> Documento detallado canónico: 👉 **[`docs/DESIGN_SYSTEM.md`](file:///c:/Users/Lucho/Desktop/dulce-mayor/docs/DESIGN_SYSTEM.md)**

---

## 1. Identidad de Marca y Filosofía

Dulce Mayor es una distribuidora mayorista de golosinas de Mendoza, Argentina. El diseño visual se inspira directamente en los gigantes de la alimentación y confitería tradicional argentina: **Arcor, Bagley, Mondelez y Terrabusi**.

- **Cálido y Apetecible:** Uso de tonos miel, caramelo y chocolate.
- **Sobrio y Funcional:** Sin distracciones visuales; datos de stock, bultos y precios de fácil lectura.
- **Zero "AI Slop":** Cero textos hinchados, frases artificiales, gradientes morados/neón o vidrios desenfocados (*glassmorphism*).

---

## 2. Paleta de Colores Oficial

| Token / Color | Hex | RGB | Función |
| :--- | :--- | :--- | :--- |
| **Caramel Orange** (Primario) | `#F58721` | `rgb(245, 135, 33)` | Botones de acción principal (CTA), enlaces destacados |
| **Golden Honey** (Secundario) | `#FAC933` | `rgb(250, 201, 51)` | Badges de promociones, estrellas, detalles de corona |
| **Terracotta Rust** | `#D85817` | `rgb(216, 88, 23)` | Bordes activos, hover de botones, textos de acento |
| **Dark Chocolate** (Texto) | `#2B1810` | `rgb(43, 24, 16)` | Encabezados, títulos, precios, texto de alta legibilidad |
| **Cocoa Muted** (Secundario) | `#665349` | `rgb(102, 83, 73)` | Subtítulos, labels de formulario, información de packaging |
| **Vanilla Cream** (Fondo) | `#FAF6F0` | `rgb(250, 246, 240)` | Fondo general de páginas (`<body>`), descansa la vista |
| **Warm White** (Superficie) | `#FFFFFF` | `rgb(255, 255, 255)` | Tarjetas, modales, formularios, dropdowns |
| **Toffee Border** | `#EADFCF` | `rgb(234, 223, 207)` | Líneas divisorias, bordes de inputs y tablas |

### Colores Semánticos de Estado
- **Mint Green** (`#28A745` / Fondo `#E8F5E9`): Pedido "Entregado", Stock disponible, Cupón aplicado.
- **Caramel Amber** (`#E69500` / Fondo `#FFF8E1`): Pedido "Pendiente", Stock bajo.
- **Marine Sugar** (`#2B6CB0` / Fondo `#EBF4FC`): Pedido "En preparación", Ruta de distribución.
- **Cherry Berry** (`#DC3545` / Fondo `#FFEBEE`): Pedido "Cancelado", Sin stock, Acción de eliminar.

---

## 3. Identidad Vectorial (SVG Oficial)

Los logotipos vectorizados oficiales se ubican en:
- **Isotipo Canónico:** [`docs/assets/logo.svg`](file:///c:/Users/Lucho/Desktop/dulce-mayor/docs/assets/logo.svg)
- **Logotipo Horizontal:** [`docs/assets/logo-horizontal.svg`](file:///c:/Users/Lucho/Desktop/dulce-mayor/docs/assets/logo-horizontal.svg)

Distribuido a los 3 frontends en:
- `client/landing/src/main/resources/static/images/logo.svg` y `logo-horizontal.svg`
- `client/central-management/public/logo.svg` y `logo-horizontal.svg`
- `client/order-management/public/logo.svg` y `logo-horizontal.svg`

---

## 4. Estandarización de Componentes Clave

### A. Diálogos Modales
- **Backdrop:** `rgba(43, 24, 16, 0.5)` con suave difuminado (`backdrop-blur-sm`).
- **Ancho:** Standard (`500px`) para confirmaciones y stock; Large (`680px`) para edición de productos y detalle de pedidos.
- **Altura:** `max-h-[90vh]` con scroll vertical (`overflow-y-auto`).
- **Cabecera:** Título claro en negrita + ícono temático + botón de cierre (`✕`).
- **Footer:** Botón cancelar a la izquierda, acción principal o confirmación destructiva a la derecha.

### B. Carga Dual de Fotos de Producto
- **Modo URL Directa:** Vínculo web a CDN o sitios de fabricantes (`https://...`).
- **Modo Archivo Local:** Convierte imágenes (`.jpg`, `.jpeg`, `.png`, `.webp` hasta 2MB) a Data URL Base64 para almacenamiento en base de datos.
- **Vista Previa en Vivo:** Cuadrada 1:1 o 4:3 con botón para remover la foto.
- **Placeholder:** Si no existe foto, contenedor `#FAF6F0` con ícono de paquete `#D5C2AA`.

### C. Botones de Acción en Listados (Action Pills)
- `Ver`: `--dm-info` (`#2B6CB0`)
- `Editar`: `--dm-primary` (`#F58721`)
- `Stock`: `--dm-success` (`#28A745`)
- `Eliminar`: `--dm-danger` (`#DC3545`) con modal de confirmación obligatorio.

### D. Desglose Financiero de Pedidos
- Tabla con columnas: Producto (con SKU y packaging), Cantidad, Precio Unitario ($ ARS), Subtotal.
- Fila de Cupón/Descuento destacada en verde `--dm-success`.
- Fila de Total a Cobrar en negrita `1.25rem` `#2B1810`.

---

## 5. Auditoría de Paridad Visual con Playwright

Se validó con pruebas automatizadas en Playwright que las 3 aplicaciones (`landing`, `central-management`, `order-management`) cumplen exactamente con las mismas variables computadas:
- Fondo `rgb(250, 246, 240)`
- Texto principal `rgb(43, 24, 16)`
- Acento primario `rgb(245, 135, 33)`
- Radios de borde entre `8px` y `16px` (desktop) y `9999px` (pills móviles)

Las 15 capturas de pantalla de la auditoría y el reporte `style_report.json` se encuentran en [`docs/review_screenshots/`](file:///c:/Users/Lucho/Desktop/dulce-mayor/docs/review_screenshots/).

# Sistema de gestion

Proyecto final desarrollado para la materia Ingenieria de Software.

## Descripción

Sistema de gestion de la empresa mayorista de golosinas Dulce Mayor, el sistema permite gestionar productos, clientes minoristas, seguir pedidos y generar reportes de ventas.

## Tecnologías utilizadas

- Servidor: Java + Spring Boot
- Base de datos: PostgreSQL
- Landing page: Thymeleaf
- Aplicaciones de gestión: React

## Estructura del proyecto

```text
dulce-mayor/
├── client/                     # Frontend
│    ├── central-management/    # Aplicación de gestión central (Puerto 5173)
│    ├── order-management/      # Aplicación de gestión de pedidos (Puerto 5174)
│    └── landing/               # Landing page (Puerto 8081)
├── server/                     # Backend REST API (Puerto 8080)
└── start.sh                    # Script para levantar todo el entorno
```

## Ejecución rápida

Para levantar el **backend** y las **3 aplicaciones frontend** simultáneamente:

```bash
./start.sh
```

Para detener cualquier instancia activa en los puertos de la aplicación:

```bash
./start.sh --stop
```

# Sistema de Control de Inventario y Facturación — Licorería

Monorepo para un POS minorista/mayorista de productos sellados (botellas y empaques cerrados).

## Estructura

```
SISTEMA-POS-LICOR/
├── backend/     # API REST — Java 17 + Spring Boot 3 + Maven
└── frontend/    # SPA — React + Vite
```

### Backend (`backend/`)

Arquitectura por capas:

| Capa          | Paquete        | Responsabilidad                          |
|---------------|----------------|------------------------------------------|
| Controller    | `controller`   | Endpoints HTTP / contrato de la API      |
| Service       | `service`      | Lógica de negocio                        |
| Repository    | `repository`   | Acceso a datos (Spring Data JPA)         |
| Model         | `model`        | Entidades JPA                            |
| DTO           | `dto`          | Objetos de transferencia                 |
| Security      | `security`     | Spring Security (esqueleto JWT/roles)    |

### Frontend (`frontend/`)

| Carpeta                    | Uso                                      |
|----------------------------|------------------------------------------|
| `src/app/`                 | Rutas: Dashboard, Inventario, POS        |
| `src/components/ui/`       | Layout, sidebar y controles reutilizables|
| `src/components/caja/`     | Componentes del punto de venta           |
| `src/services/`            | Cliente HTTP hacia el backend            |

## Requisitos

- JDK 17+
- Maven 3.9+
- Node.js 20+
- MySQL 8+

## Base de datos

```sql
CREATE DATABASE pos_licoreria CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Ajusta usuario y contraseña en `backend/src/main/resources/application.yml`.

## Cómo ejecutar

**Backend** (puerto `8080`):

```bash
cd backend
mvn spring-boot:run
```

**Frontend** (puerto `5173`):

```bash
cd frontend
npm install
npm run dev
```

La SPA hace proxy de `/api` hacia `http://localhost:8080`.

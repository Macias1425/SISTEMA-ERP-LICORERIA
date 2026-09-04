# Sistema de Control de Inventario y Facturación — Licorería

Monorepo de un POS minorista/mayorista para productos sellados (botellas y empaques cerrados),
con inventario trazable por lotes, numeración fiscal autorizada y control operativo de ventas.
Operación en Nicaragua: zona horaria `America/Managua`.

```
SISTEMA-POS-LICOR/
├── backend/     # API REST — Java 17 + Spring Boot 3 + Maven
└── frontend/    # SPA — React 18 + Vite
```

---

## 1. Reglas del negocio implementadas

Estas reglas son el núcleo del sistema: el resto del código existe para sostenerlas.

### Unidad mínima de medida (UMM) y presentaciones

Todo el stock se guarda en **unidad mínima** (la botella). Cada producto declara sus
presentaciones con un `factorAUnidadMinima`: botella = 1, six-pack = 6, caja = 24.

- Comprar 20 cajas de 24 botellas ingresa `20 × 24 = 480` UMM al inventario.
- El precio de una presentación se deriva del precio por UMM: `precioUmm × factor`.
- La conversión vive en un solo lugar (`ConversionUnidades`), así que compras, ventas,
  mermas y kardex siempre hablan el mismo idioma.

### Costo promedio ponderado (CPP)

Cuando el proveedor sube el precio pero todavía hay existencias compradas más baratas,
reemplazar el costo falsearía el margen. Al recibir una compra el costo se recalcula en
**unidad mínima (UMM)**:

```
costoNuevo = (stockAnterior × costoAnterior + cantidadEntrante × costoEntrante)
             ─────────────────────────────────────────────────────────────────
                          stockAnterior + cantidadEntrante
```

**Reglas de excepción:** si `stockAnterior ≤ 0` o no hay cantidad entrante, el nuevo costo
adopta directamente el costo unitario de la recepción (sin ponderar).

**Implementación:** `CompraService.actualizarCostoProducto()` convierte el costo por
presentación (caja, six-pack…) a UMM con `factorAUnidadMinima`, aplica la fórmula y persiste
en `Producto.precioCompra`. Cada cambio queda en auditoría (`AccionAuditoria.CAMBIO_PRECIO`).

### Ciclo de compras (Abastecimiento)

| Estado (`EstadoCompra`) | Significado |
|-------------------------|-------------|
| `PENDIENTE`             | Orden registrada; aún no ingresa stock |
| `RECIBIDA`              | Mercancía recibida, kardex actualizado, CPP y políticas aplicadas |
| `ANULADA`               | Cancelada o recepción revertida |

**Backend:** `CompraService` · **Frontend:** `ComprasPage.jsx`, `RecibirCompraModal.jsx`

**Flujo de recepción** (`POST /api/compras/recibir` o recepción de orden pendiente):

1. Validación de líneas (producto activo, vencimiento, conversión a UMM).
2. **Kardex:** `InventarioService.ingresar()` con tipo `COMPRA` y lote (`EntradaLote`) con costo y vencimiento reales de la factura.
3. **CPP:** actualiza `precioCompra` si `actualizarCostos = true`.
4. **Catálogo proveedor:** `sincronizarPrecioCatalogo()` — el costo de la factura reemplaza o crea el precio en `precios_proveedor` (ya no bloquea si difiere del catálogo anterior).
5. **Política de precio:** `PoliticaPrecioService.aplicarTrasCompra()` según la política del producto.

**Conversión de empaques** (equivalente a `purchaseUnits.js` del diseño de referencia):

```
cantidadUMM     = cantidadPresentación × factorAUnidadMinima
costoUMM        = costoPorPresentación ÷ factorAUnidadMinima
subtotalLínea   = cantidadPresentación × costoPorPresentación
```

Las presentaciones viven en `Presentacion` (botella = 1, six-pack = 6, caja = 12/24/60…).
La conversión centralizada está en `ConversionUnidades`.

### Políticas de precio de venta

Cada producto declara `politicaPrecio` y opcionalmente `margenObjetivoPct` (default **20%**).
El **margen es comercial** (% sobre el precio de venta), no markup sobre costo.

**Fórmula de venta sugerida / automática:**

```
precioVenta = costo × 100 ÷ (100 − margen%)
```

Redondeo: `CEILING` a 2 decimales (C$).

```
                    ┌─────────────────────────────┐
                    │ Recepción / nuevo costo CPP │
                    └──────────────┬──────────────┘
                                   │
         ┌─────────────────────────┼─────────────────────────┐
         │                         │                         │
  AUTOMATICO_MARKUP            SUGERIDO                   MANUAL
         │                         │                         │
  Recalcula y aplica          Registra sugerido          Solo actualiza
  precioVenta en BD           en historial; no           costo; venta
  + auditoría                 modifica venta             sin cambio auto
```

| Política (`PoliticaPrecio`) | En recepción de compra | ¿Cambia `precioVenta`? | ¿Qué ve el usuario? |
|-----------------------------|------------------------|------------------------|---------------------|
| `MANUAL`                    | Solo CPP               | No                     | Nada; edita venta usted en Productos |
| `SUGERIDO`                  | CPP + aviso en historial | No                   | Panel amarillo: “proveedor subió, venda a C$X” |
| `AUTOMATICO_MARKUP`         | CPP + aplica venta + historial | Sí            | Nada antes de recibir; cambia solo y queda en historial |

**Servicios:** `PoliticaPrecioService` (reglas) · `ProductoService.aplicarEnCatalogo()` (al guardar producto con AUTO) · historial en `MarcasPreciosService` (eventos `CAMBIO_PRECIO`).

**Frontend:** `utils/politicaPrecio.js` (preview antes de recibir) · `ProductoFormModal.jsx` (venta readonly en AUTO) · panel verde en `RecibirCompraModal.jsx`.

**Validación:** el precio de venta no puede ser menor al costo de compra (`ProductoService`).

### Mapa diseño de referencia → código actual

| Concepto de referencia | Implementación en este repo |
|------------------------|----------------------------|
| `PurchaseOrderServiceImpl` | `CompraService` |
| `ProductCostServiceImpl` | `CompraService.actualizarCostoProducto()` |
| `ProductPricingPolicy` | `PoliticaPrecio` (`MANUAL`, `SUGERIDO`, `AUTOMATICO_MARKUP`) |
| `ProductPriceServiceImpl` | `PoliticaPrecioService` |
| `ProductCostHistory` / `ProductSalePriceHistory` | Tabla `auditoria` + `HistorialPrecioDTO` |
| `PurchaseReceiptImpactDTO` | `ImpactoRecepcionDTO` en `CompraDTO.impactosRecepcion` |
| `WarehouseReceiveOrder.jsx` | `RecibirCompraModal.jsx` |
| `purchaseUnits.js` | `ConversionUnidades` + `Presentacion.factorAUnidadMinima` |
| Estados DRAFT/ORDERED/PARTIAL… | `PENDIENTE`, `PARCIAL`, `RECIBIDA`, `CERRADA`, `ANULADA` |
| `purchasePrice` / `averageCost` / `lastPurchaseCost` | `precioCompra` (CPP) + `ultimoCostoCompra` |

### Compras, CPP y políticas (implementado)

- **Estados:** `PENDIENTE`, `PARCIAL`, `RECIBIDA`, `CERRADA`, `ANULADA`
- **Recepción parcial** con cantidades recibidas/rechazadas y notas QC por línea
- **Cierre manual** de órdenes parciales (`POST /api/compras/{id}/cerrar`)
- **Campos de costo:** `ultimoCostoCompra` + `precioCompra` (CPP) en producto
- **`ImpactoRecepcionDTO`** en respuesta de recepción (`impactosRecepcion` en `CompraDTO`)
- **Frontend:** `RecibirOrdenModal`, `ImpactoRecepcionPanel`, preview de markup en recepción directa
- **Sin rol bodeguero:** cualquier usuario con `COMPRAS_GESTIONAR` puede recibir órdenes

### Lotes y rotación FEFO

Cada entrada crea un `LoteInventario` con su propio costo y vencimiento. Las salidas
consumen **primero lo que vence antes** (First-Expired, First-Out) y el kardex guarda la
traza (`L20260830-12x6, L20260901-12x3`), de modo que se puede responder de qué lote salió
cada botella. `LoteService` también valora el inventario y detecta lo que está por caducar.

### Precio de venta: el servidor manda

El POS no calcula precios: los **cotiza**. `POST /api/ventas/cotizar` devuelve precio por
línea, tarifa aplicada (detal/mayorista según volumen), IVA, total y avisos (stock
insuficiente, producto vencido, precio fuera de catálogo). `TarifaVenta` es la aritmética
compartida entre la cotización y el cobro, así que el total en pantalla es el que se cobra.

### Normativa de licor

- Edad mínima configurable, con confirmación visual o fecha de nacimiento.
- Horario semanal autorizado para vender alcohol; fuera de él el cobro se bloquea.
- Los productos sin alcohol nunca quedan bloqueados por horario.

### Facturación fiscal (DGI)

`NumeracionFiscalService` asigna el correlativo dentro del rango autorizado, valida
vigencia de la autorización DGI, avisa cuando el rango está por agotarse y bloquea la emisión si venció.
Con el régimen fiscal desactivado el negocio sigue operando con su numeración interna.

### Control de acceso por permiso

Los roles (`ADMIN`, `CAJERO`, `ALMACENISTA`) definen un conjunto base de permisos y cada
usuario puede recibir permisos adicionales. `AccesoService` es el único punto que autoriza
operaciones (`exigirPermiso`, `exigirAlguno`), y `HorarioAccesoService` permite restringir
la sesión de un usuario a franjas horarias.

---

## 2. Arquitectura

### Backend (`backend/`)

| Capa       | Paquete      | Responsabilidad                                  |
|------------|--------------|--------------------------------------------------|
| Controller | `controller` | Endpoints HTTP y contrato de la API              |
| Service    | `service`    | Reglas de negocio y transacciones                |
| Repository | `repository` | Acceso a datos (Spring Data JPA)                 |
| Model      | `model`      | Entidades JPA y enums del dominio                |
| DTO        | `dto`        | Objetos de transferencia (entrada y salida)      |
| Security   | `security`   | JWT, filtros de acceso y política de contraseñas |

Servicios de dominio destacados:

| Servicio                   | Qué resuelve                                                     |
|----------------------------|------------------------------------------------------------------|
| `AccesoService`            | Autorización por permiso efectivo (rol base + adicionales)        |
| `CotizacionVentaService`   | Cotiza el carrito con las reglas reales de cobro                  |
| `TarifaVenta`              | Aritmética de precios compartida (presentación, subtotal, IVA)    |
| `LoteService`              | Alta de lotes, consumo FEFO, valoración y vencimientos            |
| `CompraService`            | Recepción de compras, CPP, sync catálogo proveedor y políticas de precio |
| `PoliticaPrecioService`    | Markup automático, sugerido y validación de margen objetivo              |
| `NumeracionFiscalService`  | Correlativo, rango y vigencia de la autorización DGI              |
| `MargenRiesgoService`      | Productos que se venden a pérdida o bajo el margen objetivo        |
| `AlertasService`           | Resumen de alertas filtrado por permisos del usuario              |
| `ControlVentasService`     | Eventos de riesgo: alto monto, cambios de precio, anulaciones      |

### Frontend (`frontend/`)

| Carpeta             | Uso                                                        |
|---------------------|------------------------------------------------------------|
| `src/app/`          | Una carpeta por módulo/ruta (POS, inventario, finanzas, …)  |
| `src/components/ui/`| Shell de la aplicación: layout, sidebar, topbar, modales    |
| `src/components/`   | Componentes por dominio (`caja/`, `inventario/`, `finanzas/`…)|
| `src/hooks/`        | Lógica reutilizable: cotización, alertas activas            |
| `src/services/`     | Cliente HTTP, un archivo por recurso de la API              |
| `src/utils/`        | Formato, conversión de presentaciones, navegación, CSV       |
| `src/auth/`         | Sesión, permisos y mapa de navegación                       |

El menú lateral está agrupado por ejes (**Operaciones, Inventario, Bodega, Sistema**), la
barra superior muestra migas de pan y el centro de alertas, y cada enlace lleva un contador
con lo que requiere atención en ese módulo.

---

## 3. Módulos de la aplicación

| Ruta               | Módulo            | Contenido                                                        |
|--------------------|-------------------|------------------------------------------------------------------|
| `/dashboard`       | Inicio            | Indicadores del día y accesos rápidos                            |
| `/pos`             | Punto de venta    | Escáner, cotización en vivo, cobro, arqueo de caja               |
| `/facturas`        | Facturas          | Consulta, anulación y número fiscal                              |
| `/control-ventas`  | Control de ventas | Eventos de riesgo, desempeño por cajero, reglas                  |
| `/reportes`        | Reportes          | Ventas, compras, productos y vencimientos                        |
| `/finanzas`        | Finanzas          | Resultado del período, flujo de caja y **precios en riesgo**     |
| `/inventario`      | Inventario        | Existencias, **lotes y vencimientos**, kardex, mermas            |
| `/compras`         | Abastecimiento    | Órdenes, recepción y costo ponderado                             |
| `/productos`       | Catálogo          | Productos, presentaciones y precios                              |
| `/marcas-precios`  | Marcas y precios  | Listas de precio por tipo de cliente                             |
| `/usuarios`        | Usuarios          | Altas, permisos finos y horarios de acceso                       |
| `/configuracion`   | Configuración     | Normativa, horarios, **facturación fiscal** y seguridad          |
| `/auditoria`       | Auditoría         | Bitácora de acciones sensibles                                   |
| `/mantenimiento`   | Mantenimiento     | Respaldos y salud de la base de datos                            |

---

## 4. Requisitos

- JDK 17+
- Maven 3.9+
- Node.js 20+
- MySQL 8+

## 5. Base de datos

```sql
CREATE DATABASE pos_licoreria CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Ajuste usuario y contraseña en `backend/src/main/resources/application.yml`.
La zona horaria del sistema es `America/Managua` (Nicaragua) y se inyecta como `Clock`, lo que
permite fijar el tiempo en las pruebas.

## 6. Cómo ejecutar

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
El primer arranque crea los datos iniciales, incluido el usuario administrador, que debe
cambiar su contraseña en el primer inicio de sesión.

## 7. Pruebas

```bash
cd backend
mvn test
```

Cubren la aritmética del dominio (conversión de unidades, cobro, costo ponderado),
las reglas de autorización y normativa, y un recorrido de integración completo
(`VentaCompletaIntegrationTest`): catálogo → compra recibida → turno de caja → cotización →
venta → factura, comprobando que stock, lotes y totales cuadran.

```bash
cd frontend
npm run build
```

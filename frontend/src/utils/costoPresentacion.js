/** Convierte precio de compra en UMM a costo por presentación. RN-INV-01 */
export function costoPorPresentacion(precioCompraUmm, factorAUnidadMinima = 1) {
  if (precioCompraUmm == null || precioCompraUmm === '') return '';
  const factor = Number(factorAUnidadMinima) || 1;
  return Number((Number(precioCompraUmm) * factor).toFixed(2));
}

/** Convierte costo por presentación a precio UMM del producto. */
export function costoAUmm(costoPresentacion, factorAUnidadMinima = 1) {
  if (costoPresentacion == null || costoPresentacion === '') return '';
  const factor = Number(factorAUnidadMinima) || 1;
  return Number((Number(costoPresentacion) / factor).toFixed(2));
}

/** Cantidad en presentación → unidades mínimas (botellas). */
export function cantidadAUmm(cantidad, factorAUnidadMinima = 1) {
  const factor = Number(factorAUnidadMinima) || 1;
  return Math.max(0, Math.round(Number(cantidad || 0) * factor));
}

/**
 * Costo ponderado al recibir mercancía (no altera precio de venta).
 * stockActualUmm = inventario antes de esta línea; cantidadEntranteUmm = lo que ingresa.
 */
export function costoPonderadoPreview(stockActualUmm, costoActualUmm, cantidadEntranteUmm, costoEntranteUmm) {
  const stock = Number(stockActualUmm) || 0;
  const anterior = Number(costoActualUmm) || 0;
  const cantNueva = Number(cantidadEntranteUmm) || 0;
  const costoNuevo = Number(costoEntranteUmm) || 0;
  if (cantNueva <= 0) return anterior;
  if (stock <= 0) return costoNuevo;
  return Number(((stock * anterior + cantNueva * costoNuevo) / (stock + cantNueva)).toFixed(2));
}

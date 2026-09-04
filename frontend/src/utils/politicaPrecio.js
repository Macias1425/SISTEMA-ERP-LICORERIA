export const POLITICA_PRECIO = {
  MANUAL: 'MANUAL',
  SUGERIDO: 'SUGERIDO',
  AUTOMATICO_MARKUP: 'AUTOMATICO_MARKUP',
};

export const POLITICA_OPCIONES = [
  {
    value: POLITICA_PRECIO.MANUAL,
    label: 'Manual',
    hint: 'En compras solo actualiza el costo ponderado. El precio de venta lo cambia usted en el producto.',
  },
  {
    value: POLITICA_PRECIO.SUGERIDO,
    label: 'Sugerido',
    hint: 'Si el proveedor sube el costo, el sistema le avisa a qué precio vender para no perder margen. No cambia el POS solo.',
  },
  {
    value: POLITICA_PRECIO.AUTOMATICO_MARKUP,
    label: 'Automático',
    hint: 'Al recibir compras recalcula y aplica la venta solo, sin preguntarle. Queda en historial de precios.',
  },
];

export const MARGEN_DEFECTO = 20;

export function etiquetaPolitica(valor) {
  return POLITICA_OPCIONES.find((item) => item.value === valor)?.label || 'Manual';
}

export function margenEfectivo(producto, margenForm) {
  const margen = margenForm ?? producto?.margenObjetivoPct;
  const numero = Number(margen);
  if (Number.isFinite(numero) && numero > 0 && numero < 100) {
    return numero;
  }
  return MARGEN_DEFECTO;
}

/** Precio de venta desde costo y margen sobre venta (%). */
export function precioVentaDesdeMargen(costo, margenPct = MARGEN_DEFECTO) {
  const base = Number(costo);
  const margen = Number(margenPct);
  if (!Number.isFinite(base) || base <= 0) return null;
  if (!Number.isFinite(margen) || margen <= 0 || margen >= 100) return null;
  const divisor = 100 - margen;
  return Math.ceil((base * 100 / divisor) * 100) / 100;
}

/** Aviso visible solo para política SUGERIDO (no aplica venta). */
export function previewSugeridoCompra(producto, costoAnteriorUmm, costoPonderadoUmm) {
  if (!producto || costoPonderadoUmm == null) return null;
  if ((producto.politicaPrecio || POLITICA_PRECIO.MANUAL) !== POLITICA_PRECIO.SUGERIDO) return null;
  const margen = margenEfectivo(producto);
  const sugerido = precioVentaDesdeMargen(costoPonderadoUmm, margen);
  if (sugerido == null) return null;
  const anterior = Number(costoAnteriorUmm ?? producto.precioCompra ?? 0);
  const nuevo = Number(costoPonderadoUmm);
  const ventaActual = Number(producto.precioVenta);
  const subio = nuevo > anterior + 0.009;

  let mensaje;
  if (subio) {
    mensaje = `El proveedor subió el costo. Tras el ponderado (C$${nuevo.toFixed(2)}/bot.), venda a C$${sugerido.toFixed(2)} para mantener ${margen}% de margen y no perder ganancias. El POS sigue en C$${Number.isFinite(ventaActual) ? ventaActual.toFixed(2) : '0.00'} hasta que usted lo cambie.`;
  } else {
    mensaje = `Costo ponderado estimado C$${nuevo.toFixed(2)}/bot. Para ${margen}% de margen, precio sugerido: C$${sugerido.toFixed(2)}. Revise en Marcas y precios → Historial.`;
  }

  return {
    politica: POLITICA_PRECIO.SUGERIDO,
    margen,
    costoAnterior: anterior,
    costoPonderadoUmm: nuevo,
    ventaActual: Number.isFinite(ventaActual) ? ventaActual : null,
    precioSugerido: sugerido,
    subio,
    mensaje,
  };
}

/** Vista previa antes de recibir: solo MANUAL y SUGERIDO (automático actúa en silencio). */
export function previewPoliticaTrasCompra(producto, nuevoCostoUmm, costoAnteriorUmm) {
  if (!producto || nuevoCostoUmm == null) return null;
  const politica = producto.politicaPrecio || POLITICA_PRECIO.MANUAL;

  if (politica === POLITICA_PRECIO.AUTOMATICO_MARKUP) {
    return null;
  }
  if (politica === POLITICA_PRECIO.MANUAL) {
    return {
      politica,
      mensaje: 'Solo se actualizará el costo ponderado. El precio de venta no cambia hasta que usted lo edite.',
    };
  }
  return previewSugeridoCompra(producto, costoAnteriorUmm, nuevoCostoUmm);
}

/** Automático no muestra preview en pantalla. */
export function previewMarkupCompra() {
  return null;
}

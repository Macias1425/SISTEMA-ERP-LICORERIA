const LOCALE = 'es-NI';
const SIMBOLO = 'C$';
const SIN_DATO = '—';

const decimales2 = new Intl.NumberFormat(LOCALE, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const enteros = new Intl.NumberFormat(LOCALE);

/**
 * Córdobas con símbolo. Trata el nulo como cero, porque en un total de venta
 * un guion confunde más que un C$ 0.00.
 */
export function dinero(valor) {
  const monto = Number(valor || 0);
  const texto = `${SIMBOLO} ${decimales2.format(Math.abs(monto))}`;
  return monto < 0 ? `−${texto}` : texto;
}

/** Versión compacta para KPIs estrechos: C$ 1.2M, C$ 34K. */
export function dineroCorto(valor) {
  const monto = Number(valor || 0);
  const signo = monto < 0 ? '−' : '';
  const absoluto = Math.abs(monto);
  if (absoluto >= 1_000_000) return `${signo}${SIMBOLO} ${(absoluto / 1_000_000).toFixed(1)}M`;
  if (absoluto >= 1_000) return `${signo}${SIMBOLO} ${Math.round(absoluto / 1_000)}K`;
  return `${signo}${SIMBOLO} ${absoluto.toFixed(2)}`;
}

/** Igual que {@link dinero} pero deja el guion cuando de verdad no hay dato que mostrar. */
export function moneda(valor) {
  if (valor === null || valor === undefined || valor === '') return SIN_DATO;
  return dinero(valor);
}

export function porcentaje(valor, decimales = 1) {
  if (valor === null || valor === undefined || valor === '') return SIN_DATO;
  return `${Number(valor).toFixed(decimales)}%`;
}

export function numero(valor) {
  if (valor === null || valor === undefined || valor === '') return SIN_DATO;
  return enteros.format(Number(valor));
}

export function fecha(valor) {
  if (!valor) return SIN_DATO;
  return new Date(valor).toLocaleDateString(LOCALE, {
    day: '2-digit', month: '2-digit', year: 'numeric',
  });
}

export function fechaHora(valor) {
  if (!valor) return SIN_DATO;
  return new Date(valor).toLocaleString(LOCALE, {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  });
}

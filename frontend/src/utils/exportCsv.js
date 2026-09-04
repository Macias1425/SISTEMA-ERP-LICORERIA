const NEGOCIO = 'Sistema POS Licorería';

function escapar(valor) {
  const texto = valor == null ? '' : String(valor);
  return `"${texto.replace(/"/g, '""')}"`;
}

export function ahoraDocumento() {
  return new Date().toLocaleString('es-NI', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function archivoDocumento(titulo, { desde, hasta, fecha } = {}) {
  const limpio = String(titulo || 'documento')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-zA-Z0-9]+/g, '_')
    .replace(/^_|_$/g, '');
  const rango = desde && hasta ? `${desde}_al_${hasta}` : (fecha || new Date().toISOString().slice(0, 10));
  return `${limpio}_${rango}`;
}

export function textoCelda(valor, vacio = 'No registrado') {
  if (valor == null || String(valor).trim() === '') return vacio;
  return String(valor);
}

/** Montos en córdobas para CSV e impresión (Nicaragua). */
export function cordoba(valor, vacio = 'No registrado') {
  if (valor == null || valor === '') return vacio;
  const numero = Number(valor);
  if (Number.isNaN(numero)) return vacio;
  const texto = `C$ ${Math.abs(numero).toFixed(2)}`;
  return numero < 0 ? `−${texto}` : texto;
}

export function periodoTexto(desde, hasta) {
  if (desde && hasta && desde === hasta) return `del ${desde}`;
  if (desde && hasta) return `del ${desde} al ${hasta}`;
  if (desde) return `desde el ${desde}`;
  if (hasta) return `hasta el ${hasta}`;
  return '';
}

/**
 * Descarga un CSV con encabezado de documento (negocio, título, fecha, período)
 * y columnas en español. El BOM UTF-8 permite abrirlo bien en Excel.
 */
export function descargarCsv(nombreArchivo, columnas, filas, meta = {}) {
  const registros = Array.isArray(filas) ? filas : [];
  const lineas = [];
  lineas.push(escapar(meta.negocio || NEGOCIO));
  if (meta.titulo) lineas.push(escapar(meta.titulo));
  if (meta.subtitulo) lineas.push(escapar(meta.subtitulo));
  lineas.push(escapar(`Fecha de generación: ${meta.generadoEl || ahoraDocumento()}`));
  const periodo = meta.periodo || periodoTexto(meta.desde, meta.hasta);
  if (periodo) lineas.push(escapar(`Período: ${periodo}`));
  if (meta.filtros) lineas.push(escapar(`Filtros aplicados: ${meta.filtros}`));
  lineas.push(escapar(`Cantidad de registros: ${registros.length}`));
  lineas.push(escapar('Montos expresados en córdobas (C$). Documento de uso interno.'));
  lineas.push('');
  lineas.push(columnas.map((columna) => escapar(columna.label)).join(','));
  registros.forEach((fila) => {
    lineas.push(columnas.map((columna) => {
      const valor = typeof columna.format === 'function'
        ? columna.format(fila)
        : fila?.[columna.key];
      return escapar(valor == null || valor === '' ? 'No registrado' : valor);
    }).join(','));
  });

  const blob = new Blob([`\uFEFF${lineas.join('\n')}`], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombreArchivo.endsWith('.csv') ? nombreArchivo : `${nombreArchivo}.csv`;
  enlace.click();
  URL.revokeObjectURL(url);
}

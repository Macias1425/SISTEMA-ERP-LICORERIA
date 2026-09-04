export const TAMANO_PAGINA_DEFAULT = 20;

export function esPagina(respuesta) {
  return respuesta != null
    && typeof respuesta === 'object'
    && !Array.isArray(respuesta)
    && Array.isArray(respuesta.contenido)
    && respuesta.pagina != null
    && Number.isFinite(Number(respuesta.pagina));
}

export function contenidoPagina(respuesta) {
  if (Array.isArray(respuesta)) return respuesta;
  if (esPagina(respuesta)) return respuesta.contenido;
  return [];
}

export function metaPagina(respuesta) {
  if (!esPagina(respuesta)) {
    return {
      pagina: 0,
      tamano: contenidoPagina(respuesta).length,
      totalElementos: contenidoPagina(respuesta).length,
      totalPaginas: 1,
      primera: true,
      ultima: true,
    };
  }
  return {
    pagina: Number(respuesta.pagina) || 0,
    tamano: Number(respuesta.tamano) || TAMANO_PAGINA_DEFAULT,
    totalElementos: Number(respuesta.totalElementos) || 0,
    totalPaginas: Math.max(1, Number(respuesta.totalPaginas) || 1),
    primera: Boolean(respuesta.primera),
    ultima: Boolean(respuesta.ultima),
  };
}

export async function listarTodos(fetchPagina, params = {}, tamano = 200) {
  let pagina = 0;
  let acumulado = [];
  while (true) {
    const respuesta = await fetchPagina({ ...params, pagina, tamano });
    acumulado = acumulado.concat(contenidoPagina(respuesta));
    const meta = metaPagina(respuesta);
    if (meta.ultima || meta.totalPaginas <= 1) break;
    pagina += 1;
  }
  return acumulado;
}

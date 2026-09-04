import { NAV_GRUPOS, NAV_INICIO } from '../auth/permisos';

/** Páginas que cuelgan de un enlace del menú y por eso no aparecen en él. */
const SUBPAGINAS = {
  '/vencimientos': { padre: '/reportes', label: 'Vencimientos' },
  '/usuarios/permisos': { padre: '/usuarios', label: 'Permisos por usuario' },
};

const ENLACES_POR_RUTA = new Map(
  NAV_GRUPOS.flatMap((grupo) => grupo.enlaces.map((enlace) => [
    enlace.to,
    { eje: grupo.label, ejeId: grupo.id, label: enlace.label, to: enlace.to },
  ])),
);

/**
 * Ubica la ruta actual dentro de los ejes del menú para dibujar las migas de pan.
 * Devuelve `null` en pantallas fuera del menú (login, POS a pantalla completa).
 */
export function ubicarRuta(pathname) {
  if (pathname === NAV_INICIO.to) {
    return { eje: null, ejeId: null, pagina: NAV_INICIO.label, padre: null };
  }

  const subpagina = SUBPAGINAS[pathname];
  if (subpagina) {
    const padre = ENLACES_POR_RUTA.get(subpagina.padre);
    return padre
      ? { eje: padre.eje, ejeId: padre.ejeId, pagina: subpagina.label, padre }
      : { eje: null, ejeId: null, pagina: subpagina.label, padre: null };
  }

  const enlace = ENLACES_POR_RUTA.get(pathname);
  if (!enlace) {
    return null;
  }
  return { eje: enlace.eje, ejeId: enlace.ejeId, pagina: enlace.label, padre: null };
}

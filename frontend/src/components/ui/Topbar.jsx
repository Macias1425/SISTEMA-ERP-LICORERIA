import { Link } from 'react-router-dom';

import { NAV_INICIO } from '../../auth/permisos';
import { ubicarRuta } from '../../utils/navegacion';
import AlertasMenu from './AlertasMenu';

/** Barra de contexto: dice en qué eje del sistema está el usuario y qué requiere atención. */
export default function Topbar({
  pathname,
  alertas,
  cargandoAlertas,
  onRefrescarAlertas,
  menuAbierto,
  onToggleMenu,
}) {
  const ubicacion = ubicarRuta(pathname);

  return (
    <header className="topbar no-print">
      <button
        type="button"
        className="nav-toggle"
        aria-label={menuAbierto ? 'Cerrar menú' : 'Abrir menú'}
        aria-expanded={Boolean(menuAbierto)}
        onClick={onToggleMenu}
      >
        <span />
        <span />
        <span />
      </button>
      <nav className="breadcrumbs" aria-label="Ubicación">
        <Link to={NAV_INICIO.to}>{NAV_INICIO.label}</Link>
        {ubicacion?.eje ? (
          <>
            <span className="breadcrumb-sep" aria-hidden="true">/</span>
            <span className="breadcrumb-eje">{ubicacion.eje}</span>
          </>
        ) : null}
        {ubicacion?.padre ? (
          <>
            <span className="breadcrumb-sep" aria-hidden="true">/</span>
            <Link to={ubicacion.padre.to}>{ubicacion.padre.label}</Link>
          </>
        ) : null}
        {ubicacion && ubicacion.pagina !== NAV_INICIO.label ? (
          <>
            <span className="breadcrumb-sep" aria-hidden="true">/</span>
            <span className="breadcrumb-actual" aria-current="page">{ubicacion.pagina}</span>
          </>
        ) : null}
      </nav>

      <AlertasMenu resumen={alertas} cargando={cargandoAlertas} onRefrescar={onRefrescarAlertas} />
    </header>
  );
}

import { useEffect, useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';

import { useAlertasActivas } from '../../hooks/useAlertasActivas';
import Sidebar from './Sidebar';
import Topbar from './Topbar';

export default function Layout() {
  const location = useLocation();
  const caja = location.pathname === '/pos';
  const { resumen, cargando, refrescar } = useAlertasActivas();
  const [menuAbierto, setMenuAbierto] = useState(false);

  useEffect(() => {
    setMenuAbierto(false);
  }, [location.pathname]);

  if (caja) {
    return (
      <div className="app-shell app-shell-pos">
        <main className="content content-pos">
          <Outlet key={location.pathname} />
        </main>
      </div>
    );
  }

  return (
    <div className={`app-shell${menuAbierto ? ' menu-abierto' : ''}`}>
      {menuAbierto ? (
        <button
          type="button"
          className="nav-backdrop"
          aria-label="Cerrar menú"
          onClick={() => setMenuAbierto(false)}
        />
      ) : null}
      <Sidebar alertas={resumen} />
      <main className="content">
        <Topbar
          pathname={location.pathname}
          alertas={resumen}
          cargandoAlertas={cargando}
          onRefrescarAlertas={refrescar}
          menuAbierto={menuAbierto}
          onToggleMenu={() => setMenuAbierto((abierto) => !abierto)}
        />
        <Outlet key={location.pathname} />
      </main>
    </div>
  );
}

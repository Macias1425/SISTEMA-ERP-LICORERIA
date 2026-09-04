import { useEffect, useMemo, useState } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';

import { useAuth } from '../../auth/AuthContext';
import { NAV_GRUPOS, NAV_INICIO } from '../../auth/permisos';
import { alertasPorRuta } from '../../hooks/useAlertasActivas';
import Button from './Button';
import { Icono } from './Icono';

const etiquetaRol = {
  ADMIN: 'Administrador',
  CAJERO: 'Cajero',
  ALMACENISTA: 'Almacenista',
};

function enlaceActivo(pathname, to) {
  if (to === '/dashboard') return pathname === '/dashboard';
  return pathname === to || pathname.startsWith(`${to}/`);
}

function Chevron({ abierto }) {
  return (
    <svg
      className={`nav-group-chevron${abierto ? ' abierto' : ''}`}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      aria-hidden="true"
    >
      <path d="M9 6l6 6-6 6" />
    </svg>
  );
}

function Badge({ alerta }) {
  if (!alerta?.cantidad) return null;
  return (
    <span className={`nav-badge${alerta.nivel === 'CRITICA' ? ' critica' : ''}`}>
      {alerta.cantidad > 99 ? '99+' : alerta.cantidad}
    </span>
  );
}

export default function Sidebar({ alertas }) {
  const { usuario, tienePermiso, logout } = useAuth();
  const navigate = useNavigate();
  const { pathname } = useLocation();

  const alertasRuta = useMemo(() => alertasPorRuta(alertas), [alertas]);

  const inicioVisible = tienePermiso(...NAV_INICIO.permisos);

  const gruposVisibles = useMemo(
    () => NAV_GRUPOS.map((grupo) => ({
      ...grupo,
      enlaces: grupo.enlaces.filter((enlace) => tienePermiso(...enlace.permisos)),
    })).filter((grupo) => grupo.enlaces.length > 0),
    [tienePermiso],
  );

  const grupoActivoId = useMemo(
    () => gruposVisibles.find((grupo) => grupo.enlaces.some((enlace) => enlaceActivo(pathname, enlace.to)))?.id,
    [gruposVisibles, pathname],
  );

  const [abiertos, setAbiertos] = useState(() => new Set(gruposVisibles.map((g) => g.id)));

  useEffect(() => {
    setAbiertos((prev) => {
      if (prev.size > 0 || !gruposVisibles.length) return prev;
      return new Set(gruposVisibles.map((g) => g.id));
    });
  }, [gruposVisibles]);

  useEffect(() => {
    if (!grupoActivoId) return;
    setAbiertos((prev) => {
      if (prev.has(grupoActivoId)) return prev;
      const next = new Set(prev);
      next.add(grupoActivoId);
      return next;
    });
  }, [grupoActivoId]);

  function alternarGrupo(id) {
    setAbiertos((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  return (
    <aside className="sidebar">
      <div className="brand">
        <span className="brand-kicker">Licorería</span>
        POS
      </div>

      <nav className="nav" aria-label="Menú principal">
        {inicioVisible ? (
          <div className="nav-inicio">
            <NavLink to={NAV_INICIO.to} end>
              <Icono nombre={NAV_INICIO.icono} className="nav-icon" />
              {NAV_INICIO.label}
            </NavLink>
          </div>
        ) : null}

        {gruposVisibles.map((grupo) => {
          const abierto = abiertos.has(grupo.id);
          const activo = grupo.id === grupoActivoId;
          const alertasGrupo = grupo.enlaces
            .map((enlace) => alertasRuta.get(enlace.to))
            .filter(Boolean);
          const resumenGrupo = alertasGrupo.length
            ? {
              cantidad: alertasGrupo.reduce((suma, item) => suma + item.cantidad, 0),
              nivel: alertasGrupo.some((item) => item.nivel === 'CRITICA') ? 'CRITICA' : 'AVISO',
            }
            : null;

          return (
            <section
              key={grupo.id}
              className={`nav-group${abierto ? ' abierto' : ' cerrado'}${activo ? ' activo' : ''}`}
            >
              <button
                type="button"
                className="nav-group-title"
                onClick={() => alternarGrupo(grupo.id)}
                aria-expanded={abierto}
              >
                <span>{grupo.label}</span>
                <span className="nav-group-meta">
                  {abierto ? null : <Badge alerta={resumenGrupo} />}
                  <Chevron abierto={abierto} />
                </span>
              </button>

              <div className="nav-group-items">
                {grupo.enlaces.map((link) => (
                  <NavLink key={link.to} to={link.to} end>
                    <Icono nombre={link.icono} className="nav-icon" />
                    <span className="nav-label">{link.label}</span>
                    <Badge alerta={alertasRuta.get(link.to)} />
                  </NavLink>
                ))}
              </div>
            </section>
          );
        })}
      </nav>

      <div className="session-box">
        <strong>{usuario?.nombreCompleto}</strong>
        <span>{etiquetaRol[usuario?.rol] || usuario?.rol}</span>
        <Button
          variant="secondary"
          onClick={() => {
            logout();
            navigate('/login', { replace: true });
          }}
        >
          Cerrar sesión
        </Button>
      </div>
    </aside>
  );
}

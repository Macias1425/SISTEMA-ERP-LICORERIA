import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';

import { Icono } from './Icono';

/** Centro de avisos: resume qué necesita atención y lleva al módulo que lo resuelve. */
export default function AlertasMenu({ resumen, cargando, onRefrescar }) {
  const [abierto, setAbierto] = useState(false);
  const contenedor = useRef(null);

  const secciones = resumen?.secciones || [];
  const total = resumen?.total || 0;
  const criticas = resumen?.criticas || 0;

  useEffect(() => {
    if (!abierto) return undefined;

    function alClicFuera(evento) {
      if (!contenedor.current?.contains(evento.target)) setAbierto(false);
    }
    function alEscape(evento) {
      if (evento.key === 'Escape') setAbierto(false);
    }
    document.addEventListener('mousedown', alClicFuera);
    document.addEventListener('keydown', alEscape);
    return () => {
      document.removeEventListener('mousedown', alClicFuera);
      document.removeEventListener('keydown', alEscape);
    };
  }, [abierto]);

  return (
    <div className="alertas-menu" ref={contenedor}>
      <button
        type="button"
        className={`alertas-trigger${criticas > 0 ? ' critica' : ''}${total > 0 ? ' con-avisos' : ''}`}
        onClick={() => setAbierto((valor) => !valor)}
        aria-expanded={abierto}
        aria-label={total > 0 ? `${total} alertas activas` : 'Sin alertas activas'}
      >
        <Icono nombre="campana" className="alertas-trigger-icon" />
        {total > 0 ? <span className="alertas-trigger-count">{total > 99 ? '99+' : total}</span> : null}
      </button>

      {abierto ? (
        <div className="alertas-panel card" role="dialog" aria-label="Centro de alertas">
          <header className="alertas-panel-head">
            <div>
              <strong>Alertas activas</strong>
              <small>
                {total === 0
                  ? 'Todo en orden por ahora'
                  : `${total} pendiente(s)${criticas > 0 ? ` · ${criticas} crítica(s)` : ''}`}
              </small>
            </div>
            <button type="button" className="btn link" onClick={onRefrescar} disabled={cargando}>
              {cargando ? 'Actualizando…' : 'Actualizar'}
            </button>
          </header>

          {secciones.length === 0 ? (
            <p className="placeholder">No hay avisos para su perfil.</p>
          ) : (
            <ul className="alertas-lista">
              {secciones.map((seccion) => (
                <li key={seccion.clave} className={`alerta-item ${seccion.nivel === 'CRITICA' ? 'critica' : 'aviso'}`}>
                  <Link to={seccion.ruta} onClick={() => setAbierto(false)}>
                    <span className="alerta-item-top">
                      <strong>{seccion.etiqueta}</strong>
                      <span className="alerta-item-cantidad">{seccion.cantidad}</span>
                    </span>
                    <small>{seccion.detalle}</small>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      ) : null}
    </div>
  );
}

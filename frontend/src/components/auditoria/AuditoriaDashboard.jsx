import { useMemo } from 'react';

const PALETA = ['#2563eb', '#2f8f5b', '#d97706', '#dc2626', '#7c5cbf', '#0f766e', '#64748b'];

export default function AuditoriaDashboard({ eventos }) {
  const porModulo = useMemo(() => {
    const mapa = {};
    (eventos || []).forEach((evento) => {
      const clave = evento.modulo || 'Sistema';
      mapa[clave] = (mapa[clave] || 0) + 1;
    });
    return Object.entries(mapa)
      .map(([nombre, total]) => ({ nombre, total }))
      .sort((a, b) => b.total - a.total);
  }, [eventos]);

  const porAccion = useMemo(() => {
    const mapa = {};
    (eventos || []).forEach((evento) => {
      const clave = evento.eventoEtiqueta || evento.accion || 'Otro';
      mapa[clave] = (mapa[clave] || 0) + 1;
    });
    return Object.entries(mapa)
      .map(([nombre, total]) => ({ nombre, total }))
      .sort((a, b) => b.total - a.total)
      .slice(0, 8);
  }, [eventos]);

  const porRiesgo = useMemo(() => {
    const mapa = { ALTO: 0, MEDIO: 0, BAJO: 0 };
    (eventos || []).forEach((evento) => {
      const clave = evento.nivelRiesgo || 'BAJO';
      mapa[clave] = (mapa[clave] || 0) + 1;
    });
    return [
      { nombre: 'Alto', total: mapa.ALTO, color: '#dc2626' },
      { nombre: 'Medio', total: mapa.MEDIO, color: '#d97706' },
      { nombre: 'Bajo', total: mapa.BAJO, color: '#2f8f5b' },
    ];
  }, [eventos]);

  const maxModulo = Math.max(1, ...porModulo.map((item) => item.total));
  const maxAccion = Math.max(1, ...porAccion.map((item) => item.total));
  const maxRiesgo = Math.max(1, ...porRiesgo.map((item) => item.total));

  return (
    <div className="aud-dashboard">
      <div className="aud-dash-grid">
        <article className="card aud-dash-card">
          <h3>Eventos por módulo</h3>
          {!porModulo.length ? <p className="placeholder">Sin datos en el filtro actual.</p> : (
            <ul className="aud-bar-list">
              {porModulo.map((item, indice) => (
                <li key={item.nombre}>
                  <div className="aud-bar-label">
                    <span>{item.nombre}</span>
                    <strong>{item.total}</strong>
                  </div>
                  <div className="aud-bar-track">
                    <span
                      className="aud-bar-fill"
                      style={{
                        width: `${(item.total / maxModulo) * 100}%`,
                        background: PALETA[indice % PALETA.length],
                      }}
                    />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </article>

        <article className="card aud-dash-card">
          <h3>Distribución de riesgo</h3>
          <ul className="aud-bar-list">
            {porRiesgo.map((item) => (
              <li key={item.nombre}>
                <div className="aud-bar-label">
                  <span>{item.nombre}</span>
                  <strong>{item.total}</strong>
                </div>
                <div className="aud-bar-track">
                  <span
                    className="aud-bar-fill"
                    style={{
                      width: `${(item.total / maxRiesgo) * 100}%`,
                      background: item.color,
                    }}
                  />
                </div>
              </li>
            ))}
          </ul>
        </article>
      </div>

      <article className="card aud-dash-card">
        <h3>Acciones más frecuentes</h3>
        {!porAccion.length ? <p className="placeholder">Sin datos en el filtro actual.</p> : (
          <ul className="aud-bar-list aud-bar-list-wide">
            {porAccion.map((item, indice) => (
              <li key={item.nombre}>
                <div className="aud-bar-label">
                  <span>{item.nombre}</span>
                  <strong>{item.total}</strong>
                </div>
                <div className="aud-bar-track">
                  <span
                    className="aud-bar-fill"
                    style={{
                      width: `${(item.total / maxAccion) * 100}%`,
                      background: PALETA[indice % PALETA.length],
                    }}
                  />
                </div>
              </li>
            ))}
          </ul>
        )}
      </article>
    </div>
  );
}

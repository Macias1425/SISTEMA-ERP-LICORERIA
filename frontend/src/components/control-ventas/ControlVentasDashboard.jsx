import { useMemo } from 'react';

import { moneda as formatoMoneda } from '../../utils/formato';

const PALETA = ['#2f8f5b', '#5c4636', '#3b82f6', '#d97706', '#7c5cbf'];
const TIPO_LABEL = {
  ALTO_MONTO: 'Alto monto',
  OVERRIDE_PRECIO: 'Cambio de precio',
  SUPERVISOR_AUTORIZADO: 'Autorización admin',
  ANULACION: 'Anulación',
};

export default function ControlVentasDashboard({ resumen, eventos }) {
  const porTipo = useMemo(() => {
    const mapa = {};
    (eventos || []).forEach((evento) => {
      const clave = TIPO_LABEL[evento.tipo] || evento.tipo;
      mapa[clave] = (mapa[clave] || 0) + 1;
    });
    return Object.entries(mapa)
      .map(([nombre, total]) => ({ nombre, total }))
      .sort((a, b) => b.total - a.total);
  }, [eventos]);

  const maxSerie = Math.max(1, ...(resumen?.serieDiaria || []).map((d) => Number(d.monto) || 0));
  const maxTipo = Math.max(1, ...porTipo.map((item) => item.total));

  return (
    <div className="cv-dashboard">
      <div className="cv-dash-grid">
        <article className="card cv-dash-card cv-dash-wide">
          <h3>Ventas por día</h3>
          <p className="cv-dash-lead">Monto vendido y alertas en el período seleccionado.</p>
          {!resumen?.serieDiaria?.length ? (
            <p className="placeholder">Sin ventas en el período.</p>
          ) : (
            <ul className="cv-bar-list">
              {resumen.serieDiaria.map((dia, indice) => (
                <li key={dia.fecha}>
                  <div className="cv-bar-label">
                    <span>{dia.fecha}</span>
                    <strong>{formatoMoneda(dia.monto)} · {dia.ventas} venta(s)</strong>
                  </div>
                  <div className="cv-bar-track">
                    <span
                      className="cv-bar-fill"
                      style={{
                        width: `${((Number(dia.monto) || 0) / maxSerie) * 100}%`,
                        background: PALETA[indice % PALETA.length],
                      }}
                    />
                  </div>
                  {dia.alertas > 0 ? (
                    <small className="cv-bar-alert">{dia.alertas} alerta(s)</small>
                  ) : null}
                </li>
              ))}
            </ul>
          )}
        </article>

        <article className="card cv-dash-card">
          <h3>Eventos por tipo</h3>
          {!porTipo.length ? (
            <p className="placeholder">Sin eventos en el período.</p>
          ) : (
            <ul className="cv-bar-list cv-bar-list-compact">
              {porTipo.map((item, indice) => (
                <li key={item.nombre}>
                  <div className="cv-bar-label">
                    <span>{item.nombre}</span>
                    <strong>{item.total}</strong>
                  </div>
                  <div className="cv-bar-track">
                    <span
                      className="cv-bar-fill"
                      style={{
                        width: `${(item.total / maxTipo) * 100}%`,
                        background: PALETA[indice % PALETA.length],
                      }}
                    />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </article>

        <article className="card cv-dash-card cv-dash-rules">
          <h3>Umbrales activos</h3>
          <ul className="cv-indicadores">
            <li>
              <span>Alerta desde</span>
              <strong>{formatoMoneda(resumen?.reglas?.montoAlertaVenta)}</strong>
            </li>
            <li>
              <span>Admin desde</span>
              <strong>{formatoMoneda(resumen?.reglas?.montoSupervisorRequerido)}</strong>
            </li>
            <li>
              <span>Ticket promedio</span>
              <strong>{formatoMoneda(resumen?.ticketPromedio)}</strong>
            </li>
            <li>
              <span>Tasa de alertas</span>
              <strong>{resumen?.tasaAlertasPct ?? 0}%</strong>
            </li>
            <li>
              <span>Límite por turno</span>
              <strong>
                {Number(resumen?.reglas?.maxVentasPorTurno) > 0
                  ? `${resumen.reglas.maxVentasPorTurno} ventas`
                  : 'Sin límite'}
              </strong>
            </li>
          </ul>
        </article>
      </div>
    </div>
  );
}

import { useMemo } from 'react';

function etiquetaDia(fecha) {
  if (!fecha) return '—';
  const [, mes, dia] = fecha.split('-');
  return `${dia}/${mes}`;
}

export default function FinanzasGraficos({ finanzas, dinero }) {
  const serie = finanzas?.serieDiaria || [];
  const maxSerie = useMemo(() => Math.max(
    1,
    ...serie.map((p) => Math.max(Number(p.ventas || 0), Number(p.compras || 0), Math.abs(Number(p.resultado || 0))))
  ), [serie]);

  const topMargen = useMemo(() => (finanzas?.margenProductos || []).slice(0, 8), [finanzas]);
  const maxMargen = Math.max(1, ...topMargen.map((p) => Number(p.margen || 0)));

  const flujo = finanzas?.flujo;
  const totalPagos = Number(flujo?.entradasEfectivo || 0) + Number(flujo?.entradasTarjeta || 0);
  const pctEfectivo = totalPagos > 0 ? (Number(flujo?.entradasEfectivo || 0) / totalPagos) * 100 : 50;

  return (
    <div className="fin-charts">
      <article className="card fin-chart-card">
        <h3>Tendencia diaria</h3>
        {!serie.length ? <p className="placeholder">Sin datos en el período.</p> : (
          <div className="fin-trend-chart" role="img" aria-label="Gráfico de ventas y compras por día">
            {serie.map((punto) => (
              <div key={punto.fecha} className="fin-trend-col">
                <div className="fin-trend-bars">
                  <span
                    className="fin-bar fin-bar-ventas"
                    style={{ height: `${(Number(punto.ventas || 0) / maxSerie) * 100}%` }}
                    title={`Ventas ${dinero(punto.ventas)}`}
                  />
                  <span
                    className="fin-bar fin-bar-compras"
                    style={{ height: `${(Number(punto.compras || 0) / maxSerie) * 100}%` }}
                    title={`Compras ${dinero(punto.compras)}`}
                  />
                </div>
                <small>{etiquetaDia(punto.fecha)}</small>
              </div>
            ))}
          </div>
        )}
        <div className="fin-legend">
          <span><i className="fin-dot fin-dot-ventas" /> Ventas</span>
          <span><i className="fin-dot fin-dot-compras" /> Compras</span>
        </div>
      </article>

      <article className="card fin-chart-card">
        <h3>Mix de cobros</h3>
        <div className="fin-donut-wrap">
          <div
            className="fin-donut"
            style={{ background: `conic-gradient(#2f8f5b 0 ${pctEfectivo}%, #3b82f6 ${pctEfectivo}% 100%)` }}
          />
          <div className="fin-donut-center">
            <strong>{dinero(finanzas?.ventasTotal)}</strong>
            <small>Total ventas</small>
          </div>
        </div>
        <ul className="fin-pay-list">
          <li><span>Efectivo</span><strong>{dinero(flujo?.entradasEfectivo)}</strong></li>
          <li><span>Tarjeta</span><strong>{dinero(flujo?.entradasTarjeta)}</strong></li>
          <li><span>IVA cobrado</span><strong>{dinero(flujo?.ivaCobrado)}</strong></li>
        </ul>
      </article>

      <article className="card fin-chart-card fin-chart-wide">
        <h3>Top productos por margen</h3>
        {!topMargen.length ? <p className="placeholder">Sin ventas con margen calculable.</p> : (
          <ul className="aud-bar-list">
            {topMargen.map((producto, indice) => (
              <li key={producto.productoId || producto.codigo}>
                <div className="aud-bar-label">
                  <span>{producto.codigo} · {producto.nombre}</span>
                  <strong>{dinero(producto.margen)} ({Number(producto.margenPct || 0).toFixed(1)}%)</strong>
                </div>
                <div className="aud-bar-track">
                  <span
                    className="aud-bar-fill"
                    style={{
                      width: `${(Number(producto.margen || 0) / maxMargen) * 100}%`,
                      background: ['#2563eb', '#2f8f5b', '#7c5cbf', '#d97706'][indice % 4],
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

import { useMemo } from 'react';

const PALETA_PAGO = {
  EFECTIVO: '#2f8f5b',
  TARJETA: '#3b82f6',
};

const PALETA_PRODUCTOS = ['#2f8f5b', '#3b82f6', '#7c5cbf', '#d97706', '#0f766e', '#64748b'];

function claveFecha(valor) {
  if (!valor) return '';
  const fecha = new Date(valor);
  return [
    fecha.getFullYear(),
    String(fecha.getMonth() + 1).padStart(2, '0'),
    String(fecha.getDate()).padStart(2, '0'),
  ].join('-');
}

function diasEntre(desde, hasta) {
  const inicio = new Date(`${desde}T00:00:00`);
  const fin = new Date(`${hasta}T00:00:00`);
  const dias = [];
  const cursor = new Date(inicio);
  while (cursor <= fin) {
    dias.push([
      cursor.getFullYear(),
      String(cursor.getMonth() + 1).padStart(2, '0'),
      String(cursor.getDate()).padStart(2, '0'),
    ].join('-'));
    cursor.setDate(cursor.getDate() + 1);
  }
  return dias;
}

function gradienteConico(segmentos) {
  if (!segmentos.length) {
    return 'conic-gradient(#d4ddd7 0 100%)';
  }
  return `conic-gradient(${segmentos.reduce((acc, item, indice) => {
    const inicio = segmentos.slice(0, indice).reduce((suma, actual) => suma + actual.pct, 0);
    const fin = inicio + item.pct;
    const parte = `${item.color} ${inicio}% ${fin}%`;
    return acc ? `${acc}, ${parte}` : parte;
  }, '')})`;
}

function etiquetaDia(clave) {
  const [, mes, dia] = clave.split('-');
  return `${dia}/${mes}`;
}

export default function ReporteGraficos({ reporte, dinero }) {
  const barras = useMemo(() => {
    if (!reporte?.desde || !reporte?.hasta) {
      return [];
    }
    const ventas = reporte.ventas || [];
    const dias = diasEntre(reporte.desde, reporte.hasta);
    const limite = dias.length > 14 ? Math.ceil(dias.length / 7) : 1;

    if (limite > 1) {
      const bloques = [];
      for (let i = 0; i < dias.length; i += 7) {
        const grupo = dias.slice(i, i + 7);
        const total = ventas
          .filter((venta) => grupo.includes(claveFecha(venta.fecha)))
          .reduce((suma, venta) => suma + Number(venta.total || 0), 0);
        bloques.push({
          clave: `${grupo[0]}-${grupo[grupo.length - 1]}`,
          etiqueta: `${etiquetaDia(grupo[0])}–${etiquetaDia(grupo[grupo.length - 1])}`,
          total,
        });
      }
      return bloques;
    }

    return dias.map((dia) => ({
      clave: dia,
      etiqueta: etiquetaDia(dia),
      total: ventas
        .filter((venta) => claveFecha(venta.fecha) === dia)
        .reduce((suma, venta) => suma + Number(venta.total || 0), 0),
    }));
  }, [reporte]);

  const maxBarra = Math.max(1, ...barras.map((item) => item.total));

  const paletaPago = useMemo(() => {
    const efectivo = Number(reporte?.ventasEfectivoTotal || 0);
    const tarjeta = Number(reporte?.ventasTarjetaTotal || 0);
    const total = efectivo + tarjeta;
    const segmentos = [];
    if (efectivo > 0) {
      segmentos.push({
        nombre: 'Efectivo',
        valor: efectivo,
        pct: total ? (efectivo / total) * 100 : 0,
        color: PALETA_PAGO.EFECTIVO,
      });
    }
    if (tarjeta > 0) {
      segmentos.push({
        nombre: 'Tarjeta',
        valor: tarjeta,
        pct: total ? (tarjeta / total) * 100 : 0,
        color: PALETA_PAGO.TARJETA,
      });
    }
    return segmentos;
  }, [reporte]);

  const queques = useMemo(() => {
    const top = [...(reporte?.productos || [])]
      .sort((a, b) => Number(b.total || 0) - Number(a.total || 0))
      .slice(0, 5);
    const total = top.reduce((suma, item) => suma + Number(item.total || 0), 0);
    return top.map((item, indice) => ({
      nombre: item.nombre,
      codigo: item.codigo,
      valor: Number(item.total || 0),
      pct: total ? (Number(item.total || 0) / total) * 100 : 0,
      color: PALETA_PRODUCTOS[indice % PALETA_PRODUCTOS.length],
    }));
  }, [reporte]);

  const hayDatos = barras.some((item) => item.total > 0)
    || paletaPago.length > 0
    || queques.length > 0;

  if (!hayDatos) {
    return (
      <article className="card rep-graficos">
        <h3>Gráficos del período</h3>
        <p className="placeholder">No hay datos suficientes para generar barras, paletas o queques en este rango.</p>
      </article>
    );
  }

  return (
    <div className="chart-grid rep-graficos">
      <article className="card chart-card">
        <div className="chart-head">
          <div>
            <h3>Barras de ventas</h3>
            <p className="kpi-meta">
              {barras.length > 7 ? 'Totales semanales' : 'Ventas por día'} · {reporte.desde} a {reporte.hasta}
            </p>
          </div>
          <div className="chart-gain">
            <span>Total período</span>
            <strong>{dinero(reporte.ventasTotal)}</strong>
          </div>
        </div>
        <div
          className="bar-chart"
          style={{ gridTemplateColumns: `repeat(${Math.min(barras.length, 14)}, 1fr)` }}
        >
          {barras.map((item) => (
            <div key={item.clave} className="bar-col" title={`${item.etiqueta}: ${dinero(item.total)}`}>
              <div className="bar-track">
                <div
                  className="bar-fill"
                  style={{ height: `${(item.total / maxBarra) * 100}%` }}
                />
              </div>
              <span>{item.etiqueta}</span>
            </div>
          ))}
        </div>
      </article>

      <article className="card chart-card">
        <h3>Paleta de pagos</h3>
        <p className="kpi-meta">Participación efectivo vs tarjeta.</p>
        <div className="donut-wrap">
          <div className="donut" style={{ background: gradienteConico(paletaPago) }}>
            <div className="donut-hole">
              <strong>{paletaPago.length}</strong>
              <small>forma(s)</small>
            </div>
          </div>
          <ul className="donut-legend rep-paleta">
            {paletaPago.length ? paletaPago.map((item) => (
              <li key={item.nombre}>
                <i style={{ background: item.color }} />
                <span>{item.nombre}</span>
                <strong>{dinero(item.valor)}</strong>
              </li>
            )) : <li>Sin ventas por forma de pago.</li>}
          </ul>
        </div>
      </article>

      <article className="card chart-card rep-queques">
        <h3>Queques de productos</h3>
        <p className="kpi-meta">Top 5 productos por monto vendido.</p>
        <div className="donut-wrap">
          <div className="donut" style={{ background: gradienteConico(queques) }}>
            <div className="donut-hole">
              <strong>{queques.length}</strong>
              <small>top</small>
            </div>
          </div>
          <ul className="donut-legend rep-paleta">
            {queques.length ? queques.map((item) => (
              <li key={item.codigo || item.nombre}>
                <i style={{ background: item.color }} />
                <span>{item.nombre}</span>
                <strong>{dinero(item.valor)}</strong>
              </li>
            )) : <li>Sin productos vendidos.</li>}
          </ul>
        </div>
      </article>
    </div>
  );
}

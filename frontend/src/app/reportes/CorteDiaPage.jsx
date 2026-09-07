import { useEffect, useState } from 'react';
import Button from '../../components/ui/Button';
import Icon from '../../components/ui/Icon';
import { mensajeError } from '../../auth/AuthContext';
import { cargarCorteDia, descargarCortePdf } from '../../services/corteDiaService';
import { dinero } from '../../utils/formato';

function hoyLocal() {
  const d = new Date();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const dia = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${m}-${dia}`;
}

function textoWhatsApp(corte) {
  const lineas = [
    `*${corte.nombreNegocio || 'Licorería'}*`,
    `Corte del día ${corte.fecha}`,
    '',
    `Ventas: ${corte.ventasCantidad || 0} tickets · ${dinero(corte.ventasTotal)}`,
    `Efectivo: ${dinero(corte.ventasEfectivo)}`,
    `Tarjeta: ${dinero(corte.ventasTarjeta)}`,
    `Stripe: ${dinero(corte.ventasStripe)}`,
    Number(corte.ventasCredito) > 0 ? `Crédito: ${dinero(corte.ventasCredito)}` : null,
    `Compras: ${dinero(corte.comprasTotal)}`,
    `Resultado: ${dinero(corte.resultadoDia)}`,
    `Turnos abiertos: ${corte.turnosAbiertos || 0}`,
  ].filter(Boolean);
  if (corte.topProductos?.length) {
    lineas.push('', 'Top productos:');
    corte.topProductos.slice(0, 5).forEach((p, i) => {
      lineas.push(`${i + 1}. ${p.nombre} · ${dinero(p.total)}`);
    });
  }
  return lineas.join('\n');
}

export default function CorteDiaPage() {
  const [fecha, setFecha] = useState(hoyLocal());
  const [corte, setCorte] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [descargando, setDescargando] = useState(false);
  const [error, setError] = useState('');

  async function cargar(fechaDestino = fecha) {
    setCargando(true);
    setError('');
    try {
      const data = await cargarCorteDia(fechaDestino);
      setCorte(data);
    } catch (err) {
      setCorte(null);
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar(fecha);
  }, [fecha]);

  async function onPdf() {
    setDescargando(true);
    setError('');
    try {
      await descargarCortePdf(fecha);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setDescargando(false);
    }
  }

  function onWhatsApp() {
    if (!corte) return;
    const url = `https://wa.me/?text=${encodeURIComponent(textoWhatsApp(corte))}`;
    window.open(url, '_blank', 'noopener,noreferrer');
  }

  return (
    <section className="page-shell corte-page">
      <header className="page-header">
        <div>
          <p className="page-kicker">Reportes</p>
          <h1>Corte del día</h1>
          <p>Resumen para el dueño: ventas, pagos, top productos y turnos. Descarga PDF o comparte por WhatsApp.</p>
        </div>
        <div className="corte-actions">
          <label className="corte-fecha">
            <span>Fecha</span>
            <input type="date" value={fecha} max={hoyLocal()} onChange={(e) => setFecha(e.target.value)} />
          </label>
          <Button type="button" variant="secondary" onClick={() => cargar(fecha)} disabled={cargando}>
            Actualizar
          </Button>
          <Button type="button" onClick={onPdf} disabled={!corte || descargando}>
            <Icon name="receipt" size={16} strokeWidth={2} />
            {descargando ? 'Generando…' : 'Descargar PDF'}
          </Button>
          <Button type="button" variant="secondary" onClick={onWhatsApp} disabled={!corte}>
            WhatsApp
          </Button>
        </div>
      </header>

      {error ? <p className="pos-alert" role="alert">{error}</p> : null}
      {cargando && !corte ? <p className="placeholder">Cargando corte…</p> : null}

      {corte ? (
        <div className="corte-body">
          <div className="corte-kpis">
            <article className="corte-kpi">
              <span>Ventas</span>
              <strong>{dinero(corte.ventasTotal)}</strong>
              <small>{corte.ventasCantidad || 0} tickets · prom. {dinero(corte.ticketPromedio)}</small>
            </article>
            <article className="corte-kpi">
              <span>Efectivo</span>
              <strong>{dinero(corte.ventasEfectivo)}</strong>
            </article>
            <article className="corte-kpi">
              <span>Tarjeta</span>
              <strong>{dinero(corte.ventasTarjeta)}</strong>
            </article>
            <article className="corte-kpi">
              <span>Stripe</span>
              <strong>{dinero(corte.ventasStripe)}</strong>
            </article>
            <article className="corte-kpi">
              <span>Resultado</span>
              <strong>{dinero(corte.resultadoDia)}</strong>
              <small>Ventas − compras ({dinero(corte.comprasTotal)})</small>
            </article>
          </div>

          <div className="corte-grid">
            <section className="corte-card">
              <h2>Top productos</h2>
              {corte.topProductos?.length ? (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Producto</th>
                      <th>UMM</th>
                      <th>Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {corte.topProductos.map((p) => (
                      <tr key={p.productoId || p.codigo}>
                        <td>
                          <strong>{p.nombre}</strong>
                          <div className="muted">{p.codigo}</div>
                        </td>
                        <td>{p.cantidadUmm}</td>
                        <td>{dinero(p.total)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                <p className="placeholder">Sin productos vendidos en esta fecha.</p>
              )}
            </section>

            <section className="corte-card">
              <h2>Turnos de caja</h2>
              <p className="muted">
                {corte.turnosCantidad || 0} turno(s) · {corte.turnosAbiertos || 0} abierto(s)
              </p>
              {corte.turnos?.length ? (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Cajero</th>
                      <th>Estado</th>
                      <th>Ventas</th>
                      <th>Efectivo</th>
                    </tr>
                  </thead>
                  <tbody>
                    {corte.turnos.map((t) => (
                      <tr key={t.id || `${t.usuarioId}-${t.apertura}`}>
                        <td>{t.usuarioNombre || '—'}</td>
                        <td>{t.estado || '—'}</td>
                        <td>{dinero(t.totalVentas)}</td>
                        <td>{dinero(t.ventasEfectivo)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                <p className="placeholder">Sin turnos registrados.</p>
              )}
            </section>
          </div>
        </div>
      ) : null}
    </section>
  );
}

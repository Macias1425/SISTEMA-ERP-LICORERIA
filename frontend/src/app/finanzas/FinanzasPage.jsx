import { useEffect, useMemo, useState } from 'react';
import { mensajeError } from '../../auth/AuthContext';
import { CatalogFilterSelect, CatalogTabs } from '../../components/catalogo/CatalogToolbar';
import FinanzasGraficos from '../../components/finanzas/FinanzasGraficos';
import MargenRiesgoPanel from '../../components/finanzas/MargenRiesgoPanel';
import ReporteGraficos from '../../components/reportes/ReporteGraficos';
import ReportePrintHeader from '../../components/reportes/ReportePrintHeader';
import Button from '../../components/ui/Button';
import { finanzasService } from '../../services/finanzasService';
import { descargarCsv, archivoDocumento, textoCelda } from '../../utils/exportCsv';
import { dinero } from '../../utils/formato';

const TABS = [
  { id: 'resumen', label: 'Resumen ejecutivo' },
  { id: 'flujo', label: 'Flujo de caja' },
  { id: 'margen', label: 'Margen y utilidad' },
  { id: 'riesgo', label: 'Precios en riesgo' },
  { id: 'tendencias', label: 'Tendencias' },
];

const FILTROS_RAPIDOS = [
  ['hoy', 'Hoy'],
  ['semana', 'Esta semana'],
  ['mes', 'Este mes'],
];

function hoyLocal() {
  const ahora = new Date();
  return [
    ahora.getFullYear(),
    String(ahora.getMonth() + 1).padStart(2, '0'),
    String(ahora.getDate()).padStart(2, '0'),
  ].join('-');
}

function inicioSemana() {
  const ahora = new Date();
  const dia = ahora.getDay();
  const diff = dia === 0 ? 6 : dia - 1;
  ahora.setDate(ahora.getDate() - diff);
  return [
    ahora.getFullYear(),
    String(ahora.getMonth() + 1).padStart(2, '0'),
    String(ahora.getDate()).padStart(2, '0'),
  ].join('-');
}

function inicioMes() {
  const ahora = new Date();
  return `${ahora.getFullYear()}-${String(ahora.getMonth() + 1).padStart(2, '0')}-01`;
}

function pct(valor) {
  const n = Number(valor || 0);
  const signo = n > 0 ? '+' : '';
  return `${signo}${n.toFixed(1)}%`;
}

function chipVariacion(valor) {
  const n = Number(valor || 0);
  if (n > 0) return 'fin-var fin-var-up';
  if (n < 0) return 'fin-var fin-var-down';
  return 'fin-var';
}

function rangoRapido(tipo) {
  if (tipo === 'hoy') return { desde: hoyLocal(), hasta: hoyLocal() };
  if (tipo === 'semana') return { desde: inicioSemana(), hasta: hoyLocal() };
  return { desde: inicioMes(), hasta: hoyLocal() };
}

export default function FinanzasPage() {
  const [tab, setTab] = useState('resumen');
  const [desde, setDesde] = useState(inicioMes());
  const [hasta, setHasta] = useState(hoyLocal());
  const [formaPago, setFormaPago] = useState('');
  const [rapido, setRapido] = useState('mes');
  const [finanzas, setFinanzas] = useState(null);
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(true);

  async function cargar(params = {}) {
    setCargando(true);
    setError('');
    try {
      setFinanzas(await finanzasService.periodo({
        desde: params.desde ?? desde,
        hasta: params.hasta ?? hasta,
        formaPago: (params.formaPago ?? formaPago) || undefined,
      }));
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar({ desde: inicioMes(), hasta: hoyLocal(), formaPago: '' });
  }, []);

  const reporteAdaptado = useMemo(() => {
    if (!finanzas) return null;
    return {
      ...finanzas,
      productos: (finanzas.margenProductos || []).map((p) => ({
        codigo: p.codigo,
        nombre: p.nombre,
        cantidadUmm: p.cantidadUmm,
        total: p.ingresos,
        tickets: 0,
      })),
    };
  }, [finanzas]);

  const comp = finanzas?.comparativa;
  const flujo = finanzas?.flujo;
  const resultado = Number(finanzas?.resultado || 0);

  function aplicarRapido(tipo) {
    const rango = rangoRapido(tipo);
    setRapido(tipo);
    setDesde(rango.desde);
    setHasta(rango.hasta);
    cargar({ ...rango });
  }

  function exportarMargenCsv() {
    descargarCsv(archivoDocumento('Margen bruto por producto', { desde, hasta }), [
      { key: 'codigo', label: 'Código de producto', format: (f) => textoCelda(f.codigo) },
      { key: 'nombre', label: 'Producto', format: (f) => textoCelda(f.nombre) },
      { key: 'cantidadUmm', label: 'Unidades vendidas (UMM)' },
      { key: 'ingresos', label: 'Ingresos (C$)', format: (f) => dinero(f.ingresos) },
      { key: 'costo', label: 'Costo de mercancía vendida (C$)', format: (f) => dinero(f.costo) },
      { key: 'margen', label: 'Margen bruto (C$)', format: (f) => dinero(f.margen) },
      { key: 'margenPct', label: 'Margen bruto (%)', format: (f) => `${Number(f.margenPct || 0).toFixed(2)} %` },
    ], finanzas?.margenProductos || [], {
      titulo: 'Margen bruto por producto vendido',
      subtitulo: 'Ingresos menos costo de mercancía. No incluye gastos de operación.',
      desde,
      hasta,
      filtros: formaPago
        ? `Forma de pago: ${
          formaPago === 'EFECTIVO' ? 'Efectivo'
            : formaPago === 'STRIPE' ? 'Stripe'
              : formaPago === 'CREDITO' ? 'Crédito'
                : 'Tarjeta'
        }`
        : 'Todas las formas de pago',
    });
  }

  return (
    <section className="fin-page rep-page">
      <header className="cfg-topbar fin-topbar">
        <div>
          <p className="aud-kicker">Control financiero</p>
          <h1>Finanzas</h1>
          <p className="cfg-subtitle">
            Resumen económico del negocio: ventas, compras, margen bruto, flujo de caja y comparativa con el período anterior.
          </p>
        </div>
        <div className="cfg-topbar-actions">
          <Button type="button" variant="secondary" onClick={() => window.print()}>Exportar PDF</Button>
          <Button type="button" variant="secondary" onClick={exportarMargenCsv} disabled={!finanzas?.margenProductos?.length}>
            Exportar CSV
          </Button>
          <Button type="button" onClick={() => cargar()} disabled={cargando}>
            {cargando ? 'Actualizando…' : 'Actualizar'}
          </Button>
        </div>
      </header>

      {error ? <p className="auth-error" role="alert">{error}</p> : null}

      <article className="card fin-filters-card">
        <form
          className="form-actions"
          style={{ flexWrap: 'wrap', gap: '0.75rem', marginBottom: '0.75rem' }}
          onSubmit={(e) => { e.preventDefault(); setRapido(''); cargar(); }}
        >
          <label>
            Desde
            <input type="date" className="search-input" value={desde} onChange={(e) => setDesde(e.target.value)} />
          </label>
          <label>
            Hasta
            <input type="date" className="search-input" value={hasta} onChange={(e) => setHasta(e.target.value)} />
          </label>
          <CatalogFilterSelect value={formaPago} onChange={setFormaPago} ariaLabel="Forma de pago">
            <option value="">Pago: todos</option>
            <option value="EFECTIVO">Efectivo</option>
            <option value="TARJETA">Tarjeta</option>
            <option value="STRIPE">Stripe</option>
            <option value="CREDITO">Crédito</option>
          </CatalogFilterSelect>
          <Button type="submit" disabled={cargando}>Consultar</Button>
        </form>
        <div className="aud-quick-filters">
          <span>Período rápido</span>
          {FILTROS_RAPIDOS.map(([id, label]) => (
            <button
              key={id}
              type="button"
              className={`aud-quick-pill${rapido === id ? ' active' : ''}`}
              onClick={() => aplicarRapido(id)}
            >
              {label}
            </button>
          ))}
        </div>
      </article>

      {cargando ? <p className="placeholder">Cargando finanzas…</p> : null}

      {finanzas && !cargando ? (
        <>
          <div className="cat-kpi-grid fin-kpi-grid">
            <article className="cat-kpi">
              <span>Ventas netas</span>
              <strong>{dinero(finanzas.ventasTotal)}</strong>
              <small>{finanzas.ventasCantidad} venta(s) · ticket {dinero(finanzas.ticketPromedio)}</small>
            </article>
            <article className="cat-kpi">
              <span>Margen bruto</span>
              <strong>{dinero(finanzas.margenBruto)}</strong>
              <small>{Number(finanzas.margenBrutoPct || 0).toFixed(1)}% sobre ingresos</small>
            </article>
            <article className="cat-kpi">
              <span>Compras</span>
              <strong>{dinero(finanzas.comprasTotal)}</strong>
              <small>{finanzas.comprasCantidad} compra(s)</small>
            </article>
            <article className="cat-kpi">
              <span>Resultado</span>
              <strong className={resultado < 0 ? 'kpi-neg' : ''}>{dinero(finanzas.resultado)}</strong>
              <small>Ventas − compras del período</small>
            </article>
            <article className="cat-kpi">
              <span>IVA cobrado</span>
              <strong>{dinero(finanzas.ventasImpuesto)}</strong>
              <small>Subtotal {dinero(finanzas.ventasSubtotal)}</small>
            </article>
          </div>

          {comp ? (
            <div className="fin-compare-grid">
              {[
                ['Ventas', comp.ventasActual, comp.ventasVariacionPct],
                ['Compras', comp.comprasActual, comp.comprasVariacionPct],
                ['Resultado', comp.resultadoActual, comp.resultadoVariacionPct],
                ['Margen bruto', comp.margenActual, comp.margenVariacionPct],
              ].map(([titulo, monto, variacion]) => (
                <article key={titulo} className="card fin-compare-card">
                  <span>{titulo}</span>
                  <strong>{dinero(monto)}</strong>
                  <em className={chipVariacion(variacion)}>{pct(variacion)} vs período anterior</em>
                </article>
              ))}
            </div>
          ) : null}

          <CatalogTabs tabs={TABS} active={tab} onChange={setTab} />

          <div className="rep-print-area">
            <ReportePrintHeader
              tipo="Reporte financiero"
              titulo="Resumen financiero del período"
              subtitulo="Ventas, compras, margen bruto y flujo de caja. El resultado es ventas menos compras, no utilidad neta."
              desde={finanzas.desde}
              hasta={finanzas.hasta}
            />

            {tab === 'resumen' ? (
              <>
                <FinanzasGraficos finanzas={finanzas} dinero={dinero} />
                {reporteAdaptado ? <ReporteGraficos reporte={reporteAdaptado} dinero={dinero} /> : null}
              </>
            ) : null}

            {tab === 'flujo' ? (
              <div className="fin-flow-grid">
                <article className="card fin-flow-card fin-flow-in">
                  <h3>Entradas</h3>
                  <ul>
                    <li><span>Ventas totales</span><strong>{dinero(flujo?.entradasVentas)}</strong></li>
                    <li><span>Efectivo</span><strong>{dinero(flujo?.entradasEfectivo)}</strong></li>
                    <li><span>Tarjeta</span><strong>{dinero(flujo?.entradasTarjeta)}</strong></li>
                    <li><span>IVA cobrado</span><strong>{dinero(flujo?.ivaCobrado)}</strong></li>
                  </ul>
                </article>
                <article className="card fin-flow-card fin-flow-out">
                  <h3>Salidas</h3>
                  <ul>
                    <li><span>Compras a proveedores</span><strong>{dinero(flujo?.salidasCompras)}</strong></li>
                  </ul>
                </article>
                <article className="card fin-flow-card fin-flow-net">
                  <h3>Flujo neto</h3>
                  <strong className={Number(flujo?.flujoNeto || 0) < 0 ? 'kpi-neg' : ''}>{dinero(flujo?.flujoNeto)}</strong>
                  <p className="hint">Ventas totales menos compras del período.</p>
                </article>
                <article className="card fin-flow-card">
                  <h3>Arqueos de caja</h3>
                  <ul>
                    <li><span>Turnos cuadrados</span><strong>{flujo?.turnosCuadrados ?? 0}</strong></li>
                    <li><span>Con diferencia</span><strong>{flujo?.turnosConDiferencia ?? 0}</strong></li>
                    <li><span>Faltante acumulado</span><strong>{dinero(flujo?.faltanteCaja)}</strong></li>
                    <li><span>Sobrante acumulado</span><strong>{dinero(flujo?.sobranteCaja)}</strong></li>
                  </ul>
                </article>
              </div>
            ) : null}

            {tab === 'margen' ? (
              <article className="card fin-table-card">
                <header className="aud-table-header">
                  <h3>Margen por producto</h3>
                  <span>Costo = precio compra × botellas vendidas</span>
                </header>
                <div className="aud-table-wrap">
                  <table className="data-table aud-table">
                    <thead>
                      <tr>
                        <th>Código</th>
                        <th>Producto</th>
                        <th>Botellas</th>
                        <th>Ingresos</th>
                        <th>Costo</th>
                        <th>Margen</th>
                        <th>%</th>
                      </tr>
                    </thead>
                    <tbody>
                      {(finanzas.margenProductos || []).map((p) => (
                        <tr key={p.productoId || p.codigo}>
                          <td>{p.codigo}</td>
                          <td>{p.nombre}</td>
                          <td>{p.cantidadUmm}</td>
                          <td>{dinero(p.ingresos)}</td>
                          <td>{dinero(p.costo)}</td>
                          <td>{dinero(p.margen)}</td>
                          <td>{Number(p.margenPct || 0).toFixed(1)}%</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                {!finanzas.margenProductos?.length ? (
                  <p className="placeholder">No hay ventas en el período para calcular margen.</p>
                ) : null}
              </article>
            ) : null}

            {tab === 'riesgo' ? <MargenRiesgoPanel /> : null}

            {tab === 'tendencias' ? (
              <>
                <FinanzasGraficos finanzas={finanzas} dinero={dinero} />
                <article className="card fin-table-card">
                  <h3>Serie diaria</h3>
                  <div className="aud-table-wrap">
                    <table className="data-table aud-table">
                      <thead>
                        <tr>
                          <th>Fecha</th>
                          <th>Ventas</th>
                          <th>Compras</th>
                          <th>Resultado</th>
                          <th>Margen bruto</th>
                        </tr>
                      </thead>
                      <tbody>
                        {(finanzas.serieDiaria || []).map((dia) => (
                          <tr key={dia.fecha}>
                            <td>{dia.fecha}</td>
                            <td>{dinero(dia.ventas)}</td>
                            <td>{dinero(dia.compras)}</td>
                            <td>{dinero(dia.resultado)}</td>
                            <td>{dinero(dia.margenBruto)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </article>
              </>
            ) : null}
          </div>
        </>
      ) : null}
    </section>
  );
}

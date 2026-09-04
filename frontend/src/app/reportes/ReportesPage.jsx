import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { mensajeError } from '../../auth/AuthContext';
import CatalogToolbar, { CatalogFilterSelect, CatalogTabs } from '../../components/catalogo/CatalogToolbar';
import FacturaEstadoChip from '../../components/facturas/FacturaEstadoChip';
import ReporteGraficos from '../../components/reportes/ReporteGraficos';
import ReportePrintHeader from '../../components/reportes/ReportePrintHeader';
import Button from '../../components/ui/Button';
import Pagination from '../../components/ui/Pagination';
import { reporteService } from '../../services/reporteService';
import { descargarCsv, archivoDocumento, textoCelda } from '../../utils/exportCsv';
import { dinero } from '../../utils/formato';
import { TAMANO_PAGINA_DEFAULT } from '../../utils/paginaUtil';
import VencimientosPanel from './VencimientosPanel';

const TABS = [
  ['finanzas', 'Finanzas'],
  ['ventas', 'Ventas'],
  ['productos', 'Productos'],
  ['compras', 'Compras'],
  ['facturas', 'Facturas'],
  ['caja', 'Caja'],
  ['vencimientos', 'Vencimientos'],
];

const TITULOS_TAB = {
  finanzas: 'Resumen financiero del período',
  ventas: 'Detalle de ventas del período',
  productos: 'Productos vendidos en el período',
  compras: 'Compras recibidas en el período',
  facturas: 'Facturas emitidas en el período',
  caja: 'Turnos de caja del período',
};

function hoyLocal() {
  const ahora = new Date();
  return [
    ahora.getFullYear(),
    String(ahora.getMonth() + 1).padStart(2, '0'),
    String(ahora.getDate()).padStart(2, '0'),
  ].join('-');
}

function fechaHora(valor) {
  if (!valor) return '—';
  return new Date(valor).toLocaleString('es-NI');
}

function etiquetaPago(forma) {
  if (forma === 'EFECTIVO') return 'Efectivo';
  if (forma === 'TARJETA') return 'Tarjeta';
  return forma || '—';
}

function etiquetaArqueo(resultado) {
  if (resultado === 'FALTANTE') return 'Faltante';
  if (resultado === 'SOBRANTE') return 'Sobrante';
  if (resultado === 'CUADRADO') return 'Cuadrado';
  return resultado || 'Pendiente';
}

function chipArqueo(resultado) {
  if (resultado === 'FALTANTE') return 'cat-badge cat-badge-warn';
  if (resultado === 'SOBRANTE') return 'cat-badge cat-badge-info';
  if (resultado === 'CUADRADO') return 'cat-badge cat-badge-ok';
  return 'cat-badge';
}

function chipTurno(estado) {
  return estado === 'ABIERTO' ? 'cat-badge cat-badge-info' : 'cat-badge cat-badge-ok';
}

function columnasPorTab(tab) {
  if (tab === 'ventas') {
    return [
      { key: 'numero', label: 'Número de venta', format: (f) => textoCelda(f.numero) },
      { key: 'fecha', label: 'Fecha y hora', format: (f) => fechaHora(f.fecha) },
      { key: 'clienteNombre', label: 'Cliente', format: (f) => textoCelda(f.clienteNombre, 'Consumidor final') },
      { key: 'cajeroNombre', label: 'Cajero', format: (f) => textoCelda(f.cajeroNombre) },
      { key: 'formaPago', label: 'Forma de pago', format: (f) => etiquetaPago(f.formaPago) },
      { key: 'subtotal', label: 'Subtotal (C$)', format: (f) => dinero(f.subtotal) },
      { key: 'impuesto', label: 'Impuesto al valor agregado — IVA (C$)', format: (f) => dinero(f.impuesto) },
      { key: 'total', label: 'Total (C$)', format: (f) => dinero(f.total) },
    ];
  }
  if (tab === 'productos') {
    return [
      { key: 'codigo', label: 'Código de producto', format: (f) => textoCelda(f.codigo) },
      { key: 'nombre', label: 'Producto', format: (f) => textoCelda(f.nombre) },
      { key: 'cantidadUmm', label: 'Unidades vendidas (UMM)' },
      { key: 'tickets', label: 'Tickets en los que aparece' },
      { key: 'total', label: 'Importe vendido (C$)', format: (f) => dinero(f.total) },
    ];
  }
  if (tab === 'compras') {
    return [
      { key: 'numero', label: 'Número de compra', format: (f) => textoCelda(f.numero) },
      { key: 'fecha', label: 'Fecha de recepción', format: (f) => fechaHora(f.fecha) },
      { key: 'proveedorNombre', label: 'Proveedor', format: (f) => textoCelda(f.proveedorNombre) },
      { key: 'total', label: 'Total de la compra (C$)', format: (f) => dinero(f.total) },
    ];
  }
  if (tab === 'facturas') {
    return [
      { key: 'numero', label: 'Número de factura', format: (f) => textoCelda(f.numero) },
      { key: 'fechaEmision', label: 'Fecha de emisión', format: (f) => fechaHora(f.fechaEmision) },
      { key: 'clienteNombre', label: 'Cliente', format: (f) => textoCelda(f.clienteNombre, 'Consumidor final') },
      { key: 'cajeroNombre', label: 'Cajero', format: (f) => textoCelda(f.cajeroNombre) },
      { key: 'total', label: 'Total (C$)', format: (f) => dinero(f.total) },
      { key: 'estado', label: 'Estado', format: (f) => (f.estado === 'EMITIDA' ? 'Emitida' : f.estado === 'ANULADA' ? 'Anulada' : textoCelda(f.estado)) },
    ];
  }
  if (tab === 'caja') {
    return [
      { key: 'usuarioNombre', label: 'Cajero', format: (f) => textoCelda(f.usuarioNombre) },
      { key: 'fechaApertura', label: 'Fecha de apertura', format: (f) => fechaHora(f.fechaApertura) },
      { key: 'fechaCierre', label: 'Fecha de cierre', format: (f) => fechaHora(f.fechaCierre) },
      { key: 'montoInicial', label: 'Fondo inicial (C$)', format: (f) => dinero(f.montoInicial) },
      { key: 'totalVentas', label: 'Ventas del turno (C$)', format: (f) => dinero(f.totalVentas) },
      { key: 'ventasEfectivo', label: 'Ventas en efectivo (C$)', format: (f) => dinero(f.ventasEfectivo) },
      { key: 'diferencia', label: 'Diferencia de arqueo (C$)', format: (f) => dinero(f.diferencia) },
      { key: 'resultadoArqueo', label: 'Resultado del arqueo', format: (f) => etiquetaArqueo(f.resultadoArqueo) },
      { key: 'estado', label: 'Estado del turno', format: (f) => (f.estado === 'ABIERTO' ? 'Abierto' : 'Cerrado') },
    ];
  }
  return [];
}

function filasPorTab(tab, reporte) {
  if (!reporte) return [];
  if (tab === 'ventas') return reporte.ventas || [];
  if (tab === 'productos') return reporte.productos || [];
  if (tab === 'compras') return reporte.compras || [];
  if (tab === 'facturas') return reporte.facturas || [];
  if (tab === 'caja') return reporte.turnos || [];
  return [];
}

export default function ReportesPage() {
  const [params, setParams] = useSearchParams();
  const tab = TABS.some(([id]) => id === params.get('tab')) ? params.get('tab') : 'finanzas';
  const [desde, setDesde] = useState(hoyLocal());
  const [hasta, setHasta] = useState(hoyLocal());
  const [busqueda, setBusqueda] = useState('');
  const [formaPago, setFormaPago] = useState('');
  const [estadoFactura, setEstadoFactura] = useState('');
  const [reporte, setReporte] = useState(null);
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(true);
  const [pagina, setPagina] = useState(0);

  const filtrosActivos = useMemo(() => {
    let n = 0;
    if (busqueda.trim()) n += 1;
    if (formaPago && (tab === 'ventas' || tab === 'finanzas')) n += 1;
    if (estadoFactura && tab === 'facturas') n += 1;
    return n;
  }, [busqueda, formaPago, estadoFactura, tab]);

  const filtrosTexto = useMemo(() => [
    busqueda.trim() && `Búsqueda: ${busqueda.trim()}`,
    formaPago && (tab === 'ventas' || tab === 'finanzas') && `Forma de pago: ${etiquetaPago(formaPago)}`,
    estadoFactura && tab === 'facturas' && `Estado de factura: ${estadoFactura === 'EMITIDA' ? 'Emitida' : 'Anulada'}`,
  ].filter(Boolean).join(' · ') || 'Sin filtros adicionales', [busqueda, formaPago, estadoFactura, tab]);

  async function cargar(opciones = {}) {
    const rango = {
      desde: opciones.desde ?? desde,
      hasta: opciones.hasta ?? hasta,
    };
    const tabActual = opciones.tab ?? tab;
    const tabla = tabActual === 'finanzas' || tabActual === 'vencimientos' ? undefined : tabActual;
    const paginaDestino = tabla ? (opciones.pagina ?? 0) : 0;
    setCargando(true);
    setError('');
    try {
      setReporte(await reporteService.periodo({
        ...rango,
        busqueda: opciones.busqueda ?? busqueda,
        formaPago: (opciones.formaPago ?? formaPago) || undefined,
        estadoFactura: (opciones.estadoFactura ?? estadoFactura) || undefined,
        tabla,
        pagina: paginaDestino,
        tamano: opciones.tamano ?? TAMANO_PAGINA_DEFAULT,
      }));
      setPagina(paginaDestino);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar({ desde: hoyLocal(), hasta: hoyLocal(), busqueda: '', formaPago: '', estadoFactura: '' });
  }, []);

  const resultado = Number(reporte?.resultado || 0);
  const esPeriodo = tab !== 'vencimientos';

  function limpiarFiltros() {
    setBusqueda('');
    setFormaPago('');
    setEstadoFactura('');
    cargar({ busqueda: '', formaPago: '', estadoFactura: '' });
  }

  async function exportarCsv() {
    const columnas = columnasPorTab(tab);
    if (!columnas.length) return;
    const completo = await reporteService.periodo({
      desde,
      hasta,
      busqueda,
      formaPago: formaPago || undefined,
      estadoFactura: estadoFactura || undefined,
      tabla: tab,
      pagina: 0,
      tamano: 200,
    });
    const filas = filasPorTab(tab, completo);
    if (!filas.length) return;
    const nombre = archivoDocumento(TITULOS_TAB[tab] || 'Reporte operativo', {
      desde: completo.desde,
      hasta: completo.hasta,
    });
    descargarCsv(nombre, columnas, filas, {
      titulo: TITULOS_TAB[tab] || 'Reporte operativo',
      subtitulo: 'Corte operativo del período. El resultado es ventas menos compras, no utilidad neta.',
      desde: completo.desde,
      hasta: completo.hasta,
      filtros: filtrosTexto,
    });
  }

  return (
    <section className="rep-page">
      <header className="page-header no-print">
        <h1>Reportes operativos</h1>
        <p>Cortes por período, exportación CSV e impresión. El resultado es ventas menos compras, no utilidad neta.</p>
      </header>

      {error ? <p className="auth-error" role="alert">{error}</p> : null}

      <div className="no-print">
      <CatalogTabs
        tabs={TABS}
        active={tab}
        onChange={(id) => {
          setParams({ tab: id }, { replace: true });
          if (id !== 'vencimientos') {
            cargar({ tab: id, pagina: 0 });
          }
        }}
      />
      </div>

      {esPeriodo ? (
        <>
          {tab !== 'finanzas' ? (
          <div className="cat-kpi-grid no-print">
            <article className="cat-kpi">
              <span>Ventas</span>
              <strong>{dinero(reporte?.ventasTotal)}</strong>
              <small>{reporte?.ventasCantidad ?? 0} ticket(s) · IVA {dinero(reporte?.ventasImpuesto)}</small>
            </article>
            <article className="cat-kpi">
              <span>Compras</span>
              <strong>{dinero(reporte?.comprasTotal)}</strong>
              <small>{reporte?.comprasCantidad ?? 0} recepción(es)</small>
            </article>
            <article className="cat-kpi">
              <span>Resultado</span>
              <strong className={resultado < 0 ? 'kpi-neg' : ''}>{dinero(reporte?.resultado)}</strong>
              <small>{reporte ? `${reporte.desde} a ${reporte.hasta}` : 'Seleccione rango'}</small>
            </article>
            <article className="cat-kpi">
              <span>Facturación</span>
              <strong>{reporte?.facturasEmitidas ?? 0}</strong>
              <small>
                {dinero(reporte?.facturasTotalEmitido)} emitido
                {(reporte?.facturasAnuladas ?? 0) > 0 ? ` · ${reporte.facturasAnuladas} anulada(s)` : ''}
              </small>
            </article>
          </div>
          ) : null}

          <article className="card no-print" style={{ marginBottom: '1rem' }}>
            <form
              className="form-actions"
              style={{ alignItems: 'end', marginBottom: '0.85rem', flexWrap: 'wrap', gap: '0.75rem' }}
              onSubmit={(event) => {
                event.preventDefault();
                cargar();
              }}
            >
              <label>
                Desde
                <input type="date" className="search-input" value={desde} onChange={(e) => setDesde(e.target.value)} />
              </label>
              <label>
                Hasta
                <input type="date" className="search-input" value={hasta} onChange={(e) => setHasta(e.target.value)} />
              </label>
              <Button type="submit" disabled={cargando}>Consultar</Button>
              <Button type="button" variant="secondary" onClick={() => window.print()}>Imprimir</Button>
              {tab !== 'finanzas' ? (
                <Button
                  type="button"
                  variant="secondary"
                  disabled={!filasPorTab(tab, reporte).length}
                  onClick={exportarCsv}
                >
                  Exportar CSV
                </Button>
              ) : null}
            </form>

            {(tab === 'ventas' || tab === 'facturas' || tab === 'compras' || tab === 'productos' || tab === 'caja') ? (
              <CatalogToolbar
                searchValue={busqueda}
                onSearchChange={setBusqueda}
                onSubmit={() => cargar()}
                searchPlaceholder={
                  tab === 'productos'
                    ? 'Buscar por código o producto…'
                    : tab === 'compras'
                      ? 'Buscar por número o proveedor…'
                      : tab === 'caja'
                        ? 'Buscar por cajero…'
                        : 'Buscar por número, cliente o cajero…'
                }
                activeCount={filtrosActivos}
                onClearFilters={limpiarFiltros}
              >
                {tab === 'ventas' ? (
                  <CatalogFilterSelect value={formaPago} onChange={setFormaPago} ariaLabel="Forma de pago">
                    <option value="">Pago: todos</option>
                    <option value="EFECTIVO">Efectivo</option>
                    <option value="TARJETA">Tarjeta</option>
                  </CatalogFilterSelect>
                ) : null}
                {tab === 'facturas' ? (
                  <CatalogFilterSelect value={estadoFactura} onChange={setEstadoFactura} ariaLabel="Estado factura">
                    <option value="">Estado: todos</option>
                    <option value="EMITIDA">Emitidas</option>
                    <option value="ANULADA">Anuladas</option>
                  </CatalogFilterSelect>
                ) : null}
              </CatalogToolbar>
            ) : null}
          </article>
        </>
      ) : null}

      {tab === 'vencimientos' ? <VencimientosPanel /> : null}

      {esPeriodo && cargando ? <p className="placeholder">Cargando reporte…</p> : null}

      {esPeriodo && reporte && !cargando ? (
        <div className="dash-stack rep-print-area">
          <ReportePrintHeader
            tipo="Reporte operativo"
            titulo={TITULOS_TAB[tab] || 'Reporte operativo'}
            subtitulo="Corte del período. El resultado es ventas menos compras; no representa utilidad neta."
            desde={reporte.desde}
            hasta={reporte.hasta}
            registros={filasPorTab(tab, reporte).length}
            filtros={filtrosTexto}
          />

          {tab === 'finanzas' ? (
            <>
              <div className="cat-kpi-grid">
                <article className="cat-kpi">
                  <span>Ventas</span>
                  <strong>{dinero(reporte.ventasTotal)}</strong>
                  <small>{reporte.ventasCantidad} venta(s) · ticket prom. {dinero(reporte.ticketPromedio)}</small>
                </article>
                <article className="cat-kpi">
                  <span>Efectivo / Tarjeta</span>
                  <strong>{dinero(reporte.ventasEfectivoTotal)}</strong>
                  <small>Tarjeta {dinero(reporte.ventasTarjetaTotal)}</small>
                </article>
                <article className="cat-kpi">
                  <span>Compras</span>
                  <strong>{dinero(reporte.comprasTotal)}</strong>
                  <small>{reporte.comprasCantidad} compra(s)</small>
                </article>
                <article className="cat-kpi">
                  <span>Resultado</span>
                  <strong className={resultado < 0 ? 'kpi-neg' : ''}>{dinero(reporte.resultado)}</strong>
                  <small>Ventas − compras del período</small>
                </article>
                <article className="cat-kpi">
                  <span>Facturas</span>
                  <strong>{reporte.facturasEmitidas}</strong>
                  <small>{dinero(reporte.facturasTotalEmitido)} · {reporte.facturasAnuladas} anulada(s)</small>
                </article>
                <article className="cat-kpi">
                  <span>Turnos caja</span>
                  <strong>{reporte.turnosCantidad ?? 0}</strong>
                  <small>Aperturas en el rango</small>
                </article>
              </div>
              <p className="placeholder" style={{ marginTop: '0.5rem' }}>
                Use las pestañas para ver detalle, exportar CSV o imprimir cada listado.
              </p>
              <ReporteGraficos reporte={reporte} dinero={dinero} />
            </>
          ) : null}

          {tab === 'ventas' ? (
            <article className="card">
              <h3>Ventas del período</h3>
              {!(reporte.ventas || []).length ? (
                <p className="placeholder">No hay ventas en este rango o filtro.</p>
              ) : (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Número</th>
                      <th>Fecha</th>
                      <th>Cliente</th>
                      <th>Cajero</th>
                      <th>Pago</th>
                      <th>Subtotal</th>
                      <th>IVA</th>
                      <th>Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reporte.ventas.map((venta) => (
                      <tr key={venta.ventaId || venta.numero}>
                        <td>{venta.numero}</td>
                        <td>{fechaHora(venta.fecha)}</td>
                        <td>{venta.clienteNombre || '—'}</td>
                        <td>{venta.cajeroNombre || '—'}</td>
                        <td><span className="cat-badge">{etiquetaPago(venta.formaPago)}</span></td>
                        <td>{dinero(venta.subtotal)}</td>
                        <td>{dinero(venta.impuesto)}</td>
                        <td>{dinero(venta.total)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              <Pagination
                pagina={reporte.pagina ?? pagina}
                totalPaginas={reporte.totalPaginas ?? 1}
                totalElementos={reporte.totalElementos ?? 0}
                tamano={reporte.tamano ?? TAMANO_PAGINA_DEFAULT}
                cargando={cargando}
                onChange={(nueva) => cargar({ pagina: nueva })}
              />
            </article>
          ) : null}

          {tab === 'productos' ? (
            <article className="card">
              <h3>Productos vendidos</h3>
              {!(reporte.productos || []).length ? (
                <p className="placeholder">No hay productos vendidos en este rango.</p>
              ) : (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Código</th>
                      <th>Producto</th>
                      <th>Botellas</th>
                      <th>Tickets</th>
                      <th>Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reporte.productos.map((producto) => (
                      <tr key={producto.productoId || producto.codigo}>
                        <td>{producto.codigo}</td>
                        <td>{producto.nombre}</td>
                        <td>{producto.cantidadUmm}</td>
                        <td>{producto.tickets}</td>
                        <td>{dinero(producto.total)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              <Pagination
                pagina={reporte.pagina ?? pagina}
                totalPaginas={reporte.totalPaginas ?? 1}
                totalElementos={reporte.totalElementos ?? 0}
                tamano={reporte.tamano ?? TAMANO_PAGINA_DEFAULT}
                cargando={cargando}
                onChange={(nueva) => cargar({ pagina: nueva })}
              />
            </article>
          ) : null}

          {tab === 'compras' ? (
            <article className="card">
              <h3>Compras del período</h3>
              {!(reporte.compras || []).length ? (
                <p className="placeholder">No hay compras en este rango.</p>
              ) : (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Número</th>
                      <th>Fecha</th>
                      <th>Proveedor</th>
                      <th>Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reporte.compras.map((compra) => (
                      <tr key={compra.compraId || compra.numero}>
                        <td>{compra.numero}</td>
                        <td>{fechaHora(compra.fecha)}</td>
                        <td>{compra.proveedorNombre}</td>
                        <td>{dinero(compra.total)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              <Pagination
                pagina={reporte.pagina ?? pagina}
                totalPaginas={reporte.totalPaginas ?? 1}
                totalElementos={reporte.totalElementos ?? 0}
                tamano={reporte.tamano ?? TAMANO_PAGINA_DEFAULT}
                cargando={cargando}
                onChange={(nueva) => cargar({ pagina: nueva })}
              />
            </article>
          ) : null}

          {tab === 'facturas' ? (
            <article className="card">
              <h3>Facturas del período</h3>
              {!(reporte.facturas || []).length ? (
                <p className="placeholder">No hay facturas en este rango.</p>
              ) : (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Número</th>
                      <th>Fecha</th>
                      <th>Cliente</th>
                      <th>Cajero</th>
                      <th>Total</th>
                      <th>Estado</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reporte.facturas.map((factura) => (
                      <tr key={factura.facturaId || factura.numero}>
                        <td>{factura.numero}</td>
                        <td>{fechaHora(factura.fechaEmision)}</td>
                        <td>{factura.clienteNombre || '—'}</td>
                        <td>{factura.cajeroNombre || '—'}</td>
                        <td>{dinero(factura.total)}</td>
                        <td><FacturaEstadoChip estado={factura.estado} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              <Pagination
                pagina={reporte.pagina ?? pagina}
                totalPaginas={reporte.totalPaginas ?? 1}
                totalElementos={reporte.totalElementos ?? 0}
                tamano={reporte.tamano ?? TAMANO_PAGINA_DEFAULT}
                cargando={cargando}
                onChange={(nueva) => cargar({ pagina: nueva })}
              />
            </article>
          ) : null}

          {tab === 'caja' ? (
            <article className="card">
              <h3>Turnos de caja</h3>
              {!(reporte.turnos || []).length ? (
                <p className="placeholder">No hay turnos de caja en este rango.</p>
              ) : (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Cajero</th>
                      <th>Apertura</th>
                      <th>Cierre</th>
                      <th>Inicial</th>
                      <th>Ventas</th>
                      <th>Efectivo</th>
                      <th>Diferencia</th>
                      <th>Arqueo</th>
                      <th>Estado</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reporte.turnos.map((turno) => (
                      <tr key={turno.id}>
                        <td>{turno.usuarioNombre || '—'}</td>
                        <td>{fechaHora(turno.fechaApertura)}</td>
                        <td>{fechaHora(turno.fechaCierre)}</td>
                        <td>{dinero(turno.montoInicial)}</td>
                        <td>{dinero(turno.totalVentas)}</td>
                        <td>{dinero(turno.ventasEfectivo)}</td>
                        <td>{dinero(turno.diferencia)}</td>
                        <td><span className={chipArqueo(turno.resultadoArqueo)}>{etiquetaArqueo(turno.resultadoArqueo)}</span></td>
                        <td><span className={chipTurno(turno.estado)}>{turno.estado === 'ABIERTO' ? 'Abierto' : 'Cerrado'}</span></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              <Pagination
                pagina={reporte.pagina ?? pagina}
                totalPaginas={reporte.totalPaginas ?? 1}
                totalElementos={reporte.totalElementos ?? 0}
                tamano={reporte.tamano ?? TAMANO_PAGINA_DEFAULT}
                cargando={cargando}
                onChange={(nueva) => cargar({ pagina: nueva })}
              />
            </article>
          ) : null}
        </div>
      ) : null}
    </section>
  );
}

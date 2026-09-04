import { Link } from 'react-router-dom';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { mensajeError, useAuth } from '../../auth/AuthContext';
import { PERMISOS } from '../../auth/permisos';
import CatalogToolbar, { CatalogFilterSelect, CatalogTabs } from '../../components/catalogo/CatalogToolbar';
import ControlVentaDetalleModal from '../../components/control-ventas/ControlVentaDetalleModal';
import ControlVentasDashboard from '../../components/control-ventas/ControlVentasDashboard';
import Button from '../../components/ui/Button';
import { controlVentasService } from '../../services/controlVentasService';
import { descargarCsv, archivoDocumento, textoCelda, cordoba } from '../../utils/exportCsv';
import { fechaHora, moneda as formatoMoneda } from '../../utils/formato';
import Pagination from '../../components/ui/Pagination';
import { contenidoPagina, metaPagina, TAMANO_PAGINA_DEFAULT } from '../../utils/paginaUtil';

const TABS_BASE = [
  { id: 'resumen', label: 'Resumen' },
  { id: 'eventos', label: 'Eventos' },
  { id: 'cajeros', label: 'Por cajero' },
  { id: 'reglas', label: 'Reglas', permiso: PERMISOS.CONTROL_VENTAS_CONFIG },
];

const TIPO_EVENTO = {
  ALTO_MONTO: 'Alto monto',
  OVERRIDE_PRECIO: 'Cambio de precio',
  SUPERVISOR_AUTORIZADO: 'Autorización admin',
  ANULACION: 'Anulación',
};

const RANGOS_RAPIDOS = [
  ['hoy', 'Hoy'],
  ['7d', '7 días'],
  ['30d', '30 días'],
];

function hoyIso() {
  return new Date().toISOString().slice(0, 10);
}

function haceDiasIso(dias) {
  const d = new Date();
  d.setDate(d.getDate() - dias);
  return d.toISOString().slice(0, 10);
}

function badgeClass(nivel) {
  const val = (nivel || 'medio').toLowerCase();
  return `cv-badge cv-badge-${val}`;
}

function IconoTitulo() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" aria-hidden="true">
      <path d="M12 3L3 7v6c0 5 4 8 9 8s9-3 9-8V7l-9-4z" />
      <path d="M9 12l2 2 4-4" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function IconoKpi({ tipo }) {
  const paths = {
    ventas: <><path d="M4 19V5" /><path d="M4 19h16" /><path d="M8 15l3-4 3 2 4-6" /></>,
    ticket: <><circle cx="12" cy="12" r="8" /><path d="M12 8v4l2 2" /></>,
    alerta: <><path d="M12 3l9 16H3L12 3z" /><path d="M12 10v4M12 18h.01" /></>,
    supervisor: <><circle cx="12" cy="8" r="3" /><path d="M5 20v-1a7 7 0 0114 0v1" /><path d="M16 11l2 2 4-4" /></>,
  };
  return (
    <span className={`cv-kpi-icon cv-kpi-icon-${tipo}`} aria-hidden="true">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7">
        {paths[tipo]}
      </svg>
    </span>
  );
}

export default function ControlVentasPage() {
  const { tienePermiso } = useAuth();
  const tabs = useMemo(
    () => TABS_BASE.filter((tab) => !tab.permiso || tienePermiso(tab.permiso)),
    [tienePermiso],
  );

  const [tab, setTab] = useState('resumen');
  const [desde, setDesde] = useState(haceDiasIso(6));
  const [hasta, setHasta] = useState(hoyIso());
  const [rangoRapido, setRangoRapido] = useState('7d');
  const [tipoEvento, setTipoEvento] = useState('');
  const [cajeroId, setCajeroId] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [autoRefresh, setAutoRefresh] = useState('off');
  const [resumen, setResumen] = useState(null);
  const [eventos, setEventos] = useState([]);
  const [reglas, setReglas] = useState(null);
  const [reglasEdit, setReglasEdit] = useState(null);
  const [eventoDetalle, setEventoDetalle] = useState(null);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [pagina, setPagina] = useState(0);
  const [paginaMeta, setPaginaMeta] = useState({
    totalElementos: 0, totalPaginas: 1, tamano: TAMANO_PAGINA_DEFAULT,
  });

  const paramsBase = useMemo(() => ({ desde, hasta }), [desde, hasta]);

  const cajerosOpciones = useMemo(
    () => resumen?.porCajero || [],
    [resumen?.porCajero],
  );

  const cargarResumen = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      const [info, reglasActuales] = await Promise.all([
        controlVentasService.resumen(paramsBase),
        controlVentasService.reglas(),
      ]);
      setResumen(info);
      setReglas(reglasActuales);
      setReglasEdit(reglasActuales);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }, [paramsBase]);

  const cargarEventos = useCallback(async (paginaDestino = 0) => {
    setCargando(true);
    setError('');
    try {
      const lista = await controlVentasService.eventos({
        ...paramsBase,
        tipo: tipoEvento || undefined,
        cajeroId: cajeroId || undefined,
        busqueda: busqueda.trim() || undefined,
        pagina: paginaDestino,
        tamano: TAMANO_PAGINA_DEFAULT,
      });
      setEventos(contenidoPagina(lista));
      setPaginaMeta(metaPagina(lista));
      setPagina(paginaDestino);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }, [paramsBase, tipoEvento, cajeroId, busqueda]);

  const refrescar = useCallback(async () => {
    if (tab === 'eventos' || tab === 'resumen') {
      await Promise.all([cargarResumen(), cargarEventos()]);
      return;
    }
    await cargarResumen();
  }, [tab, cargarResumen, cargarEventos]);

  useEffect(() => {
    if (!tabs.some((item) => item.id === tab)) {
      setTab(tabs[0]?.id || 'resumen');
    }
  }, [tabs, tab]);

  useEffect(() => {
    if (tab === 'resumen' || tab === 'eventos') {
      cargarResumen();
      cargarEventos();
      return;
    }
    if (tab === 'reglas' || tab === 'cajeros') {
      cargarResumen();
    }
  }, [tab, desde, hasta, cajeroId, tipoEvento]);

  useEffect(() => {
    if (autoRefresh === 'off') return undefined;
    const ms = autoRefresh === '30s' ? 30000 : 60000;
    const id = setInterval(refrescar, ms);
    return () => clearInterval(id);
  }, [autoRefresh, refrescar]);

  function aplicarRango(id) {
    setRangoRapido(id);
    const hoy = hoyIso();
    if (id === 'hoy') {
      setDesde(hoy);
      setHasta(hoy);
      return;
    }
    if (id === '7d') {
      setDesde(haceDiasIso(6));
      setHasta(hoy);
      return;
    }
    setDesde(haceDiasIso(29));
    setHasta(hoy);
  }

  const kpis = useMemo(() => ([
    {
      id: 'ventas',
      label: 'Ventas',
      valor: resumen?.totalVentas ?? 0,
      detalle: formatoMoneda(resumen?.montoTotal),
      clase: 'cv-kpi-ventas',
    },
    {
      id: 'ticket',
      label: 'Ticket prom.',
      valor: formatoMoneda(resumen?.ticketPromedio),
      detalle: `${resumen?.tasaAlertasPct ?? 0}% con alerta`,
      clase: 'cv-kpi-ticket',
    },
    {
      id: 'alerta',
      label: 'Alertas',
      valor: resumen?.alertasTotales ?? 0,
      detalle: `${resumen?.ventasAltoMonto ?? 0} alto monto`,
      clase: 'cv-kpi-alerta',
    },
    {
      id: 'supervisor',
      label: 'Supervisión',
      valor: resumen?.ventasSupervisor ?? 0,
      detalle: `${resumen?.overridesPrecio ?? 0} cambios precio · ${resumen?.anulaciones ?? 0} anul.`,
      clase: 'cv-kpi-riesgo',
    },
  ]), [resumen]);

  async function guardarReglas() {
    setGuardando(true);
    setError('');
    setOk('');
    try {
      const guardadas = await controlVentasService.guardarReglas({
        ...reglasEdit,
        montoAlertaVenta: Number(reglasEdit.montoAlertaVenta),
        montoSupervisorRequerido: Number(reglasEdit.montoSupervisorRequerido),
        maxVentasPorTurno: Number(reglasEdit.maxVentasPorTurno),
      });
      setReglas(guardadas);
      setReglasEdit(guardadas);
      setOk('Reglas de control actualizadas.');
      await cargarResumen();
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setGuardando(false);
    }
  }

  function exportarEventos() {
    descargarCsv(archivoDocumento('Control de ventas', { desde, hasta }), [
      { key: 'tipo', label: 'Tipo de evento', format: (e) => TIPO_EVENTO[e.tipo] || textoCelda(e.tipo) },
      { key: 'fecha', label: 'Fecha y hora', format: (e) => fechaHora(e.fecha) },
      { key: 'referencia', label: 'Referencia', format: (e) => textoCelda(e.referencia) },
      { key: 'cajeroNombre', label: 'Cajero', format: (e) => textoCelda(e.cajeroNombre) },
      { key: 'monto', label: 'Monto (C$)', format: (e) => cordoba(e.monto) },
      { key: 'detalle', label: 'Descripción', format: (e) => textoCelda(e.detalle, 'Sin descripción') },
      { key: 'nivel', label: 'Nivel de alerta', format: (e) => {
        const n = String(e.nivel || '').toLowerCase();
        if (n === 'alto') return 'Alto';
        if (n === 'medio') return 'Medio';
        if (n === 'bajo') return 'Bajo';
        return textoCelda(e.nivel);
      } },
    ], eventos, {
      titulo: 'Control de ventas sensibles',
      subtitulo: 'Autorizaciones, cambios de precio, montos altos y anulaciones en caja',
      desde,
      hasta,
      filtros: [
        tipoEvento && `Tipo: ${TIPO_EVENTO[tipoEvento] || tipoEvento}`,
        cajeroId && 'Cajero filtrado',
        busqueda.trim() && `Búsqueda: ${busqueda.trim()}`,
      ].filter(Boolean).join(' · ') || 'Sin filtros adicionales',
    });
  }

  return (
    <section className="cv-page fca-page">
      <header className="fca-topbar cv-topbar">
        <div className="fca-title-block">
          <div className="fca-title-row">
            <span className="cv-title-icon">
              <IconoTitulo />
            </span>
            <h1>Control de ventas</h1>
          </div>
          <p>
            Supervise ventas sensibles, autorizaciones de administrador, cambios de precio y anulaciones en caja.
          </p>
        </div>
        <div className="fac-header-actions cv-header-actions">
          <Button variant="secondary" onClick={() => refrescar()}>Actualizar</Button>
          <Link to="/facturas" className="btn secondary">Auditoría de facturas</Link>
        </div>
      </header>

      {error ? <p className="pos-alert" role="alert">{error}</p> : null}
      {ok ? <p className="ok-banner" role="status">{ok}</p> : null}

      <article className="card cv-periodo-card">
        <div className="cv-periodo-grid">
          <div className="cv-periodo-label">
            <span>Período</span>
            <div className="cv-quick-filters">
              {RANGOS_RAPIDOS.map(([id, label]) => (
                <button
                  key={id}
                  type="button"
                  className={`cv-quick-pill${rangoRapido === id ? ' active' : ''}`}
                  onClick={() => aplicarRango(id)}
                >
                  {label}
                </button>
              ))}
            </div>
          </div>
          <label className="cv-date-pill">
            Desde
            <input type="date" value={desde} onChange={(e) => { setDesde(e.target.value); setRangoRapido(''); }} />
          </label>
          <label className="cv-date-pill">
            Hasta
            <input type="date" value={hasta} onChange={(e) => { setHasta(e.target.value); setRangoRapido(''); }} />
          </label>
          <label className="cv-date-pill">
            Actualización
            <select value={autoRefresh} onChange={(e) => setAutoRefresh(e.target.value)} aria-label="Auto actualizar">
              <option value="off">Manual</option>
              <option value="30s">Auto 30s</option>
              <option value="60s">Auto 1 min</option>
            </select>
          </label>
        </div>
      </article>

      <div className="cv-kpi-grid">
        {kpis.map((kpi) => (
          <article key={kpi.id} className={`cv-kpi-card ${kpi.clase}`}>
            <IconoKpi tipo={kpi.id} />
            <div>
              <span>{kpi.label}</span>
              <strong>{kpi.valor}</strong>
              <small>{kpi.detalle}</small>
            </div>
          </article>
        ))}
      </div>

      <CatalogTabs tabs={tabs} active={tab} onChange={setTab} />

      {cargando ? <p className="placeholder cv-loading">Cargando…</p> : null}

      {!cargando && tab === 'resumen' && resumen ? (
        <>
          <div className="cv-resumen-layout">
            <article className="card cv-resumen-card">
              <h3>Resumen del período</h3>
              <p className="cv-resumen-periodo">{resumen.desde} — {resumen.hasta}</p>
              <div className="cv-reglas-resumen">
                <span>Alerta ≥ {formatoMoneda(resumen.reglas?.montoAlertaVenta)}</span>
                <span>Admin ≥ {formatoMoneda(resumen.reglas?.montoSupervisorRequerido)}</span>
                <span>
                  Límite turno:{' '}
                  {resumen.reglas?.maxVentasPorTurno > 0 ? resumen.reglas.maxVentasPorTurno : 'Sin límite'}
                </span>
              </div>
              <ul className="cv-resumen-stats">
                <li>
                  <span>Total vendido</span>
                  <strong>{formatoMoneda(resumen.montoTotal)}</strong>
                  <small>{resumen.totalVentas} venta(s)</small>
                </li>
                <li>
                  <span>Con alerta</span>
                  <strong>{resumen.alertasTotales}</strong>
                  <small>{resumen.tasaAlertasPct}% del período</small>
                </li>
                <li>
                  <span>Alto monto</span>
                  <strong>{resumen.ventasAltoMonto}</strong>
                  <small>{formatoMoneda(resumen.montoAltoMonto)}</small>
                </li>
                <li>
                  <span>Autorización admin</span>
                  <strong>{resumen.ventasSupervisor}</strong>
                </li>
                <li>
                  <span>Cambios de precio</span>
                  <strong>{resumen.overridesPrecio}</strong>
                </li>
                <li>
                  <span>Anulaciones</span>
                  <strong>{resumen.anulaciones}</strong>
                </li>
              </ul>
            </article>

            <article className="card cv-recientes-card">
              <div className="cv-table-header">
                <h3>Alertas recientes</h3>
                <Button variant="secondary" onClick={() => setTab('eventos')}>Ver todas</Button>
              </div>
              {!resumen.eventosRecientes?.length ? (
                <p className="placeholder">No hay alertas recientes en este período.</p>
              ) : (
                <ul className="cv-recientes-list">
                  {resumen.eventosRecientes.map((evento, idx) => (
                    <li key={`${evento.tipo}-${evento.referencia}-${idx}`}>
                      <button type="button" className="cv-reciente-item" onClick={() => setEventoDetalle(evento)}>
                        <div className="cv-reciente-top">
                          <span className={badgeClass(evento.nivel)}>{TIPO_EVENTO[evento.tipo] || evento.tipo}</span>
                          <small>{fechaHora(evento.fecha)}</small>
                        </div>
                        <strong>{evento.referencia}</strong>
                        <span className="cv-reciente-meta">
                          {evento.cajeroNombre} · {formatoMoneda(evento.monto)}
                        </span>
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </article>
          </div>

          <ControlVentasDashboard resumen={resumen} eventos={eventos} />
        </>
      ) : null}

      {!cargando && tab === 'cajeros' && resumen ? (
        <article className="card cv-cajeros-card">
          <div className="cv-table-header">
            <h3>Ranking por cajero</h3>
            <span>{resumen.porCajero?.length || 0} cajero(s) con actividad</span>
          </div>
          {!resumen.porCajero?.length ? (
            <p className="placeholder">No hay ventas en el período.</p>
          ) : (
            <div className="cv-table-wrap">
              <table className="data-table cv-table">
                <thead>
                  <tr>
                    <th>Cajero</th>
                    <th>Ventas</th>
                    <th>Monto</th>
                    <th>Ticket prom.</th>
                    <th>Alertas</th>
                    <th>Cambios precio</th>
                    <th>Anulaciones</th>
                  </tr>
                </thead>
                <tbody>
                  {resumen.porCajero.map((cajero) => (
                    <tr
                      key={cajero.cajeroId}
                      className={cajero.alertas > 0 ? 'cv-row-alerta' : ''}
                      role="button"
                      tabIndex={0}
                      onClick={() => { setCajeroId(String(cajero.cajeroId)); setTab('eventos'); }}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') {
                          setCajeroId(String(cajero.cajeroId));
                          setTab('eventos');
                        }
                      }}
                    >
                      <td><strong>{cajero.cajeroNombre || `#${cajero.cajeroId}`}</strong></td>
                      <td>{cajero.totalVentas}</td>
                      <td>{formatoMoneda(cajero.montoTotal)}</td>
                      <td>{formatoMoneda(cajero.ticketPromedio)}</td>
                      <td>
                        {cajero.alertas > 0
                          ? <span className="cv-badge cv-badge-alto">{cajero.alertas}</span>
                          : '0'}
                      </td>
                      <td>{cajero.overrides}</td>
                      <td>{cajero.anulaciones}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </article>
      ) : null}

      {!cargando && tab === 'eventos' ? (
        <article className="card cv-eventos-card">
          <CatalogToolbar
            searchValue={busqueda}
            onSearchChange={setBusqueda}
            onSubmit={cargarEventos}
            searchPlaceholder="Buscar referencia, cajero o detalle…"
            activeCount={[tipoEvento, cajeroId, busqueda.trim()].filter(Boolean).length}
            onClearFilters={() => { setTipoEvento(''); setCajeroId(''); setBusqueda(''); cargarEventos(); }}
          >
            <CatalogFilterSelect value={tipoEvento} onChange={setTipoEvento} ariaLabel="Tipo de evento">
              <option value="">Tipo: todos</option>
              <option value="ALTO_MONTO">Alto monto</option>
              <option value="OVERRIDE_PRECIO">Cambio de precio</option>
              <option value="SUPERVISOR_AUTORIZADO">Autorización admin</option>
              <option value="ANULACION">Anulación</option>
            </CatalogFilterSelect>
            <CatalogFilterSelect value={cajeroId} onChange={setCajeroId} ariaLabel="Cajero">
              <option value="">Cajero: todos</option>
              {cajerosOpciones.map((c) => (
                <option key={c.cajeroId} value={c.cajeroId}>{c.cajeroNombre}</option>
              ))}
            </CatalogFilterSelect>
          </CatalogToolbar>

          <div className="cv-table-header">
            <h3>
              Eventos supervisados
              <span className="cv-count-badge">{eventos.length}</span>
            </h3>
            <div className="cv-table-actions">
              <Button variant="secondary" onClick={exportarEventos} disabled={!eventos.length}>Exportar CSV</Button>
            </div>
          </div>
          {!eventos.length ? (
            <p className="placeholder">No hay eventos con los filtros actuales.</p>
          ) : (
            <div className="cv-table-wrap">
              <table className="data-table cv-table cv-table-click">
                <thead>
                  <tr>
                    <th>Tipo</th>
                    <th>Fecha</th>
                    <th>Referencia</th>
                    <th>Cajero</th>
                    <th>Monto</th>
                    <th>Detalle</th>
                  </tr>
                </thead>
                <tbody>
                  {eventos.map((evento, idx) => (
                    <tr
                      key={`${evento.tipo}-${evento.referencia}-${idx}`}
                      onClick={() => setEventoDetalle(evento)}
                      role="button"
                      tabIndex={0}
                      onKeyDown={(e) => { if (e.key === 'Enter') setEventoDetalle(evento); }}
                    >
                      <td><span className={badgeClass(evento.nivel)}>{TIPO_EVENTO[evento.tipo] || evento.tipo}</span></td>
                      <td>{fechaHora(evento.fecha)}</td>
                      <td>{evento.referencia}</td>
                      <td>{evento.cajeroNombre || '—'}</td>
                      <td>{formatoMoneda(evento.monto)}</td>
                      <td>{evento.detalle || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <Pagination
            pagina={pagina}
            totalPaginas={paginaMeta.totalPaginas}
            totalElementos={paginaMeta.totalElementos}
            tamano={paginaMeta.tamano}
            cargando={cargando}
            onChange={(nueva) => cargarEventos(nueva)}
          />
        </article>
      ) : null}

      {!cargando && tab === 'reglas' && reglasEdit ? (
        <article className="card cv-reglas-card">
          <h3>Reglas operativas</h3>
          <p className="cv-reglas-intro">
            Configure umbrales de alerta, autorización de administrador y límites por turno de caja.
          </p>
          <div className="cv-reglas-grid">
            <label>
              Monto alerta (C$)
              <input
                type="number"
                min="0"
                step="0.01"
                value={reglasEdit.montoAlertaVenta}
                onChange={(e) => setReglasEdit({ ...reglasEdit, montoAlertaVenta: e.target.value })}
              />
              <small>Ventas iguales o superiores generan alerta en el panel.</small>
            </label>
            <label>
              Monto exige administrador (C$)
              <input
                type="number"
                min="0"
                step="0.01"
                value={reglasEdit.montoSupervisorRequerido}
                onChange={(e) => setReglasEdit({ ...reglasEdit, montoSupervisorRequerido: e.target.value })}
              />
              <small>El cajero debe autorizar con clave de administrador.</small>
            </label>
            <label>
              Máx. ventas por turno
              <input
                type="number"
                min="0"
                step="1"
                value={reglasEdit.maxVentasPorTurno}
                onChange={(e) => setReglasEdit({ ...reglasEdit, maxVentasPorTurno: e.target.value })}
              />
              <small>0 = sin límite.</small>
            </label>
            <label className="cv-check">
              <input
                type="checkbox"
                checked={Boolean(reglasEdit.alertarOverridePrecio)}
                onChange={(e) => setReglasEdit({ ...reglasEdit, alertarOverridePrecio: e.target.checked })}
              />
              Registrar y destacar cambios de precio en supervisión
            </label>
          </div>
          <div className="cv-reglas-actions">
            <Button variant="secondary" onClick={() => setReglasEdit(reglas)} disabled={guardando}>Deshacer</Button>
            <Button onClick={guardarReglas} disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar reglas'}</Button>
          </div>
        </article>
      ) : null}

      <ControlVentaDetalleModal evento={eventoDetalle} onClose={() => setEventoDetalle(null)} />
    </section>
  );
}

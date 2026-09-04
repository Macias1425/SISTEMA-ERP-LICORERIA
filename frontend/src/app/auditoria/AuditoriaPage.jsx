import { useCallback, useEffect, useMemo, useState } from 'react';
import { mensajeError } from '../../auth/AuthContext';
import CatalogToolbar, { CatalogFilterSelect, CatalogTabs } from '../../components/catalogo/CatalogToolbar';
import AuditoriaDashboard from '../../components/auditoria/AuditoriaDashboard';
import AuditoriaDetalleModal from '../../components/auditoria/AuditoriaDetalleModal';
import AuditoriaEventoChip from '../../components/auditoria/AuditoriaEventoChip';
import AuditoriaRiesgoChip from '../../components/auditoria/AuditoriaRiesgoChip';
import Button from '../../components/ui/Button';
import ReportePrintHeader from '../../components/reportes/ReportePrintHeader';
import { auditoriaService } from '../../services/auditoriaService';
import { usuarioService } from '../../services/usuarioService';
import { descargarCsv, archivoDocumento, textoCelda } from '../../utils/exportCsv';
import Pagination from '../../components/ui/Pagination';
import { contenidoPagina, listarTodos, metaPagina, TAMANO_PAGINA_DEFAULT } from '../../utils/paginaUtil';

const ACCIONES = [
  'VENTA',
  'ANULACION_FACTURA',
  'COMPRA',
  'CAMBIO_PRECIO',
  'ENTRADA_STOCK',
  'AJUSTE_STOCK',
  'CONFIGURACION',
  'APERTURA_CAJA',
  'CIERRE_CAJA',
  'MERMA_SOLICITADA',
  'MERMA_APROBADA',
  'MERMA_RECHAZADA',
  'BORRADO_PRODUCTO',
];

const MODULOS = ['Ventas', 'Caja', 'Inventario', 'Catálogo', 'Configuración', 'Compras'];

const CATEGORIAS = [
  ['', 'Todos los eventos'],
  ['riesgo_alto', 'Riesgo alto / crítico'],
  ['accesos_denegados', 'Accesos denegados'],
  ['caja', 'Modificaciones de caja'],
  ['inventario', 'Ajustes de inventario'],
];

const TABS = [
  { id: 'bitacora', label: 'Bitácora de auditoría' },
  { id: 'dashboard', label: 'Dashboard analítico' },
];

function fechaHora(valor) {
  if (!valor) return '—';
  return new Date(valor).toLocaleString('es-NI', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

function moduloAEntidad(modulo) {
  if (!modulo) return '';
  const mapa = {
    Ventas: 'Factura',
    Caja: 'TurnoCaja',
    Inventario: 'Inventario',
    Catálogo: 'Producto',
    Configuración: 'Configuracion',
    Compras: 'Compra',
  };
  return mapa[modulo] || '';
}

export default function AuditoriaPage() {
  const [tab, setTab] = useState('bitacora');
  const [eventos, setEventos] = useState([]);
  const [resumen, setResumen] = useState(null);
  const [usuarios, setUsuarios] = useState([]);
  const [detalle, setDetalle] = useState(null);
  const [modalDetalle, setModalDetalle] = useState(false);
  const [busqueda, setBusqueda] = useState('');
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');
  const [usuarioId, setUsuarioId] = useState('');
  const [accion, setAccion] = useState('');
  const [modulo, setModulo] = useState('');
  const [nivelRiesgo, setNivelRiesgo] = useState('');
  const [categoria, setCategoria] = useState('');
  const [autoRefresh, setAutoRefresh] = useState('off');
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(true);
  const [pagina, setPagina] = useState(0);
  const [paginaMeta, setPaginaMeta] = useState({
    totalElementos: 0, totalPaginas: 1, tamano: TAMANO_PAGINA_DEFAULT,
  });

  const filtrosActivos = useMemo(() => {
    let n = 0;
    if (busqueda.trim()) n += 1;
    if (desde) n += 1;
    if (hasta) n += 1;
    if (usuarioId) n += 1;
    if (accion) n += 1;
    if (modulo) n += 1;
    if (nivelRiesgo) n += 1;
    if (categoria) n += 1;
    return n;
  }, [busqueda, desde, hasta, usuarioId, accion, modulo, nivelRiesgo, categoria]);

  const cargar = useCallback(async (params = {}, paginaDestino = 0) => {
    setCargando(true);
    setError('');
    try {
      const entidad = params.modulo !== undefined ? moduloAEntidad(params.modulo ?? modulo) : moduloAEntidad(modulo);
      const filtros = {
        busqueda: (params.busqueda ?? busqueda) || undefined,
        desde: (params.desde ?? desde) || undefined,
        hasta: (params.hasta ?? hasta) || undefined,
        usuarioId: (params.usuarioId ?? usuarioId) || undefined,
        accion: (params.accion ?? accion) || undefined,
        entidad: entidad || undefined,
        nivelRiesgo: (params.nivelRiesgo ?? nivelRiesgo) || undefined,
        categoria: (params.categoria ?? categoria) || undefined,
        pagina: paginaDestino,
        tamano: TAMANO_PAGINA_DEFAULT,
      };
      const [lista, kpis] = await Promise.all([
        auditoriaService.listar(filtros),
        auditoriaService.resumen(),
      ]);
      setEventos(contenidoPagina(lista));
      setPaginaMeta(metaPagina(lista));
      setPagina(paginaDestino);
      setResumen(kpis);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }, [busqueda, desde, hasta, usuarioId, accion, modulo, nivelRiesgo, categoria]);

  useEffect(() => {
    cargar();
    usuarioService.listar({ activo: true, tamano: 200 }).then((r) => setUsuarios(contenidoPagina(r))).catch(() => {});
  }, []);

  useEffect(() => {
    if (autoRefresh === 'off') return undefined;
    const ms = autoRefresh === '10' ? 10000 : 30000;
    const timer = setInterval(() => cargar(), ms);
    return () => clearInterval(timer);
  }, [autoRefresh, cargar]);

  function limpiarFiltros() {
    setBusqueda('');
    setDesde('');
    setHasta('');
    setUsuarioId('');
    setAccion('');
    setModulo('');
    setNivelRiesgo('');
    setCategoria('');
    cargar({
      busqueda: '',
      desde: '',
      hasta: '',
      usuarioId: '',
      accion: '',
      modulo: '',
      nivelRiesgo: '',
      categoria: '',
    });
  }

  function aplicarCategoria(valor) {
    setCategoria(valor);
    cargar({ categoria: valor });
  }

  async function exportarCsv() {
    try {
      const entidad = moduloAEntidad(modulo);
      const todos = await listarTodos((p) => auditoriaService.listar({
        busqueda: busqueda || undefined,
        desde: desde || undefined,
        hasta: hasta || undefined,
        usuarioId: usuarioId || undefined,
        accion: accion || undefined,
        entidad: entidad || undefined,
        nivelRiesgo: nivelRiesgo || undefined,
        categoria: categoria || undefined,
        ...p,
      }));
      descargarCsv(archivoDocumento('Bitacora de auditoria operacional'), [
        { key: 'fechaHora', label: 'Fecha y hora del evento', format: (f) => fechaHora(f.fechaHora) },
        { key: 'usuarioNombre', label: 'Usuario responsable', format: (f) => textoCelda(f.usuarioNombre || f.rol) },
        { key: 'eventoEtiqueta', label: 'Descripción del evento', format: (f) => textoCelda(f.eventoEtiqueta) },
        { key: 'codigoEvento', label: 'Código de evento', format: (f) => textoCelda(f.codigoEvento || f.accion) },
        { key: 'nivelRiesgo', label: 'Nivel de riesgo', format: (f) => (f.nivelRiesgo === 'ALTO' ? 'Alto' : f.nivelRiesgo === 'MEDIO' ? 'Medio' : f.nivelRiesgo === 'BAJO' ? 'Bajo' : textoCelda(f.nivelRiesgo)) },
        { key: 'modulo', label: 'Módulo operativo', format: (f) => textoCelda(f.modulo) },
        { key: 'tablaEntidad', label: 'Entidad afectada', format: (f) => textoCelda(f.tablaEntidad || f.entidad) },
        { key: 'entidadId', label: 'Identificador de registro', format: (f) => (f.entidadId != null ? String(f.entidadId) : 'No aplica') },
        { key: 'ip', label: 'Dirección IP', format: (f) => textoCelda(f.ip, 'No capturada') },
        { key: 'detalle', label: 'Observación / detalle', format: (f) => textoCelda(f.detalle, 'Sin observación') },
      ], todos, {
        titulo: 'Bitácora de auditoría operacional',
        subtitulo: 'Registro inmutable de ventas, caja, inventario y configuración',
        desde: desde || undefined,
        hasta: hasta || undefined,
        filtros: [
          busqueda && `Búsqueda: ${busqueda}`,
          usuarioId && 'Usuario filtrado',
          accion && `Acción: ${accion.replaceAll('_', ' ')}`,
          modulo && `Módulo: ${modulo}`,
          nivelRiesgo && `Riesgo: ${nivelRiesgo}`,
          categoria && `Categoría: ${CATEGORIAS.find(([v]) => v === categoria)?.[1] || categoria}`,
        ].filter(Boolean).join(' · ') || 'Ninguno (bitácora completa)',
      });
    } catch (err) {
      setError(mensajeError(err));
    }
  }

  function exportarPdf() {
    window.print();
  }

  async function verDetalle(evento) {
    try {
      setDetalle(await auditoriaService.obtener(evento.id));
    } catch {
      setDetalle(evento);
    }
    setModalDetalle(true);
  }

  const kpis = resumen || {
    eventosTotales: eventos.length,
    eventosHoy: 0,
    ventasAuditadas: 0,
    eventosCaja: 0,
    riesgoAlto: 0,
  };

  return (
    <section className="aud-page rep-page">
      <header className="cfg-topbar aud-topbar">
        <div>
          <p className="aud-kicker">Trazabilidad y seguridad</p>
          <h1>Auditoría operacional</h1>
          <p className="cfg-subtitle">
            Bitácora append-only de ventas, caja, inventario y configuración. Los registros no pueden editarse ni eliminarse.
          </p>
          <span className="cat-badge cat-badge-info aud-count-badge">
            {kpis.eventosTotales ?? 0} eventos registrados
          </span>
        </div>
        <div className="cfg-topbar-actions aud-topbar-actions">
          <div className="aud-refresh-group" role="group" aria-label="Auto-refresco">
            <span>Auto-refresco</span>
            {[
              ['off', 'OFF'],
              ['10', '10s'],
              ['30', '30s'],
            ].map(([valor, etiqueta]) => (
              <button
                key={valor}
                type="button"
                className={`aud-refresh-btn${autoRefresh === valor ? ' active' : ''}`}
                onClick={() => setAutoRefresh(valor)}
              >
                {etiqueta}
              </button>
            ))}
          </div>
          <Button type="button" variant="secondary" onClick={exportarCsv} disabled={!paginaMeta.totalElementos}>
            Exportar CSV
          </Button>
          <Button type="button" variant="secondary" onClick={exportarPdf}>
            Exportar PDF
          </Button>
          <Button type="button" onClick={() => cargar()} disabled={cargando}>
            {cargando ? 'Actualizando…' : 'Actualizar'}
          </Button>
        </div>
      </header>

      {error ? <p className="auth-error" role="alert">{error}</p> : null}

      <div className="cat-kpi-grid aud-kpi-grid">
        <article className="cat-kpi">
          <span>Eventos totales</span>
          <strong>{kpis.eventosTotales ?? 0}</strong>
          <small>Histórico completo</small>
        </article>
        <article className="cat-kpi">
          <span>Actividad hoy</span>
          <strong>{kpis.eventosHoy ?? 0}</strong>
          <small>Operaciones del día</small>
        </article>
        <article className="cat-kpi">
          <span>Ventas auditadas</span>
          <strong>{kpis.ventasAuditadas ?? 0}</strong>
          <small>Ventas y anulaciones</small>
        </article>
        <article className="cat-kpi">
          <span>Caja / efectivo</span>
          <strong>{kpis.eventosCaja ?? 0}</strong>
          <small>Aperturas y cierres</small>
        </article>
        <article className="cat-kpi aud-kpi-riesgo">
          <span>Riesgo alto</span>
          <strong>{kpis.riesgoAlto ?? 0}</strong>
          <small>Eventos sensibles</small>
        </article>
      </div>

      <CatalogTabs tabs={TABS} active={tab} onChange={setTab} />

      {tab === 'bitacora' ? (
        <>
          <article className="card aud-filters-card">
            <h3>Filtros combinables</h3>
            <CatalogToolbar
              searchValue={busqueda}
              onSearchChange={setBusqueda}
              onSubmit={() => cargar()}
              searchPlaceholder="Buscar usuario, IP, evento…"
              activeCount={filtrosActivos}
              onClearFilters={limpiarFiltros}
            >
              <label className="cat-filter-pill aud-date-pill">
                <span>Desde</span>
                <input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} aria-label="Desde" />
              </label>
              <label className="cat-filter-pill aud-date-pill">
                <span>Hasta</span>
                <input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} aria-label="Hasta" />
              </label>
              <CatalogFilterSelect value={usuarioId} onChange={setUsuarioId} ariaLabel="Usuario">
                <option value="">Usuario</option>
                {usuarios.map((usuario) => (
                  <option key={usuario.id} value={usuario.id}>{usuario.nombreCompleto}</option>
                ))}
              </CatalogFilterSelect>
              <CatalogFilterSelect value={nivelRiesgo} onChange={setNivelRiesgo} ariaLabel="Riesgo">
                <option value="">Riesgo</option>
                <option value="ALTO">Alto</option>
                <option value="MEDIO">Medio</option>
                <option value="BAJO">Bajo</option>
              </CatalogFilterSelect>
              <CatalogFilterSelect value={accion} onChange={setAccion} ariaLabel="Acción">
                <option value="">Acción específica</option>
                {ACCIONES.map((item) => (
                  <option key={item} value={item}>{item.replaceAll('_', ' ')}</option>
                ))}
              </CatalogFilterSelect>
              <CatalogFilterSelect value={modulo} onChange={setModulo} ariaLabel="Módulo">
                <option value="">Módulo</option>
                {MODULOS.map((item) => (
                  <option key={item} value={item}>{item}</option>
                ))}
              </CatalogFilterSelect>
              <Button type="button" onClick={() => cargar()}>Aplicar filtros</Button>
            </CatalogToolbar>

            <div className="aud-quick-filters">
              <span>Filtros rápidos</span>
              {CATEGORIAS.map(([valor, etiqueta]) => (
                <button
                  key={valor || 'todos'}
                  type="button"
                  className={`aud-quick-pill${categoria === valor ? ' active' : ''}`}
                  onClick={() => aplicarCategoria(valor)}
                >
                  {etiqueta}
                </button>
              ))}
            </div>
          </article>

          <article className="card aud-table-card rep-print-area">
            <ReportePrintHeader
              tipo="Documento de auditoría"
              titulo="Bitácora de auditoría operacional"
              subtitulo="Registro inmutable de operaciones de ventas, caja, inventario y configuración."
              desde={desde || undefined}
              hasta={hasta || undefined}
              registros={paginaMeta.totalElementos}
              filtros={[
                busqueda && `Búsqueda: ${busqueda}`,
                usuarioId && 'Usuario filtrado',
                accion && `Acción: ${accion.replaceAll('_', ' ')}`,
                modulo && `Módulo: ${modulo}`,
                nivelRiesgo && `Riesgo: ${nivelRiesgo}`,
                categoria && `Categoría: ${CATEGORIAS.find(([v]) => v === categoria)?.[1] || categoria}`,
              ].filter(Boolean).join(' · ') || 'Ninguno (bitácora completa)'}
            />
            <header className="aud-table-header">
              <h3>Eventos registrados</h3>
              <span>
                {paginaMeta.totalElementos} evento{paginaMeta.totalElementos === 1 ? '' : 's'}
                {paginaMeta.totalPaginas > 1 ? ` · página ${pagina + 1} de ${paginaMeta.totalPaginas}` : ''}
              </span>
            </header>
            <Pagination
              pagina={pagina}
              totalPaginas={paginaMeta.totalPaginas}
              totalElementos={paginaMeta.totalElementos}
              tamano={paginaMeta.tamano}
              cargando={cargando}
              onChange={(nueva) => cargar({}, nueva)}
            />
            {cargando ? <p className="placeholder">Cargando bitácora…</p> : (
              <div className="aud-table-wrap">
                <table className="data-table aud-table">
                  <thead>
                    <tr>
                      <th>Fecha y hora</th>
                      <th>Usuario</th>
                      <th>Evento</th>
                      <th>Riesgo</th>
                      <th>Módulo operativo</th>
                      <th>Registro ID</th>
                      <th>IP</th>
                      <th>Detalle</th>
                    </tr>
                  </thead>
                  <tbody>
                    {eventos.map((evento) => (
                      <tr key={evento.id}>
                        <td>{fechaHora(evento.fechaHora)}</td>
                        <td>{evento.usuarioNombre || evento.rol || '—'}</td>
                        <td>
                          <AuditoriaEventoChip
                            etiqueta={evento.eventoEtiqueta}
                            codigo={evento.codigoEvento || evento.accion}
                          />
                        </td>
                        <td><AuditoriaRiesgoChip nivel={evento.nivelRiesgo} /></td>
                        <td>
                          <div className="aud-modulo">
                            <strong>{evento.modulo || '—'}</strong>
                            <small>{evento.tablaEntidad || evento.entidad || '—'}</small>
                          </div>
                        </td>
                        <td>{evento.entidadId != null ? `#${evento.entidadId}` : '—'}</td>
                        <td>{evento.ip || '—'}</td>
                        <td>
                          <button type="button" className="aud-ver-btn" onClick={() => verDetalle(evento)}>
                            Ver
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            {!cargando && !eventos.length ? <p className="placeholder">No hay eventos con los filtros actuales.</p> : null}
            <Pagination
              pagina={pagina}
              totalPaginas={paginaMeta.totalPaginas}
              totalElementos={paginaMeta.totalElementos}
              tamano={paginaMeta.tamano}
              cargando={cargando}
              onChange={(nueva) => cargar({}, nueva)}
            />
          </article>
        </>
      ) : (
        <AuditoriaDashboard eventos={eventos} />
      )}

      <AuditoriaDetalleModal
        open={modalDetalle}
        evento={detalle}
        onClose={() => setModalDetalle(false)}
      />
    </section>
  );
}

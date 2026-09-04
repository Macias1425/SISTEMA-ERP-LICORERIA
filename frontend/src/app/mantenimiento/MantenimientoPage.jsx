import { useEffect, useMemo, useState } from 'react';
import { mensajeError } from '../../auth/AuthContext';
import { CatalogTabs } from '../../components/catalogo/CatalogToolbar';
import ConfirmacionModal from '../../components/mantenimiento/ConfirmacionModal';
import RestaurarModal from '../../components/mantenimiento/RestaurarModal';
import Button from '../../components/ui/Button';
import { mantenimientoService } from '../../services/mantenimientoService';
import Pagination from '../../components/ui/Pagination';
import { fechaHora, numero } from '../../utils/formato';
import { contenidoPagina, metaPagina, TAMANO_PAGINA_DEFAULT } from '../../utils/paginaUtil';

const TABS = [
  { id: 'resumen', label: 'Resumen' },
  { id: 'respaldos', label: 'Respaldos SQL' },
  { id: 'salud', label: 'Salud de la BD' },
];

function badgeEstado(estado) {
  const val = String(estado || 'OK').toUpperCase();
  if (val === 'CRITICO' || val === 'ERROR') return 'cat-badge cat-badge-danger';
  if (val === 'ADVERTENCIA') return 'cat-badge cat-badge-alerta';
  return 'cat-badge cat-badge-ok';
}

function etiquetaEstado(estado) {
  const val = String(estado || 'OK').toUpperCase();
  if (val === 'CRITICO') return 'Crítico';
  if (val === 'ERROR') return 'Error';
  if (val === 'ADVERTENCIA') return 'Advertencia';
  return 'En orden';
}

function haceTexto(horas) {
  if (horas == null) return 'Sin respaldos';
  if (horas < 1) return 'Hace menos de 1 h';
  if (horas < 24) return `Hace ${horas} h`;
  const dias = Math.floor(horas / 24);
  return `Hace ${dias} día${dias === 1 ? '' : 's'}`;
}

function chipMetodo(metodo) {
  if (metodo === 'MYSQLDUMP') return 'cat-badge cat-badge-info';
  if (metodo === 'JDBC') return 'cat-badge cat-badge-muted';
  return 'cat-badge';
}

function IconoTitulo() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" aria-hidden="true">
      <path d="M12 3L3 7v6c0 5 4 8 9 8s9-3 9-8V7l-9-4z" />
      <path d="M8 12h8M12 8v8" strokeLinecap="round" />
    </svg>
  );
}

function IconoKpi({ tipo }) {
  const paths = {
    respaldo: <><path d="M4 7h16v12H4z" /><path d="M8 7V5h8v2" /><path d="M9 13h6" /></>,
    reloj: <><circle cx="12" cy="12" r="8" /><path d="M12 8v4l3 2" /></>,
    disco: <><ellipse cx="12" cy="7" rx="7" ry="3" /><path d="M5 7v10c0 1.7 3.1 3 7 3s7-1.3 7-3V7" /></>,
    tablas: <><path d="M4 6h16v12H4z" /><path d="M4 10h16M10 6v12" /></>,
    motor: <><circle cx="12" cy="12" r="3" /><path d="M12 5v2M12 17v2M5 12h2M17 12h2" /></>,
  };
  return (
    <span className={`cv-kpi-icon mnt-kpi-icon-${tipo}`} aria-hidden="true">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7">
        {paths[tipo]}
      </svg>
    </span>
  );
}

export default function MantenimientoPage() {
  const [tab, setTab] = useState('resumen');
  const [resumen, setResumen] = useState(null);
  const [respaldos, setRespaldos] = useState([]);
  const [resultado, setResultado] = useState(null);
  const [busqueda, setBusqueda] = useState('');
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [cargando, setCargando] = useState(true);
  const [procesando, setProcesando] = useState('');
  const [modalRestaurar, setModalRestaurar] = useState(null);
  const [confirmacion, setConfirmacion] = useState(null);
  const [pagina, setPagina] = useState(0);
  const [paginaMeta, setPaginaMeta] = useState({
    totalElementos: 0, totalPaginas: 1, tamano: TAMANO_PAGINA_DEFAULT,
  });

  async function cargar(paginaDestino = pagina) {
    setCargando(true);
    setError('');
    try {
      const [info, lista] = await Promise.all([
        mantenimientoService.resumen(),
        mantenimientoService.listarRespaldos({ pagina: paginaDestino, tamano: TAMANO_PAGINA_DEFAULT }),
      ]);
      setResumen(info);
      setRespaldos(contenidoPagina(lista));
      setPaginaMeta(metaPagina(lista));
      setPagina(paginaDestino);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar(0);
  }, []);

  const respaldosFiltrados = useMemo(() => {
    const q = busqueda.trim().toLowerCase();
    if (!q) return respaldos;
    return respaldos.filter((item) => item.nombre.toLowerCase().includes(q) || String(item.metodo || '').toLowerCase().includes(q));
  }, [respaldos, busqueda]);

  const tablasOrdenadas = useMemo(() => {
    const lista = [...(resumen?.tablasSalud || [])];
    return lista;
  }, [resumen?.tablasSalud]);

  const kpis = useMemo(() => ([
    {
      id: 'respaldo',
      label: 'Respaldos',
      valor: resumen?.respaldosTotales ?? 0,
      detalle: `Máx. ${resumen?.maxRespaldos ?? 30} en rotación`,
      clase: 'mnt-kpi-ok',
    },
    {
      id: 'reloj',
      label: 'Último respaldo',
      valor: haceTexto(resumen?.horasDesdeUltimoRespaldo),
      detalle: resumen?.ultimoRespaldo ? fechaHora(resumen.ultimoRespaldo) : 'Cree el primero ahora',
      clase: resumen?.estadoRespaldo === 'CRITICO' ? 'mnt-kpi-critico' : resumen?.estadoRespaldo === 'ADVERTENCIA' ? 'mnt-kpi-alerta' : 'mnt-kpi-ok',
    },
    {
      id: 'disco',
      label: 'Espacio respaldos',
      valor: resumen?.espacioRespaldosLegible || '0 B',
      detalle: `Libre en disco: ${resumen?.espacioDiscoLibreLegible || '—'}`,
      clase: 'mnt-kpi-disco',
    },
    {
      id: 'tablas',
      label: 'Tablas',
      valor: resumen?.tablas ?? 0,
      detalle: `${numero(resumen?.registrosEstimados ?? 0)} filas estimadas`,
      clase: resumen?.tablasConError ? 'mnt-kpi-critico' : resumen?.tablasConAdvertencia ? 'mnt-kpi-alerta' : 'mnt-kpi-ok',
    },
    {
      id: 'motor',
      label: 'Motor',
      valor: resumen?.mysqldumpDisponible ? 'mysqldump' : 'JDBC',
      detalle: resumen?.host || 'MySQL local',
      clase: 'mnt-kpi-motor',
    },
  ]), [resumen]);

  async function crearRespaldo() {
    setProcesando('respaldo');
    setError('');
    setOk('');
    try {
      const creado = await mantenimientoService.crearRespaldo();
      setOk(`Respaldo ${creado.nombre} creado (${creado.metodo}, ${creado.tamanoLegible}).`);
      await cargar(0);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setProcesando('');
    }
  }

  async function descargar(nombre) {
    setError('');
    try {
      await mantenimientoService.descargarRespaldo(nombre);
    } catch (err) {
      setError(mensajeError(err));
    }
  }

  async function eliminarConfirmado(nombre) {
    setProcesando('eliminar');
    setError('');
    setOk('');
    try {
      await mantenimientoService.eliminarRespaldo(nombre);
      setOk('Respaldo eliminado.');
      setConfirmacion(null);
      await cargar();
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setProcesando('');
    }
  }

  async function restaurar({ archivo, confirmacion: texto, nombreExistente }) {
    setProcesando('restaurar');
    setError('');
    setOk('');
    try {
      const res = nombreExistente
        ? await mantenimientoService.restaurarExistente(nombreExistente, texto)
        : await mantenimientoService.restaurar(archivo, texto);
      setOk(res.mensaje);
      setModalRestaurar(null);
      await cargar(0);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setProcesando('');
    }
  }

  async function ejecutarTarea(tipo) {
    setProcesando(tipo);
    setError('');
    setOk('');
    setResultado(null);
    try {
      let res;
      if (tipo === 'verificar') res = await mantenimientoService.verificar();
      else if (tipo === 'fragmentadas') res = await mantenimientoService.optimizar(true);
      else res = await mantenimientoService.optimizar(false);
      setResultado(res);
      setOk(res.mensaje);
      setConfirmacion(null);
      await cargar();
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setProcesando('');
    }
  }

  const ocupado = Boolean(procesando);

  return (
    <section className="cv-page mnt-page">
      <header className="fca-topbar cv-topbar">
        <div className="fca-title-block">
          <div className="fca-title-row">
            <span className="cv-title-icon mnt-title-icon">
              <IconoTitulo />
            </span>
            <h1>Mantenimiento</h1>
          </div>
          <p>
            Respalde, restaure y cuide la base de datos MySQL del POS. Las operaciones quedan en auditoría.
          </p>
        </div>
        <div className="fac-header-actions cv-header-actions">
          <Button type="button" onClick={crearRespaldo} disabled={ocupado || cargando}>
            {procesando === 'respaldo' ? 'Creando respaldo…' : 'Crear respaldo SQL'}
          </Button>
          <Button
            type="button"
            variant="secondary"
            onClick={() => setModalRestaurar({})}
            disabled={!resumen?.restauracionHabilitada || ocupado}
          >
            Restaurar SQL
          </Button>
          <Button type="button" variant="secondary" onClick={() => cargar()} disabled={cargando || ocupado}>
            {cargando ? 'Actualizando…' : 'Actualizar'}
          </Button>
        </div>
      </header>

      {resumen ? (
        <div className={`mnt-status mnt-status-${String(resumen.estadoSalud || 'OK').toLowerCase()}`}>
          <strong>{etiquetaEstado(resumen.estadoSalud)}</strong>
          <span>
            {resumen.baseDatos} · {resumen.version || resumen.motor}
            {resumen.estadoRespaldo === 'OK' ? ' · Respaldo al día' : ` · Respaldo: ${etiquetaEstado(resumen.estadoRespaldo).toLowerCase()}`}
          </span>
        </div>
      ) : null}

      {error ? <p className="pos-alert" role="alert">{error}</p> : null}
      {ok ? <p className="ok-banner" role="status">{ok}</p> : null}

      {ocupado ? (
        <p className="mnt-busy" role="status">
          {procesando === 'respaldo' && 'Generando respaldo SQL… esto puede tardar unos segundos.'}
          {procesando === 'restaurar' && 'Restaurando base de datos… no cierre esta ventana.'}
          {procesando === 'verificar' && 'Verificando integridad de las tablas…'}
          {(procesando === 'optimizar' || procesando === 'fragmentadas') && 'Optimizando tablas… puede bloquear escrituras unos segundos.'}
          {procesando === 'eliminar' && 'Eliminando respaldo…'}
        </p>
      ) : null}

      <div className="cv-kpi-grid mnt-kpi-grid">
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

      <CatalogTabs tabs={TABS} active={tab} onChange={setTab} />

      {tab === 'resumen' ? (
        <div className="mnt-summary-grid">
          <article className="card mnt-info-card">
            <h3>Estado del servidor</h3>
            {cargando && !resumen ? <p className="placeholder">Cargando…</p> : (
              <dl className="mnt-info-list">
                <div><dt>Motor</dt><dd>{resumen?.motor} {resumen?.version}</dd></div>
                <div><dt>Base de datos</dt><dd>{resumen?.baseDatos}</dd></div>
                <div><dt>Servidor</dt><dd>{resumen?.host}</dd></div>
                <div><dt>Directorio</dt><dd>{resumen?.directorioRespaldos}</dd></div>
                <div>
                  <dt>mysqldump</dt>
                  <dd>{resumen?.mysqldumpDisponible ? 'Disponible' : 'No detectado · usa JDBC'}</dd>
                </div>
                <div>
                  <dt>Cliente mysql</dt>
                  <dd>{resumen?.mysqlDisponible ? 'Disponible' : 'No detectado · restauración JDBC'}</dd>
                </div>
                <div>
                  <dt>Restauración</dt>
                  <dd>{resumen?.restauracionHabilitada ? 'Habilitada' : 'Deshabilitada en este entorno'}</dd>
                </div>
                <div>
                  <dt>Rotación</dt>
                  <dd>Máximo {resumen?.maxRespaldos ?? 30} archivos</dd>
                </div>
              </dl>
            )}
          </article>
          <article className="card mnt-info-card">
            <h3>Recomendaciones</h3>
            <ul className="mnt-tips">
              {(resumen?.alertas || []).map((alerta) => (
                <li key={alerta.codigo} className={`mnt-tip mnt-tip-${alerta.nivel}`}>
                  <span className={badgeEstado(alerta.nivel === 'critico' ? 'CRITICO' : alerta.nivel === 'advertencia' ? 'ADVERTENCIA' : 'OK')}>
                    {alerta.nivel === 'critico' ? 'Crítico' : alerta.nivel === 'advertencia' ? 'Atención' : 'Info'}
                  </span>
                  {alerta.mensaje}
                </li>
              ))}
            </ul>
          </article>
        </div>
      ) : null}

      {tab === 'respaldos' ? (
        <article className="card mnt-table-card">
          <header className="aud-table-header mnt-table-head">
            <div>
              <h3>Respaldos SQL</h3>
              <p className="hint">Descargue una copia fuera del servidor. Restaurar pide confirmación del nombre de la base.</p>
            </div>
            <label className="mnt-search">
              <input
                type="search"
                value={busqueda}
                onChange={(e) => setBusqueda(e.target.value)}
                placeholder="Buscar archivo o método…"
                aria-label="Buscar respaldo"
              />
            </label>
          </header>
          {cargando ? <p className="placeholder">Cargando respaldos…</p> : (
            <div className="aud-table-wrap">
              <table className="data-table aud-table">
                <thead>
                  <tr>
                    <th>Archivo</th>
                    <th>Fecha</th>
                    <th>Tamaño</th>
                    <th>Método</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {respaldosFiltrados.map((item) => (
                    <tr key={item.nombre} className={item.vacio ? 'mnt-row-warn' : undefined}>
                      <td>
                        <code>{item.nombre}</code>
                        {item.vacio ? <span className="cat-badge cat-badge-danger">Vacío</span> : null}
                      </td>
                      <td>{fechaHora(item.fechaCreacion)}</td>
                      <td>{item.tamanoLegible}</td>
                      <td><span className={chipMetodo(item.metodo)}>{item.metodo}</span></td>
                      <td>
                        <div className="mnt-row-actions">
                          <button type="button" className="aud-ver-btn" onClick={() => descargar(item.nombre)} disabled={ocupado || item.vacio}>
                            Descargar
                          </button>
                          <button
                            type="button"
                            className="aud-ver-btn"
                            disabled={!item.restaurable || ocupado}
                            onClick={() => setModalRestaurar({ nombre: item.nombre })}
                          >
                            Restaurar
                          </button>
                          <button
                            type="button"
                            className="aud-ver-btn mnt-delete-btn"
                            disabled={ocupado}
                            onClick={() => setConfirmacion({
                              tipo: 'eliminar',
                              nombre: item.nombre,
                              titulo: 'Eliminar respaldo',
                              mensaje: `¿Eliminar de forma permanente el archivo ${item.nombre}? Esta acción no se puede deshacer.`,
                              confirmarLabel: 'Eliminar',
                              peligro: true,
                            })}
                          >
                            Eliminar
                          </button>
                        </div>
                      </td>
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
            onChange={(nueva) => cargar(nueva)}
          />
          {!cargando && !respaldosFiltrados.length ? (
            <p className="placeholder">
              {busqueda ? 'Ningún respaldo coincide con la búsqueda.' : 'Aún no hay respaldos. Use «Crear respaldo SQL» para generar el primero.'}
            </p>
          ) : null}
        </article>
      ) : null}

      {tab === 'salud' ? (
        <div className="mnt-maint-grid">
          <article className="card mnt-task-card">
            <h3>Verificar integridad</h3>
            <p>Ejecuta <code>CHECK TABLE</code> en cada tabla para detectar corrupción. No modifica datos.</p>
            <Button type="button" onClick={() => ejecutarTarea('verificar')} disabled={ocupado}>
              {procesando === 'verificar' ? 'Verificando…' : 'Verificar tablas'}
            </Button>
          </article>
          <article className="card mnt-task-card">
            <h3>Optimizar fragmentadas</h3>
            <p>
              Reorganiza solo las tablas con espacio reclamable
              {resumen?.tablasConAdvertencia ? ` (${resumen.tablasConAdvertencia})` : ''}.
              Más rápido que optimizar todo.
            </p>
            <Button
              type="button"
              variant="secondary"
              disabled={ocupado || !resumen?.tablasConAdvertencia}
              onClick={() => setConfirmacion({
                tipo: 'fragmentadas',
                titulo: 'Optimizar tablas fragmentadas',
                mensaje: 'OPTIMIZE TABLE puede bloquear escrituras unos segundos. Hágalo fuera de hora pico.',
                confirmarLabel: 'Optimizar fragmentadas',
              })}
            >
              {procesando === 'fragmentadas' ? 'Optimizando…' : 'Optimizar fragmentadas'}
            </Button>
          </article>
          <article className="card mnt-task-card">
            <h3>Optimizar todas</h3>
            <p>Ejecuta <code>OPTIMIZE TABLE</code> en toda la base. Úselo de forma puntual, no a diario.</p>
            <Button
              type="button"
              variant="secondary"
              disabled={ocupado}
              onClick={() => setConfirmacion({
                tipo: 'optimizar',
                titulo: 'Optimizar todas las tablas',
                mensaje: 'Esta operación recorre toda la base y puede tardar. Confirme que no hay cajeros cobrando.',
                confirmarLabel: 'Optimizar todas',
              })}
            >
              {procesando === 'optimizar' ? 'Optimizando…' : 'Optimizar todas'}
            </Button>
          </article>

          {resultado ? (
            <article className="card mnt-result-card">
              <h3>Resultado · {resultado.tipo || 'Tarea'}</h3>
              <p className={resultado.exito ? 'ok-banner' : 'pos-alert'}>{resultado.mensaje}</p>
              <ul className="mnt-result-meta">
                {resultado.duracionMs != null ? <li>{(resultado.duracionMs / 1000).toFixed(1)} s</li> : null}
                {resultado.tablasOk ? <li>{resultado.tablasOk} OK</li> : null}
                {resultado.tablasAdvertencia ? <li>{resultado.tablasAdvertencia} advertencia(s)</li> : null}
                {resultado.tablasError ? <li>{resultado.tablasError} error(es)</li> : null}
                {resultado.respaldoSeguridad ? <li>Seguridad: {resultado.respaldoSeguridad}</li> : null}
              </ul>
              {resultado.detalles?.length ? (
                <ul className="mnt-result-list">
                  {resultado.detalles.map((linea) => (
                    <li key={linea}>{linea}</li>
                  ))}
                </ul>
              ) : null}
            </article>
          ) : null}

          <article className="card mnt-table-card">
            <h3>Salud por tabla</h3>
            {cargando ? <p className="placeholder">Cargando…</p> : (
              <div className="aud-table-wrap">
                <table className="data-table aud-table">
                  <thead>
                    <tr>
                      <th>Tabla</th>
                      <th>Motor</th>
                      <th>Filas</th>
                      <th>Tamaño</th>
                      <th>Libre</th>
                      <th>Frag.</th>
                      <th>Estado</th>
                    </tr>
                  </thead>
                  <tbody>
                    {tablasOrdenadas.map((tabla) => (
                      <tr key={tabla.nombre}>
                        <td><code>{tabla.nombre}</code></td>
                        <td>{tabla.motor || '—'}</td>
                        <td>{numero(tabla.filas ?? 0)}</td>
                        <td>{Number(tabla.tamanoMb || 0).toFixed(2)} MB</td>
                        <td>{Number(tabla.espacioLibreMb || 0).toFixed(2)} MB</td>
                        <td>{Number(tabla.fragmentacionPct || 0).toFixed(0)}%</td>
                        <td>
                          <span className={badgeEstado(tabla.estado)} title={tabla.comentario || ''}>
                            {etiquetaEstado(tabla.estado)}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </article>
        </div>
      ) : null}

      {modalRestaurar ? (
        <RestaurarModal
          baseDatos={resumen?.baseDatos}
          respaldoNombre={modalRestaurar.nombre}
          onCerrar={() => setModalRestaurar(null)}
          onConfirmar={restaurar}
          procesando={procesando === 'restaurar'}
        />
      ) : null}

      <ConfirmacionModal
        open={Boolean(confirmacion)}
        titulo={confirmacion?.titulo}
        mensaje={confirmacion?.mensaje}
        confirmarLabel={confirmacion?.confirmarLabel}
        peligro={confirmacion?.peligro}
        procesando={ocupado}
        onCerrar={() => setConfirmacion(null)}
        onConfirmar={() => {
          if (confirmacion?.tipo === 'eliminar') eliminarConfirmado(confirmacion.nombre);
          else if (confirmacion?.tipo === 'fragmentadas') ejecutarTarea('fragmentadas');
          else if (confirmacion?.tipo === 'optimizar') ejecutarTarea('optimizar');
        }}
      />
    </section>
  );
}

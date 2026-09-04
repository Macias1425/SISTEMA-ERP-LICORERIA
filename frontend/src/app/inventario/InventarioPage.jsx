import { Link } from 'react-router-dom';
import { useEffect, useMemo, useState } from 'react';
import { mensajeError, useAuth } from '../../auth/AuthContext';
import { PERMISOS } from '../../auth/permisos';
import Button from '../../components/ui/Button';
import MermaDetalleModal from '../../components/inventario/MermaDetalleModal';
import ProductoKardexModal from '../../components/inventario/ProductoKardexModal';
import MermaEstadoChip from '../../components/inventario/MermaEstadoChip';
import MermaResolucionModal from '../../components/inventario/MermaResolucionModal';
import LotesPanel from '../../components/inventario/LotesPanel';
import MovimientoInventarioModal from '../../components/inventario/MovimientoInventarioModal';
import SolicitarMermaModal from '../../components/inventario/SolicitarMermaModal';
import Pagination from '../../components/ui/Pagination';
import { inventarioService } from '../../services/inventarioService';
import { productoService } from '../../services/productoService';
import { descargarCsv, archivoDocumento, textoCelda } from '../../utils/exportCsv';
import { contenidoPagina, listarTodos, metaPagina, TAMANO_PAGINA_DEFAULT } from '../../utils/paginaUtil';

function fechaCorta(valor) {
  if (!valor) return '—';
  return new Date(valor).toLocaleString('es-NI', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  });
}

function chipAlerta(nivel) {
  if (nivel === 'CRITICO') return 'inv-chip inv-chip-critico';
  if (nivel === 'MINIMO') return 'inv-chip inv-chip-minimo';
  return 'inv-chip inv-chip-ok';
}

function dirIcono(direccion) {
  if (direccion === 'ENTRADA') return <span className="inv-dir inv-dir-in">+</span>;
  if (direccion === 'SALIDA') return <span className="inv-dir inv-dir-out">−</span>;
  return <span className="inv-dir">•</span>;
}

export default function InventarioPage() {
  const { tienePermiso } = useAuth();
  const puedeAjustar = tienePermiso(PERMISOS.INVENTARIO_AJUSTAR);
  const puedeSolicitarMerma = tienePermiso(PERMISOS.MERMA_SOLICITAR);

  const [tab, setTab] = useState('existencias');
  const [productos, setProductos] = useState([]);
  const [productosCatalogo, setProductosCatalogo] = useState([]);
  const [alertas, setAlertas] = useState([]);
  const [movimientos, setMovimientos] = useState([]);
  const [mermas, setMermas] = useState([]);
  const [mermaSel, setMermaSel] = useState(null);
  const [modalDetalleMerma, setModalDetalleMerma] = useState(false);
  const [productoKardexSel, setProductoKardexSel] = useState(null);
  const [modalKardex, setModalKardex] = useState(false);
  const [productoKardex, setProductoKardex] = useState('');
  const [filtroMerma, setFiltroMerma] = useState('TODAS');
  const [desdeKardex, setDesdeKardex] = useState('');
  const [hastaKardex, setHastaKardex] = useState('');
  const [desdeMerma, setDesdeMerma] = useState('');
  const [hastaMerma, setHastaMerma] = useState('');
  const [tipoMov, setTipoMov] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [filtroAlerta, setFiltroAlerta] = useState('');
  const [soloActivos, setSoloActivos] = useState(true);
  const [modalMov, setModalMov] = useState(false);
  const [productoMov, setProductoMov] = useState(null);
  const [modalMerma, setModalMerma] = useState(false);
  const [modalResolucion, setModalResolucion] = useState(null);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [cargando, setCargando] = useState(true);
  const [cargandoKardex, setCargandoKardex] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [paginaExistencias, setPaginaExistencias] = useState(0);
  const [metaExistencias, setMetaExistencias] = useState({
    totalElementos: 0, totalPaginas: 1, tamano: TAMANO_PAGINA_DEFAULT,
  });
  const [paginaKardex, setPaginaKardex] = useState(0);
  const [metaKardex, setMetaKardex] = useState({
    totalElementos: 0, totalPaginas: 1, tamano: 30,
  });
  const [paginaMermas, setPaginaMermas] = useState(0);
  const [metaMermas, setMetaMermas] = useState({
    totalElementos: 0, totalPaginas: 1, tamano: TAMANO_PAGINA_DEFAULT,
  });

  async function cargarBase(paginaDestino = paginaExistencias) {
    setCargando(true);
    setError('');
    try {
      const [respuestaProductos, listaAlertas] = await Promise.all([
        productoService.listar({
          activo: soloActivos ? true : undefined,
          busqueda: busqueda.trim() || undefined,
          nivelAlerta: filtroAlerta || undefined,
          pagina: paginaDestino,
          tamano: TAMANO_PAGINA_DEFAULT,
        }),
        inventarioService.alertas({ tamano: 200 }).catch(() => []),
      ]);
      setProductos(contenidoPagina(respuestaProductos));
      setMetaExistencias(metaPagina(respuestaProductos));
      setPaginaExistencias(paginaDestino);
      setAlertas(contenidoPagina(listaAlertas));
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }

  async function cargarKardex(params = {}, paginaDestino = 0) {
    setCargandoKardex(true);
    try {
      const respuesta = await inventarioService.listarMovimientos({
        productoId: (params.productoId ?? productoKardex) || undefined,
        tipo: (params.tipo ?? tipoMov) || undefined,
        desde: (params.desde ?? desdeKardex) || undefined,
        hasta: (params.hasta ?? hastaKardex) || undefined,
        pagina: paginaDestino,
        tamano: 30,
      });
      setMovimientos(contenidoPagina(respuesta));
      setMetaKardex(metaPagina(respuesta));
      setPaginaKardex(paginaDestino);
      setError('');
    } catch (err) {
      setMovimientos([]);
      setError(mensajeError(err));
    } finally {
      setCargandoKardex(false);
    }
  }

  async function cargarMermas(params = {}, paginaDestino = 0) {
    try {
      const estado = params.estado ?? filtroMerma;
      const respuesta = await inventarioService.listarMermas({
        estado: estado === 'TODAS' ? undefined : estado,
        desde: (params.desde ?? desdeMerma) || undefined,
        hasta: (params.hasta ?? hastaMerma) || undefined,
        pagina: paginaDestino,
        tamano: TAMANO_PAGINA_DEFAULT,
      });
      setMermas(contenidoPagina(respuesta));
      setMetaMermas(metaPagina(respuesta));
      setPaginaMermas(paginaDestino);
    } catch (err) {
      setMermas([]);
      setError(mensajeError(err));
    }
  }

  useEffect(() => {
    cargarBase(0);
    listarTodos((p) => productoService.listar({ activo: true, ...p }))
      .then(setProductosCatalogo)
      .catch(() => setProductosCatalogo([]));
  }, [soloActivos]);

  useEffect(() => {
    if (tab === 'kardex') cargarKardex();
    if (tab === 'mermas') cargarMermas();
  }, [tab]);

  const mapaAlertas = useMemo(() => {
    const mapa = new Map();
    alertas.forEach((a) => mapa.set(a.productoId, a.nivelAlerta));
    return mapa;
  }, [alertas]);

  const productosFiltrados = productos;

  const resumenExistencias = useMemo(() => {
    const criticos = alertas.filter((a) => a.nivelAlerta === 'CRITICO').length;
    const minimos = alertas.filter((a) => a.nivelAlerta === 'MINIMO').length;
    const stockTotal = productos.reduce((s, p) => s + Number(p.stockActual || 0), 0);
    return {
      criticos,
      minimos,
      stockTotal,
      productos: metaExistencias.totalElementos,
    };
  }, [alertas, productos, metaExistencias.totalElementos]);

  const resumenMermas = useMemo(() => ({
    pendientes: mermas.filter((m) => m.estado === 'PENDIENTE').length,
    aprobadas: mermas.filter((m) => m.estado === 'APROBADA').length,
    rechazadas: mermas.filter((m) => m.estado === 'RECHAZADA').length,
  }), [mermas]);

  function irAKardex(producto) {
    setProductoKardexSel(producto);
    setModalKardex(true);
    setOk('');
    setError('');
  }

  async function confirmarMovimiento(payload) {
    setGuardando(true);
    setError('');
    setOk('');
    try {
      await inventarioService.registrarMovimiento(payload);
      setModalMov(false);
      setOk(payload.tipo === 'ENTRADA' ? 'Entrada registrada.' : 'Ajuste aplicado.');
      await Promise.all([cargarBase(), cargarKardex({ productoId: payload.productoId })]);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setGuardando(false);
    }
  }

  async function verMerma(merma) {
    setError('');
    try {
      setMermaSel(await inventarioService.obtenerMerma(merma.id));
    } catch {
      setMermaSel(merma);
    }
    setModalDetalleMerma(true);
  }

  async function confirmarMerma(payload) {
    setGuardando(true);
    setError('');
    setOk('');
    try {
      const creada = await inventarioService.solicitarMerma(payload);
      setModalMerma(false);
      setOk('Solicitud de merma enviada. Pendiente de aprobación admin.');
      setMermaSel(creada);
      await Promise.all([cargarBase(), cargarMermas()]);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setGuardando(false);
    }
  }

  async function resolverMerma(payload) {
    if (!mermaSel || !modalResolucion) return;
    setGuardando(true);
    setError('');
    setOk('');
    try {
      const actualizada = modalResolucion === 'aprobar'
        ? await inventarioService.aprobarMerma(mermaSel.id, payload)
        : await inventarioService.rechazarMerma(mermaSel.id, payload);
      setMermaSel(actualizada);
      setMermas((lista) => lista.map((item) => (item.id === actualizada.id ? { ...item, ...actualizada } : item)));
      setModalResolucion(null);
      setOk(modalResolucion === 'aprobar' ? 'Merma aprobada. Stock descontado.' : 'Merma rechazada.');
      await Promise.all([cargarBase(), cargarMermas(), cargarKardex()]);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setGuardando(false);
    }
  }

  function exportarExistenciasCsv() {
    descargarCsv(archivoDocumento('Existencias de inventario'), [
      { key: 'codigo', label: 'Código de producto', format: (p) => textoCelda(p.codigo) },
      { key: 'nombre', label: 'Producto', format: (p) => textoCelda(p.nombre) },
      { key: 'categoriaNombre', label: 'Categoría', format: (p) => textoCelda(p.categoriaNombre, 'Sin categoría') },
      { key: 'stockActual', label: 'Existencia actual (UMM)' },
      { key: 'stockMinimo', label: 'Stock mínimo' },
      { key: 'stockCritico', label: 'Stock crítico' },
      { key: 'nivelAlerta', label: 'Nivel de alerta', format: (p) => {
        const n = mapaAlertas.get(p.id) || p.nivelAlerta || 'OK';
        if (n === 'CRITICO') return 'Crítico';
        if (n === 'MINIMO') return 'Mínimo';
        return 'Normal';
      } },
    ], productosFiltrados, {
      titulo: 'Existencias de inventario',
      subtitulo: 'Stock actual por producto en unidad mínima de manejo',
      filtros: [
        soloActivos ? 'Solo productos activos' : 'Activos e inactivos',
        filtroAlerta && `Alerta: ${filtroAlerta === 'CRITICO' ? 'Crítico' : filtroAlerta === 'MINIMO' ? 'Mínimo' : 'Normal'}`,
        busqueda && `Búsqueda: ${busqueda}`,
      ].filter(Boolean).join(' · '),
    });
  }

  return (
    <section className="inv-page">
      <header className="page-header fac-header">
        <div>
          <h1>Inventario</h1>
          <p>
            Consulte existencias, kardex trazable y mermas. Las compras se registran en{' '}
            <Link to="/compras">Abastecimiento</Link>.
          </p>
        </div>
        <div className="inv-header-actions">
          {puedeAjustar ? (
            <Button type="button" onClick={() => { setProductoMov(null); setModalMov(true); }}>
              Entrada / ajuste
            </Button>
          ) : null}
          <Link to="/compras" className="btn secondary">Ir a compras</Link>
        </div>
      </header>

      {error ? <p className="pos-alert" role="alert">{error}</p> : null}
      {ok ? <p className="ok-banner" role="status">{ok}</p> : null}

      <nav className="inv-tabs" aria-label="Secciones de inventario">
        {[
          ['existencias', 'Existencias'],
          ['lotes', 'Lotes y vencimientos'],
          ['kardex', 'Kardex'],
          ['mermas', 'Mermas'],
        ].map(([id, etiqueta]) => (
          <button
            key={id}
            type="button"
            className={`inv-tab${tab === id ? ' active' : ''}`}
            onClick={() => { setTab(id); setError(''); setOk(''); }}
          >
            {etiqueta}
          </button>
        ))}
      </nav>

      {tab === 'existencias' ? (
        <>
          <div className="fac-stats inv-stats">
            <article className="pos-stat accent">
              <span>Productos</span>
              <strong>{resumenExistencias.productos}</strong>
              <small>{resumenExistencias.stockTotal} UMM en stock</small>
            </article>
            <article className="pos-stat warn">
              <span>Críticos</span>
              <strong>{resumenExistencias.criticos}</strong>
              <small>Reposición urgente</small>
            </article>
            <article className="pos-stat">
              <span>En mínimo</span>
              <strong>{resumenExistencias.minimos}</strong>
              <small>Alerta de reorden</small>
            </article>
          </div>

          <form
            className="fac-filters inv-filters"
            onSubmit={(e) => {
              e.preventDefault();
              cargarBase(0);
            }}
          >
            <label className="inv-search">
              Buscar
              <input
                type="search"
                placeholder="Código, nombre o categoría…"
                value={busqueda}
                onChange={(e) => setBusqueda(e.target.value)}
              />
            </label>
            <label>
              Alerta
              <select value={filtroAlerta} onChange={(e) => setFiltroAlerta(e.target.value)}>
                <option value="">Todas</option>
                <option value="CRITICO">Crítico</option>
                <option value="MINIMO">Mínimo</option>
                <option value="OK">OK</option>
              </select>
            </label>
            <label className="check inv-check-inline">
              <input
                type="checkbox"
                checked={soloActivos}
                onChange={(e) => setSoloActivos(e.target.checked)}
              />
              Solo activos
            </label>
            <button type="submit" className="btn secondary">Aplicar filtros</button>
            <button type="button" className="btn secondary" onClick={exportarExistenciasCsv}>
              Exportar CSV
            </button>
          </form>

          <article className="card inv-card">
            <div className="inv-card-head">
              <h3>Stock actual</h3>
              <span className="hint">{metaExistencias.totalElementos} producto(s)</span>
            </div>
            {cargando ? (
              <p className="placeholder">Cargando existencias…</p>
            ) : !productosFiltrados.length ? (
              <p className="placeholder">No hay productos con los filtros actuales.</p>
            ) : (
              <div className="fac-table-wrap">
                <table className="data-table fac-table inv-table">
                  <thead>
                    <tr>
                      <th>Código</th>
                      <th>Producto</th>
                      <th>Categoría</th>
                      <th>Stock (UMM)</th>
                      <th>Mín / Crít</th>
                      <th>Alerta</th>
                      <th className="no-print">Acciones</th>
                    </tr>
                  </thead>
                  <tbody>
                    {productosFiltrados.map((producto) => {
                      const nivel = mapaAlertas.get(producto.id) || producto.nivelAlerta || 'OK';
                      return (
                        <tr key={producto.id}>
                          <td><code>{producto.codigo}</code></td>
                          <td>
                            <strong>{producto.nombre}</strong>
                            {producto.activo === false ? <span className="inv-inactivo"> Inactivo</span> : null}
                          </td>
                          <td>{producto.categoriaNombre || '—'}</td>
                          <td>{producto.stockActual} {producto.unidadMinima || 'UMM'}</td>
                          <td>{producto.stockMinimo} / {producto.stockCritico}</td>
                          <td><span className={chipAlerta(nivel)}>{nivel}</span></td>
                          <td className="no-print">
                            <button
                              type="button"
                              className="btn link inv-link-btn"
                              onClick={() => irAKardex(producto)}
                            >
                              Ver kardex
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
            <Pagination
              pagina={paginaExistencias}
              totalPaginas={metaExistencias.totalPaginas}
              totalElementos={metaExistencias.totalElementos}
              tamano={metaExistencias.tamano}
              cargando={cargando}
              onChange={(nueva) => cargarBase(nueva)}
            />
          </article>
        </>
      ) : null}

      {tab === 'lotes' ? <LotesPanel productos={productosCatalogo} /> : null}

      {tab === 'kardex' ? (
        <>
          <form
            className="fac-filters inv-filters"
            onSubmit={(e) => {
              e.preventDefault();
              cargarKardex({}, 0);
            }}
          >
            <label>
              Producto
              <select
                value={productoKardex}
                onChange={(e) => setProductoKardex(e.target.value)}
              >
                <option value="">Todos</option>
                {productosCatalogo.map((producto) => (
                  <option key={producto.id} value={producto.id}>
                    {producto.codigo} · {producto.nombre}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Tipo
              <select value={tipoMov} onChange={(e) => setTipoMov(e.target.value)}>
                <option value="">Todos</option>
                <option value="ENTRADA">Entrada</option>
                <option value="AJUSTE">Ajuste</option>
                <option value="COMPRA">Compra</option>
                <option value="VENTA">Venta</option>
                <option value="MERMA">Merma</option>
                <option value="ANULACION">Anulación</option>
              </select>
            </label>
            <label>
              Desde
              <input type="date" value={desdeKardex} onChange={(e) => setDesdeKardex(e.target.value)} />
            </label>
            <label>
              Hasta
              <input type="date" value={hastaKardex} onChange={(e) => setHastaKardex(e.target.value)} />
            </label>
            <button type="submit" className="btn secondary">Filtrar</button>
            {puedeAjustar ? (
              <button
                type="button"
                className="btn primary"
                onClick={() => {
                  const p = productos.find((item) => String(item.id) === String(productoKardex));
                  setProductoMov(p || null);
                  setModalMov(true);
                }}
              >
                Registrar movimiento
              </button>
            ) : null}
          </form>

          <article className="card inv-card">
            <div className="inv-card-head">
              <h3>Historial de movimientos</h3>
              <span className="hint">Últimos 250 registros</span>
            </div>
            {cargandoKardex ? (
              <p className="placeholder">Cargando kardex…</p>
            ) : !movimientos.length ? (
              <p className="placeholder">Sin movimientos con los filtros actuales.</p>
            ) : (
              <div className="fac-table-wrap">
                <table className="data-table fac-table inv-kardex-table">
                  <thead>
                    <tr>
                      <th>Fecha</th>
                      <th>Dir.</th>
                      <th>Tipo</th>
                      <th>Producto</th>
                      <th>Presentación</th>
                      <th>Cant.</th>
                      <th>UMM</th>
                      <th>Stock</th>
                      <th>Lotes</th>
                      <th>Referencia / motivo</th>
                      <th>Usuario</th>
                    </tr>
                  </thead>
                  <tbody>
                    {movimientos.map((movimiento) => (
                      <tr key={movimiento.id}>
                        <td>{fechaCorta(movimiento.fecha)}</td>
                        <td>{dirIcono(movimiento.direccion)}</td>
                        <td><span className="inv-tipo">{movimiento.tipo}</span></td>
                        <td>{movimiento.productoNombre || movimiento.productoId}</td>
                        <td>{movimiento.presentacionNombre || '—'}</td>
                        <td>{movimiento.cantidadPresentacion}</td>
                        <td>{movimiento.cantidadUmm}</td>
                        <td>{movimiento.stockResultante}</td>
                        <td className="inv-lotes-cell">
                          {movimiento.detalleLotes ? <code>{movimiento.detalleLotes}</code> : '—'}
                        </td>
                        <td className="inv-ref">
                          {movimiento.compraId ? (
                            <Link to="/compras">Compra #{movimiento.compraId}</Link>
                          ) : movimiento.ventaId ? (
                            <Link to="/facturas">Venta #{movimiento.ventaId}</Link>
                          ) : movimiento.mermaId ? (
                            <button
                              type="button"
                              className="inv-link-btn"
                              onClick={() => {
                                inventarioService.obtenerMerma(movimiento.mermaId)
                                  .then((detalle) => {
                                    setMermaSel(detalle);
                                    setModalDetalleMerma(true);
                                  })
                                  .catch(() => {});
                              }}
                            >
                              Merma #{movimiento.mermaId}
                            </button>
                          ) : (movimiento.motivo || '—')}
                        </td>
                        <td>{movimiento.usuarioNombre || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <Pagination
              pagina={paginaKardex}
              totalPaginas={metaKardex.totalPaginas}
              totalElementos={metaKardex.totalElementos}
              tamano={metaKardex.tamano}
              cargando={cargandoKardex}
              onChange={(nueva) => cargarKardex({}, nueva)}
            />
          </article>
        </>
      ) : null}

      {tab === 'mermas' ? (
        <>
          <div className="fac-stats inv-stats">
            <article className="pos-stat warn">
              <span>Pendientes</span>
              <strong>{resumenMermas.pendientes}</strong>
              <small>Esperan admin</small>
            </article>
            <article className="pos-stat accent">
              <span>Aprobadas</span>
              <strong>{resumenMermas.aprobadas}</strong>
              <small>Stock descontado</small>
            </article>
            <article className="pos-stat">
              <span>Rechazadas</span>
              <strong>{resumenMermas.rechazadas}</strong>
              <small>Sin movimiento</small>
            </article>
          </div>

          <form
            className="fac-filters inv-filters"
            onSubmit={(e) => {
              e.preventDefault();
              cargarMermas({}, 0);
            }}
          >
            <div className="chips-row inv-chips">
              {['TODAS', 'PENDIENTE', 'APROBADA', 'RECHAZADA'].map((estado) => (
                <button
                  key={estado}
                  type="button"
                  className={`chip-btn${filtroMerma === estado ? ' active' : ''}`}
                  onClick={() => setFiltroMerma(estado)}
                >
                  {estado === 'TODAS' ? 'Todas' : estado.charAt(0) + estado.slice(1).toLowerCase()}
                </button>
              ))}
            </div>
            <label>
              Desde
              <input type="date" value={desdeMerma} onChange={(e) => setDesdeMerma(e.target.value)} />
            </label>
            <label>
              Hasta
              <input type="date" value={hastaMerma} onChange={(e) => setHastaMerma(e.target.value)} />
            </label>
            <button type="submit" className="btn secondary">Filtrar</button>
            {puedeSolicitarMerma ? (
              <button type="button" className="btn primary" onClick={() => setModalMerma(true)}>
                Solicitar merma
              </button>
            ) : null}
          </form>

          <article className="card inv-card">
            <h3>Solicitudes</h3>
            {!mermas.length ? (
              <p className="placeholder">No hay mermas con los filtros actuales.</p>
            ) : (
              <div className="fac-table-wrap">
                <table className="data-table fac-table">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Producto</th>
                      <th>Cant.</th>
                      <th>Estado</th>
                      <th>Fecha</th>
                      <th className="no-print">Acciones</th>
                    </tr>
                  </thead>
                  <tbody>
                    {mermas.map((merma) => (
                      <tr key={merma.id}>
                        <td>{merma.id}</td>
                        <td>{merma.productoNombre || merma.productoId}</td>
                        <td>{merma.cantidadPresentacion}</td>
                        <td><MermaEstadoChip estado={merma.estado} /></td>
                        <td>{fechaCorta(merma.fechaSolicitud)}</td>
                        <td className="no-print">
                          <button type="button" className="btn link" onClick={() => verMerma(merma)}>
                            Ver detalle
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <Pagination
              pagina={paginaMermas}
              totalPaginas={metaMermas.totalPaginas}
              totalElementos={metaMermas.totalElementos}
              tamano={metaMermas.tamano}
              onChange={(nueva) => cargarMermas({}, nueva)}
            />
          </article>
        </>
      ) : null}

      <ProductoKardexModal
        open={modalKardex}
        producto={productoKardexSel}
        onClose={() => setModalKardex(false)}
      />
      <MermaDetalleModal
        open={modalDetalleMerma}
        merma={mermaSel}
        onClose={() => setModalDetalleMerma(false)}
        onSolicitar={puedeSolicitarMerma ? () => setModalMerma(true) : null}
        onAprobar={() => setModalResolucion('aprobar')}
        onRechazar={() => setModalResolucion('rechazar')}
      />
      <MovimientoInventarioModal
        open={modalMov}
        productos={productosCatalogo}
        productoPreseleccionado={productoMov}
        guardando={guardando}
        onClose={() => setModalMov(false)}
        onConfirmar={confirmarMovimiento}
      />
      <SolicitarMermaModal
        open={modalMerma}
        productos={productosCatalogo.filter((p) => p.activo !== false && Number(p.stockActual || 0) > 0)}
        guardando={guardando}
        onClose={() => setModalMerma(false)}
        onConfirmar={confirmarMerma}
      />
      <MermaResolucionModal
        open={Boolean(modalResolucion)}
        merma={mermaSel}
        modo={modalResolucion}
        guardando={guardando}
        onClose={() => setModalResolucion(null)}
        onConfirmar={resolverMerma}
      />
    </section>
  );
}

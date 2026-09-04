import { useEffect, useMemo, useState } from 'react';
import { mensajeError } from '../../auth/AuthContext';
import { CatalogFilterSelect, CatalogTabs } from '../../components/catalogo/CatalogToolbar';
import EditarPreciosModal from '../../components/marcas-precios/EditarPreciosModal';
import Button from '../../components/ui/Button';
import Modal from '../../components/ui/Modal';
import { marcasPreciosService } from '../../services/marcasPreciosService';
import { descargarCsv, archivoDocumento, textoCelda } from '../../utils/exportCsv';
import { dinero } from '../../utils/formato';
import Pagination from '../../components/ui/Pagination';
import { contenidoPagina, metaPagina, TAMANO_PAGINA_DEFAULT } from '../../utils/paginaUtil';

const TABS = [
  { id: 'marcas', label: 'Marcas' },
  { id: 'historial', label: 'Historial de precios' },
  { id: 'catalogo', label: 'Catálogo de precios' },
];

const TIPOS_CAMBIO = [
  ['', 'Todos los tipos'],
  ['COMPRA_VENTA', 'Compra y venta'],
  ['COSTO', 'Costo'],
  ['COSTO_COMPRA', 'Costo por compra'],
  ['VENTA', 'Precio venta'],
  ['AUTO_MARKUP_COMPRA', 'Markup automático (compra)'],
  ['SUGERIDO_COMPRA', 'Precio sugerido (compra)'],
  ['LISTA_MAYORISTA', 'Lista mayorista'],
  ['LISTA_DETAL', 'Lista detalle'],
  ['OVERRIDE_VENTA', 'Override POS'],
];

function hoyLocal() {
  const ahora = new Date();
  return [
    ahora.getFullYear(),
    String(ahora.getMonth() + 1).padStart(2, '0'),
    String(ahora.getDate()).padStart(2, '0'),
  ].join('-');
}

function hace30Dias() {
  const ahora = new Date();
  ahora.setDate(ahora.getDate() - 30);
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

function chipTipo(tipo) {
  if (tipo === 'COSTO_COMPRA' || tipo === 'COSTO') return 'cat-badge cat-badge-warn';
  if (tipo === 'AUTO_MARKUP_COMPRA' || tipo === 'SUGERIDO_COMPRA') return 'cat-badge cat-badge-info';
  if (tipo?.startsWith('LISTA')) return 'cat-badge cat-badge-info';
  if (tipo === 'OVERRIDE_VENTA') return 'cat-badge';
  return 'cat-badge cat-badge-ok';
}

function etiquetaTipo(tipo) {
  const mapa = {
    COMPRA_VENTA: 'Compra y venta',
    COSTO: 'Costo',
    COSTO_COMPRA: 'Costo compra',
    VENTA: 'Venta',
    AUTO_MARKUP_COMPRA: 'Markup automático',
    SUGERIDO_COMPRA: 'Precio sugerido',
    LISTA_MAYORISTA: 'Mayorista',
    LISTA_DETAL: 'Detalle',
    LISTA_PRECIO: 'Lista precio',
    OVERRIDE_VENTA: 'Override POS',
    PRECIO: 'Precio',
  };
  return mapa[tipo] || tipo || '—';
}

export default function MarcasPreciosPage() {
  const [tab, setTab] = useState('marcas');
  const [resumen, setResumen] = useState(null);
  const [marcas, setMarcas] = useState([]);
  const [historial, setHistorial] = useState([]);
  const [catalogo, setCatalogo] = useState([]);
  const [marcaSel, setMarcaSel] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [desde, setDesde] = useState(hace30Dias());
  const [hasta, setHasta] = useState(hoyLocal());
  const [tipoCambio, setTipoCambio] = useState('');
  const [soloActivos, setSoloActivos] = useState(true);
  const [editando, setEditando] = useState(null);
  const [renombrar, setRenombrar] = useState(null);
  const [marcaNueva, setMarcaNueva] = useState('');
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [cargando, setCargando] = useState(true);
  const [pagina, setPagina] = useState(0);
  const [paginaMeta, setPaginaMeta] = useState({
    totalElementos: 0, totalPaginas: 1, tamano: TAMANO_PAGINA_DEFAULT,
  });

  async function cargar(params = {}, paginaDestino = 0) {
    setCargando(true);
    setError('');
    try {
      const comunes = {
        busqueda: params.busqueda ?? busqueda,
        marca: params.marca ?? marcaSel,
        pagina: paginaDestino,
        tamano: TAMANO_PAGINA_DEFAULT,
      };
      const [info, listaMarcas, hist, cat] = await Promise.all([
        marcasPreciosService.resumen(),
        marcasPreciosService.marcas({ busqueda: comunes.busqueda, pagina: paginaDestino, tamano: TAMANO_PAGINA_DEFAULT }),
        marcasPreciosService.historial({
          desde: params.desde ?? desde,
          hasta: params.hasta ?? hasta,
          busqueda: comunes.busqueda,
          marca: comunes.marca,
          tipoCambio: params.tipoCambio ?? tipoCambio,
          pagina: paginaDestino,
          tamano: TAMANO_PAGINA_DEFAULT,
        }),
        marcasPreciosService.catalogo({
          busqueda: comunes.busqueda,
          marca: comunes.marca,
          activo: (params.soloActivos ?? soloActivos) ? true : undefined,
          pagina: paginaDestino,
          tamano: TAMANO_PAGINA_DEFAULT,
        }),
      ]);
      setResumen(info);
      const tabActual = params.tab ?? tab;
      const paginaActiva = tabActual === 'historial' ? hist : tabActual === 'catalogo' ? cat : listaMarcas;
      setMarcas(contenidoPagina(listaMarcas));
      setHistorial(contenidoPagina(hist));
      setCatalogo(contenidoPagina(cat));
      setPaginaMeta(metaPagina(paginaActiva));
      setPagina(paginaDestino);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar({ busqueda: '', marca: '', tipoCambio: '', soloActivos: true });
  }, []);

  const kpis = resumen || {
    marcasRegistradas: 0,
    productosConMarca: 0,
    productosSinMarca: 0,
    cambiosPrecioMes: 0,
    cambiosPrecioTotal: 0,
  };

  const marcasOpciones = useMemo(
    () => [{ nombre: '' }, ...marcas.map((m) => ({ nombre: m.nombre }))],
    [marcas]
  );

  function filtrarPorMarca(nombre) {
    setMarcaSel(nombre);
    setTab(nombre ? 'historial' : tab);
    cargar({ marca: nombre });
  }

  async function confirmarRenombrar() {
    if (!renombrar || !marcaNueva.trim()) return;
    setError('');
    setOk('');
    try {
      const res = await marcasPreciosService.renombrarMarca(renombrar, marcaNueva.trim());
      setOk(`Marca actualizada en ${res.actualizados} producto(s).`);
      setRenombrar(null);
      setMarcaNueva('');
      await cargar({ marca: '' });
    } catch (err) {
      setError(mensajeError(err));
    }
  }

  function exportarHistorialCsv() {
    descargarCsv(archivoDocumento('Historial de precios', { desde, hasta }), [
      { key: 'fechaHora', label: 'Fecha y hora del cambio', format: (f) => fechaHora(f.fechaHora) },
      { key: 'productoCodigo', label: 'Código de producto', format: (f) => textoCelda(f.productoCodigo) },
      { key: 'productoNombre', label: 'Producto', format: (f) => textoCelda(f.productoNombre) },
      { key: 'marca', label: 'Marca', format: (f) => textoCelda(f.marca, 'Sin marca') },
      { key: 'tipoCambio', label: 'Tipo de cambio', format: (f) => etiquetaTipo(f.tipoCambio) },
      { key: 'valorAnterior', label: 'Valor anterior (C$)', format: (f) => textoCelda(f.valorAnterior) },
      { key: 'valorNuevo', label: 'Valor nuevo (C$)', format: (f) => textoCelda(f.valorNuevo) },
      { key: 'variacionPct', label: 'Variación porcentual', format: (f) => (f.variacionPct != null ? `${Number(f.variacionPct).toFixed(1)} %` : 'No aplica') },
      { key: 'usuarioNombre', label: 'Usuario que registró el cambio', format: (f) => textoCelda(f.usuarioNombre) },
      { key: 'detalle', label: 'Observación', format: (f) => textoCelda(f.detalle, 'Sin observación') },
    ], historial, {
      titulo: 'Historial de cambios de precios y costos',
      subtitulo: 'Trazabilidad de listas de precio, costo y venta',
      desde,
      hasta,
      filtros: [
        marcaSel && `Marca: ${marcaSel}`,
        tipoCambio && `Tipo de cambio: ${TIPOS_CAMBIO.find(([v]) => v === tipoCambio)?.[1] || tipoCambio}`,
      ].filter(Boolean).join(' · ') || 'Sin filtros adicionales',
    });
  }

  return (
    <section className="mpc-page">
      <header className="cfg-topbar mpc-topbar">
        <div>
          <p className="aud-kicker">Catálogo comercial</p>
          <h1>Marcas e historial de precios</h1>
          <p className="cfg-subtitle">
            Gestione marcas, consulte el historial de cambios de costo/venta y actualice listas de precios con trazabilidad en auditoría.
          </p>
        </div>
        <div className="cfg-topbar-actions">
          <Button type="button" variant="secondary" onClick={exportarHistorialCsv} disabled={!historial.length}>
            Exportar CSV
          </Button>
          <Button type="button" onClick={() => cargar()} disabled={cargando}>
            {cargando ? 'Actualizando…' : 'Actualizar'}
          </Button>
        </div>
      </header>

      {error ? <p className="auth-error" role="alert">{error}</p> : null}
      {ok ? <p className="ok-banner" role="status">{ok}</p> : null}

      <div className="cat-kpi-grid mpc-kpi-grid">
        <article className="cat-kpi">
          <span>Marcas</span>
          <strong>{kpis.marcasRegistradas}</strong>
          <small>{kpis.productosConMarca} productos etiquetados</small>
        </article>
        <article className="cat-kpi">
          <span>Sin marca</span>
          <strong>{kpis.productosSinMarca}</strong>
          <small>Productos por clasificar</small>
        </article>
        <article className="cat-kpi">
          <span>Cambios del mes</span>
          <strong>{kpis.cambiosPrecioMes}</strong>
          <small>Eventos CAMBIO_PRECIO</small>
        </article>
        <article className="cat-kpi">
          <span>Historial total</span>
          <strong>{kpis.cambiosPrecioTotal}</strong>
          <small>Registros acumulados</small>
        </article>
      </div>

      <article className="card mpc-filters-card">
        <div className="mpc-filter-row">
          <label>
            Buscar
            <input
              type="search"
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              placeholder="Producto, código, marca…"
            />
          </label>
          <label>
            Desde
            <input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} />
          </label>
          <label>
            Hasta
            <input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} />
          </label>
          <CatalogFilterSelect value={marcaSel} onChange={setMarcaSel} ariaLabel="Marca">
            <option value="">Marca: todas</option>
            {marcasOpciones.filter((m) => m.nombre).map((m) => (
              <option key={m.nombre} value={m.nombre}>{m.nombre}</option>
            ))}
          </CatalogFilterSelect>
          <CatalogFilterSelect value={tipoCambio} onChange={setTipoCambio} ariaLabel="Tipo">
            {TIPOS_CAMBIO.map(([valor, etiqueta]) => (
              <option key={valor || 'todos'} value={valor}>{etiqueta}</option>
            ))}
          </CatalogFilterSelect>
          <label className="mpc-check">
            <input type="checkbox" checked={soloActivos} onChange={(e) => setSoloActivos(e.target.checked)} />
            Solo activos
          </label>
          <Button type="button" onClick={() => cargar()}>Aplicar filtros</Button>
        </div>
      </article>

      <CatalogTabs tabs={TABS} active={tab} onChange={(valor) => { setTab(valor); cargar({ tab: valor }); }} />

      {cargando ? <p className="placeholder">Cargando…</p> : null}

      {!cargando && tab === 'marcas' ? (
        <article className="card mpc-table-card">
          <header className="aud-table-header">
            <h3>Marcas registradas</h3>
            <span>{marcas.length} marca{marcas.length === 1 ? '' : 's'}</span>
          </header>
          <div className="aud-table-wrap">
            <table className="data-table aud-table">
              <thead>
                <tr>
                  <th>Marca</th>
                  <th>Productos</th>
                  <th>Activos</th>
                  <th>Compra prom.</th>
                  <th>Venta prom.</th>
                  <th>Margen prom.</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {marcas.map((marca) => (
                  <tr key={marca.nombre}>
                    <td><strong>{marca.nombre}</strong></td>
                    <td>{marca.productosTotal}</td>
                    <td>{marca.productosActivos}</td>
                    <td>{dinero(marca.precioCompraPromedio)}</td>
                    <td>{dinero(marca.precioVentaPromedio)}</td>
                    <td>{Number(marca.margenPromedioPct || 0).toFixed(1)}%</td>
                    <td>
                      <div className="mpc-row-actions">
                        <button type="button" className="aud-ver-btn" onClick={() => filtrarPorMarca(marca.nombre)}>
                          Ver historial
                        </button>
                        <button type="button" className="aud-ver-btn" onClick={() => { setRenombrar(marca.nombre); setMarcaNueva(marca.nombre); }}>
                          Renombrar
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!marcas.length ? <p className="placeholder">No hay marcas con los filtros actuales.</p> : null}
        </article>
      ) : null}

      {!cargando && tab === 'historial' ? (
        <article className="card mpc-table-card">
          <header className="aud-table-header">
            <h3>Historial de precios y costos</h3>
            <span>{historial.length} evento{historial.length === 1 ? '' : 's'}</span>
          </header>
          <div className="aud-table-wrap">
            <table className="data-table aud-table">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Producto</th>
                  <th>Marca</th>
                  <th>Tipo</th>
                  <th>Antes</th>
                  <th>Después</th>
                  <th>Δ%</th>
                  <th>Usuario</th>
                </tr>
              </thead>
              <tbody>
                {historial.map((item) => (
                  <tr key={item.id}>
                    <td>{fechaHora(item.fechaHora)}</td>
                    <td>
                      <div className="mpc-producto-cell">
                        <strong>{item.productoCodigo || '—'}</strong>
                        <small>{item.productoNombre}</small>
                      </div>
                    </td>
                    <td>{item.marca || '—'}</td>
                    <td><span className={chipTipo(item.tipoCambio)}>{etiquetaTipo(item.tipoCambio)}</span></td>
                    <td><code>{item.valorAnterior || '—'}</code></td>
                    <td><code>{item.valorNuevo || '—'}</code></td>
                    <td>{item.variacionPct != null ? `${Number(item.variacionPct).toFixed(1)}%` : '—'}</td>
                    <td>{item.usuarioNombre || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!historial.length ? <p className="placeholder">No hay cambios de precio en el rango seleccionado.</p> : null}
        </article>
      ) : null}

      {!cargando && tab === 'catalogo' ? (
        <article className="card mpc-table-card">
          <header className="aud-table-header">
            <h3>Catálogo de precios actual</h3>
            <span>{catalogo.length} producto{catalogo.length === 1 ? '' : 's'}</span>
          </header>
          <div className="aud-table-wrap">
            <table className="data-table aud-table">
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Producto</th>
                  <th>Marca</th>
                  <th>Compra</th>
                  <th>Venta</th>
                  <th>Mayorista</th>
                  <th>Margen</th>
                  <th>Último cambio</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {catalogo.map((item) => (
                  <tr key={item.id}>
                    <td>{item.codigo}</td>
                    <td>{item.nombre}</td>
                    <td>{item.marca || '—'}</td>
                    <td>{dinero(item.precioCompra)}</td>
                    <td>{dinero(item.precioVenta)}</td>
                    <td>{item.precioMayorista != null ? dinero(item.precioMayorista) : '—'}</td>
                    <td>{Number(item.margenPct || 0).toFixed(1)}%</td>
                    <td>{fechaHora(item.ultimoCambioPrecio)}</td>
                    <td>
                      <button type="button" className="aud-ver-btn" onClick={() => setEditando(item)}>Editar</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </article>
      ) : null}

      <Pagination
        pagina={pagina}
        totalPaginas={paginaMeta.totalPaginas}
        totalElementos={paginaMeta.totalElementos}
        tamano={paginaMeta.tamano}
        cargando={cargando}
        onChange={(nueva) => cargar({ tab }, nueva)}
      />

      <EditarPreciosModal
        producto={editando}
        onCerrar={() => setEditando(null)}
        onGuardado={() => {
          setOk('Precios actualizados. Revisa el historial para ver el registro.');
          cargar();
        }}
      />

      <Modal
        open={Boolean(renombrar)}
        title="Renombrar marca"
        subtitle={renombrar || ''}
        onClose={() => setRenombrar(null)}
        size="sm"
        footer={(
          <>
            <Button type="button" variant="secondary" onClick={() => setRenombrar(null)}>Cancelar</Button>
            <Button type="submit" form="form-renombrar-marca" disabled={!marcaNueva.trim()}>Guardar</Button>
          </>
        )}
      >
        <form
          id="form-renombrar-marca"
          className="close-modal-form"
          onSubmit={(e) => { e.preventDefault(); confirmarRenombrar(); }}
        >
          <p className="close-modal-lead">
            El nombre se aplica a todos los productos con la marca <strong>{renombrar}</strong>.
          </p>
          <label>
            Nuevo nombre
            <input value={marcaNueva} onChange={(e) => setMarcaNueva(e.target.value)} required />
          </label>
        </form>
      </Modal>
    </section>
  );
}

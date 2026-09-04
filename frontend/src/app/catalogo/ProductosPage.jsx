import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../../auth/AuthContext';
import { mensajeError } from '../../auth/AuthContext';
import { PERMISOS } from '../../auth/permisos';
import CatalogToolbar, { CatalogFilterSelect, CatalogTabs } from '../../components/catalogo/CatalogToolbar';
import Pagination from '../../components/ui/Pagination';
import EmptyState from '../../components/ui/EmptyState';
import Icon from '../../components/ui/Icon';
import EliminarProductoModal from '../../components/productos/EliminarProductoModal';
import PresentacionModal from '../../components/productos/PresentacionModal';
import ProductoCatalogoGrid from '../../components/productos/ProductoCatalogoGrid';
import ProductoCatalogoTabla from '../../components/productos/ProductoCatalogoTabla';
import ProductoDetalleModal from '../../components/productos/ProductoDetalleModal';
import ProductoFormModal from '../../components/productos/ProductoFormModal';
import { categoriaService } from '../../services/categoriaService';
import { productoService } from '../../services/productoService';
import { dineroCorto } from '../../utils/formato';
import { contenidoPagina, listarTodos, metaPagina, TAMANO_PAGINA_DEFAULT } from '../../utils/paginaUtil';

function parseActivoFiltro(valor) {
  if (valor === 'ACTIVOS' || valor === true) return true;
  if (valor === 'INACTIVOS' || valor === false) return false;
  return undefined;
}

function IconoAlerta() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <path d="M12 9v4M12 17h.01M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
    </svg>
  );
}

function IconoEstado() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <circle cx="12" cy="12" r="8" />
      <path d="M8 12h8" />
    </svg>
  );
}

export default function ProductosPage() {
  const { tieneRol, tienePermiso } = useAuth();
  const esAdmin = tieneRol('ADMIN');
  const puedeGestionar = tienePermiso(PERMISOS.PRODUCTOS_GESTIONAR);

  const [productos, setProductos] = useState([]);
  const [categorias, setCategorias] = useState([]);
  const [seleccionado, setSeleccionado] = useState(null);
  const [busqueda, setBusqueda] = useState('');
  const [categoriaId, setCategoriaId] = useState('');
  const [filtroActivo, setFiltroActivo] = useState('');
  const [filtroAlerta, setFiltroAlerta] = useState('');
  const [tab, setTab] = useState('gestion');
  const [modalForm, setModalForm] = useState(false);
  const [modalDetalle, setModalDetalle] = useState(false);
  const [modalPresentacion, setModalPresentacion] = useState(false);
  const [modalEliminar, setModalEliminar] = useState(false);
  const [editando, setEditando] = useState(null);
  const [presentacionEditando, setPresentacionEditando] = useState(null);
  const [errorForm, setErrorForm] = useState('');
  const [errorPresentacion, setErrorPresentacion] = useState('');
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [pagina, setPagina] = useState(0);
  const [paginaMeta, setPaginaMeta] = useState({
    totalElementos: 0, totalPaginas: 1, tamano: TAMANO_PAGINA_DEFAULT,
  });
  const [vista, setVista] = useState(() => localStorage.getItem('productos-vista') || 'grid');
  const [totalAlertas, setTotalAlertas] = useState(0);

  /** El KPI de alertas cuenta todo el catálogo, no la página visible. */
  async function cargarTotalAlertas() {
    try {
      const respuesta = await productoService.listar({
        activo: true, nivelAlerta: 'ALERTA', pagina: 0, tamano: 1,
      });
      setTotalAlertas(metaPagina(respuesta).totalElementos ?? 0);
    } catch {
      setTotalAlertas(0);
    }
  }

  async function cargar(params = {}, paginaDestino = 0) {
    setCargando(true);
    setError('');
    try {
      const activo = params.activo !== undefined ? parseActivoFiltro(params.activo) : parseActivoFiltro(filtroActivo);
      const esTabAlertas = (params.tab ?? tab) === 'alertas';
      const alertaElegida = (params.nivelAlerta ?? filtroAlerta) || undefined;
      const respuesta = await productoService.listar({
        busqueda: params.busqueda ?? busqueda,
        categoriaId: (params.categoriaId ?? categoriaId) || undefined,
        activo,
        // En la pestaña de alertas el filtro lo resuelve el servidor: si se filtrara
        // en cliente, solo se verían las alertas de la página cargada.
        nivelAlerta: esTabAlertas ? (alertaElegida || 'ALERTA') : alertaElegida,
        pagina: paginaDestino,
        tamano: TAMANO_PAGINA_DEFAULT,
      });
      setProductos(contenidoPagina(respuesta));
      setPaginaMeta(metaPagina(respuesta));
      setPagina(paginaDestino);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    Promise.all([
      cargar(),
      cargarTotalAlertas(),
      listarTodos((p) => categoriaService.listar(p)).then(setCategorias).catch(() => setCategorias([])),
    ]);
  }, []);

  const resumen = useMemo(() => {
    const activos = productos.filter((p) => p.activo);
    const valoracion = productos.reduce(
      (s, p) => s + Number(p.stockActual || 0) * Number(p.precioCompra || 0),
      0
    );
    return {
      total: paginaMeta.totalElementos,
      activos: activos.length,
      alertas: totalAlertas,
      valoracion,
      categorias: categorias.filter((c) => c.activo).length,
    };
  }, [productos, categorias, paginaMeta.totalElementos, totalAlertas]);

  const filtrosActivos = useMemo(() => {
    let n = 0;
    if (busqueda.trim()) n += 1;
    if (categoriaId) n += 1;
    if (filtroActivo) n += 1;
    if (filtroAlerta) n += 1;
    if (tab === 'alertas') n += 1;
    return n;
  }, [busqueda, categoriaId, filtroActivo, filtroAlerta, tab]);

  async function ver(producto) {
    setError('');
    try {
      setSeleccionado(await productoService.obtener(producto.id));
    } catch {
      setSeleccionado(producto);
    }
    setModalDetalle(true);
  }

  function aplicarBusqueda() {
    cargar({ busqueda, categoriaId, activo: filtroActivo, nivelAlerta: filtroAlerta }, 0);
  }

  /** Los filtros se aplican al elegirlos: esperar un submit hacía creer que no funcionaban. */
  function cambiarCategoria(valor) {
    setCategoriaId(valor);
    cargar({ categoriaId: valor }, 0);
  }

  function cambiarFiltroActivo(valor) {
    setFiltroActivo(valor);
    cargar({ activo: valor }, 0);
  }

  function cambiarFiltroAlerta(valor) {
    setFiltroAlerta(valor);
    cargar({ nivelAlerta: valor }, 0);
  }

  function limpiarFiltros() {
    setBusqueda('');
    setCategoriaId('');
    setFiltroActivo('');
    setFiltroAlerta('');
    setTab('gestion');
    cargar({ busqueda: '', categoriaId: '', activo: '', nivelAlerta: '', tab: 'gestion' }, 0);
  }

  function cambiarTab(nuevaTab) {
    setTab(nuevaTab);
    if (nuevaTab === 'catalogo') {
      setFiltroActivo('ACTIVOS');
      setFiltroAlerta('');
      cargar({ busqueda, categoriaId, activo: 'ACTIVOS', nivelAlerta: '', tab: nuevaTab }, 0);
    } else if (nuevaTab === 'alertas') {
      setFiltroActivo('ACTIVOS');
      cargar({ busqueda, categoriaId, activo: 'ACTIVOS', nivelAlerta: '', tab: nuevaTab }, 0);
    } else {
      cargar({ busqueda, categoriaId, activo: filtroActivo, nivelAlerta: filtroAlerta, tab: nuevaTab }, 0);
    }
  }

  const esCatalogo = tab === 'catalogo';
  const esGestion = tab === 'gestion';

  async function abrirEditar(producto) {
    setErrorForm('');
    setGuardando(true);
    try {
      setEditando(await productoService.obtener(producto.id));
      setModalForm(true);
    } catch (err) {
      setErrorForm(mensajeError(err));
      setModalForm(true);
      setEditando(producto);
    } finally {
      setGuardando(false);
    }
  }

  async function guardarProducto(payload) {
    if (!puedeGestionar) {
      setErrorForm('No tiene permiso para gestionar productos.');
      return;
    }
    setGuardando(true);
    setError('');
    setErrorForm('');
    setOk('');
    const eraEdicion = Boolean(editando);
    try {
      const body = eraEdicion
        ? {
          codigo: editando.codigo,
          nombre: editando.nombre,
          marca: editando.marca,
          urlImagen: editando.urlImagen,
          categoriaId: editando.categoriaId,
          unidadMinima: editando.unidadMinima,
          precioCompra: editando.precioCompra,
          precioVenta: editando.precioVenta,
          stockMinimo: editando.stockMinimo,
          stockCritico: editando.stockCritico,
          fechaVencimiento: editando.fechaVencimiento,
          esAlcoholico: editando.esAlcoholico,
          activo: editando.activo,
          ...payload,
        }
        : payload;
      const guardado = eraEdicion
        ? await productoService.actualizar(editando.id, body)
        : await productoService.crear(body);
      setModalForm(false);
      setEditando(null);
      setSeleccionado(guardado);
      setOk(eraEdicion ? 'Producto actualizado.' : 'Producto creado.');
      await Promise.all([cargar({}, pagina), cargarTotalAlertas()]);
    } catch (err) {
      setErrorForm(mensajeError(err));
    } finally {
      setGuardando(false);
    }
  }

  async function guardarPresentacion(payload) {
    if (!seleccionado?.id) {
      setErrorPresentacion('Seleccione un producto antes de agregar la presentación.');
      return;
    }
    if (!puedeGestionar) {
      setErrorPresentacion('No tiene permiso para gestionar productos.');
      return;
    }
    setGuardando(true);
    setError('');
    setErrorPresentacion('');
    try {
      if (payload.id) {
        await productoService.actualizarPresentacion(seleccionado.id, payload.id, payload);
      } else {
        await productoService.agregarPresentacion(seleccionado.id, payload);
      }
      setModalPresentacion(false);
      setPresentacionEditando(null);
      setOk(payload.id ? 'Presentación actualizada.' : 'Presentación agregada.');
      const actualizado = await productoService.obtener(seleccionado.id);
      setSeleccionado(actualizado);
      await cargar({}, pagina);
    } catch (err) {
      setErrorPresentacion(mensajeError(err));
    } finally {
      setGuardando(false);
    }
  }

  function abrirNuevaPresentacion(producto) {
    setSeleccionado(producto);
    setPresentacionEditando(null);
    setErrorPresentacion('');
    setModalDetalle(false);
    setModalPresentacion(true);
  }

  function abrirEditarPresentacion(presentacion) {
    setPresentacionEditando(presentacion);
    setErrorPresentacion('');
    setModalDetalle(false);
    setModalPresentacion(true);
  }

  function cambiarVista(nuevaVista) {
    setVista(nuevaVista);
    localStorage.setItem('productos-vista', nuevaVista);
  }

  async function confirmarEliminar() {
    if (!seleccionado) return;
    setGuardando(true);
    setError('');
    try {
      await productoService.eliminar(seleccionado.id);
      setModalEliminar(false);
      setSeleccionado(null);
      setOk('Producto eliminado.');
      await cargar();
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <section className="productos-page">
      <header className="page-header fac-header">
        <div>
          <h1>{esCatalogo ? 'Catálogo comercial' : 'Catálogo de productos'}</h1>
          <p>
            {esCatalogo
              ? 'Consulta de productos activos para venta. Sin edición ni altas desde esta vista.'
              : 'Consola de almacén para productos, existencias mínimas, precios y disponibilidad para venta.'}
          </p>
        </div>
        {puedeGestionar && esGestion ? (
          <button type="button" className="btn primary" onClick={() => { setEditando(null); setErrorForm(''); setModalForm(true); }}>
            + Nuevo producto
          </button>
        ) : null}
      </header>

      {error ? <p className="pos-alert" role="alert">{error}</p> : null}
      {ok ? <p className="ok-banner" role="status">{ok}</p> : null}

      <div className="cat-kpi-grid">
        {!esCatalogo ? (
          <>
            <article className="cat-kpi">
              <span>Catálogo único</span>
              <strong>{resumen.total}</strong>
              <small>SKUs registrados</small>
            </article>
            <article className="cat-kpi">
              <span>Valoración</span>
              <strong>{dineroCorto(resumen.valoracion)}</strong>
              <small>Stock × costo</small>
            </article>
            <article className="cat-kpi">
              <span>Alertas de stock</span>
              <strong>{resumen.alertas}</strong>
              <small>Mínimo o crítico</small>
            </article>
            <article className="cat-kpi">
              <span>Categorías</span>
              <strong>{resumen.categorias}</strong>
              <small>Activas en catálogo</small>
            </article>
          </>
        ) : (
          <article className="cat-kpi">
            <span>Productos activos</span>
            <strong>{resumen.total}</strong>
            <small>Disponibles para consulta comercial</small>
          </article>
        )}
      </div>

      <CatalogTabs
        active={tab}
        onChange={cambiarTab}
        tabs={[
          { id: 'gestion', label: 'Gestión de productos' },
          { id: 'catalogo', label: 'Catálogo comercial' },
          { id: 'alertas', label: 'Alertas de stock bajo' },
        ]}
      />

      <CatalogToolbar
        searchValue={busqueda}
        onSearchChange={setBusqueda}
        onSubmit={aplicarBusqueda}
        searchPlaceholder="Buscar por nombre o código…"
        activeCount={filtrosActivos}
        onClearFilters={limpiarFiltros}
      >
        <CatalogFilterSelect
          ariaLabel="Categoría"
          value={categoriaId}
          onChange={cambiarCategoria}
        >
          <option value="">Todas las categorías</option>
          {categorias.map((c) => (
            <option key={c.id} value={c.id}>{c.nombre}</option>
          ))}
        </CatalogFilterSelect>

        {!esCatalogo ? (
          <>
            <CatalogFilterSelect
              ariaLabel="Estado"
              icon={<IconoEstado />}
              value={filtroActivo}
              onChange={cambiarFiltroActivo}
            >
              <option value="">Todos los estados</option>
              <option value="ACTIVOS">Solo activos</option>
              <option value="INACTIVOS">Solo inactivos</option>
            </CatalogFilterSelect>

            <CatalogFilterSelect
              ariaLabel="Alerta de stock"
              icon={<IconoAlerta />}
              value={filtroAlerta}
              onChange={cambiarFiltroAlerta}
            >
              <option value="">Todas las alertas</option>
              <option value="ALERTA">Mínimo o crítico</option>
              <option value="CRITICO">Crítico</option>
              <option value="MINIMO">Mínimo</option>
              <option value="OK">Sin alerta</option>
            </CatalogFilterSelect>
          </>
        ) : null}
      </CatalogToolbar>

      <article className="card">
        <div className="prod-list-head">
          <h3>
            {tab === 'alertas' ? 'Productos en alerta' : esCatalogo ? 'Vitrina comercial' : 'Inventario'}
          </h3>
          <div className="prod-view-toggle" role="tablist" aria-label="Vista de productos">
            <button
              type="button"
              role="tab"
              aria-selected={vista === 'grid'}
              className={`prod-view-btn${vista === 'grid' ? ' active' : ''}`}
              title="Vista en tarjetas"
              onClick={() => cambiarVista('grid')}
            >
              <Icon name="grid" size={16} strokeWidth={2} />
              Tarjetas
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={vista === 'table'}
              className={`prod-view-btn${vista === 'table' ? ' active' : ''}`}
              title="Vista en tabla"
              onClick={() => cambiarVista('table')}
            >
              <Icon name="list" size={16} strokeWidth={2} />
              Tabla
            </button>
          </div>
        </div>
        {cargando ? <p className="placeholder">Cargando…</p> : !productos.length ? (
          <EmptyState
            icon="package"
            title={tab === 'alertas' ? 'Sin alertas de stock' : 'Sin productos'}
            message={tab === 'alertas'
              ? 'Ningún producto activo está por debajo de su mínimo o crítico.'
              : 'No hay productos con los filtros actuales.'}
          />
        ) : vista === 'grid' ? (
          <ProductoCatalogoGrid productos={productos} onVer={ver} modoCatalogo={esCatalogo} />
        ) : (
          <ProductoCatalogoTabla productos={productos} onVer={ver} modoCatalogo={esCatalogo} />
        )}
        <Pagination
          pagina={pagina}
          totalPaginas={paginaMeta.totalPaginas}
          totalElementos={paginaMeta.totalElementos}
          tamano={paginaMeta.tamano}
          cargando={cargando}
          onChange={(nueva) => cargar({}, nueva)}
        />
      </article>

      <ProductoDetalleModal
        open={modalDetalle}
        producto={seleccionado}
        esAdmin={esAdmin}
        soloCatalogo={esCatalogo}
        puedeGestionar={puedeGestionar}
        onClose={() => setModalDetalle(false)}
        onEditar={abrirEditar}
        onPresentacion={abrirNuevaPresentacion}
        onEditarPresentacion={abrirEditarPresentacion}
        onEliminar={() => setModalEliminar(true)}
      />

      <ProductoFormModal
        open={modalForm}
        producto={editando}
        categorias={categorias}
        guardando={guardando}
        errorApi={errorForm}
        onClose={() => { setModalForm(false); setEditando(null); setErrorForm(''); }}
        onConfirmar={guardarProducto}
      />
      <PresentacionModal
        open={modalPresentacion}
        producto={seleccionado}
        presentacion={presentacionEditando}
        guardando={guardando}
        errorApi={errorPresentacion}
        onClose={() => { setModalPresentacion(false); setPresentacionEditando(null); setErrorPresentacion(''); }}
        onConfirmar={guardarPresentacion}
      />
      <EliminarProductoModal
        open={modalEliminar}
        producto={seleccionado}
        guardando={guardando}
        onClose={() => setModalEliminar(false)}
        onConfirmar={confirmarEliminar}
      />
    </section>
  );
}

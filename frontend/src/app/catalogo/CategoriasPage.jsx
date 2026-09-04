import { useEffect, useMemo, useState } from 'react';
import { mensajeError } from '../../auth/AuthContext';
import CatalogToolbar, { CatalogFilterSelect, CatalogTabs } from '../../components/catalogo/CatalogToolbar';
import CategoriaFormModal from '../../components/catalogo/CategoriaFormModal';
import { categoriaService } from '../../services/categoriaService';
import Pagination from '../../components/ui/Pagination';
import { contenidoPagina, metaPagina, TAMANO_PAGINA_DEFAULT } from '../../utils/paginaUtil';

function IconoEstado() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <circle cx="12" cy="12" r="8" />
      <path d="M8 12h8" />
    </svg>
  );
}

export default function CategoriasPage() {
  const [categorias, setCategorias] = useState([]);
  const [busqueda, setBusqueda] = useState('');
  const [filtroActivo, setFiltroActivo] = useState('');
  const [tab, setTab] = useState('todas');
  const [seleccionada, setSeleccionada] = useState(null);
  const [modalForm, setModalForm] = useState(false);
  const [editando, setEditando] = useState(null);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [pagina, setPagina] = useState(0);
  const [paginaMeta, setPaginaMeta] = useState({
    totalElementos: 0, totalPaginas: 1, tamano: TAMANO_PAGINA_DEFAULT,
  });

  function activoFiltro(tabActual = tab, estado = filtroActivo) {
    if (tabActual === 'activas' || estado === 'ACTIVAS') return true;
    if (tabActual === 'inactivas' || estado === 'INACTIVAS') return false;
    return undefined;
  }

  async function cargar(params = {}, paginaDestino = 0) {
    setCargando(true);
    try {
      const respuesta = await categoriaService.listar({
        busqueda: params.busqueda ?? busqueda,
        activo: params.activo !== undefined ? params.activo : activoFiltro(params.tab ?? tab, params.filtroActivo ?? filtroActivo),
        pagina: paginaDestino,
        tamano: TAMANO_PAGINA_DEFAULT,
      });
      setCategorias(contenidoPagina(respuesta));
      setPaginaMeta(metaPagina(respuesta));
      setPagina(paginaDestino);
      setError('');
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar();
  }, []);

  const categoriasVista = categorias;

  const resumen = useMemo(() => ({
    total: paginaMeta.totalElementos,
    activas: categorias.filter((c) => c.activo).length,
    inactivas: categorias.filter((c) => !c.activo).length,
  }), [categorias, paginaMeta.totalElementos]);

  const filtrosActivos = useMemo(() => {
    let n = 0;
    if (busqueda.trim()) n += 1;
    if (filtroActivo) n += 1;
    if (tab !== 'todas') n += 1;
    return n;
  }, [busqueda, filtroActivo, tab]);

  function limpiarFiltros() {
    setBusqueda('');
    setFiltroActivo('');
    setTab('todas');
    cargar({ busqueda: '', filtroActivo: '', tab: 'todas', activo: null });
  }

  async function guardarCategoria(payload) {
    setGuardando(true);
    setError('');
    setOk('');
    try {
      const guardada = editando
        ? await categoriaService.actualizar(editando.id, payload)
        : await categoriaService.crear(payload);
      setModalForm(false);
      setEditando(null);
      setSeleccionada(guardada);
      setOk(editando ? 'Categoría actualizada.' : 'Categoría creada.');
      await cargar();
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <section className="categorias-page">
      <header className="page-header fac-header">
        <div>
          <h1>Categorías del catálogo</h1>
          <p>Clasifique licores, cervezas, vinos y productos sin alcohol para filtrar el inventario.</p>
        </div>
        <button type="button" className="btn primary" onClick={() => { setEditando(null); setModalForm(true); }}>
          + Nueva categoría
        </button>
      </header>

      {error ? <p className="pos-alert" role="alert">{error}</p> : null}
      {ok ? <p className="ok-banner" role="status">{ok}</p> : null}

      <div className="cat-kpi-grid">
        <article className="cat-kpi">
          <span>Total</span>
          <strong>{resumen.total}</strong>
          <small>Categorías registradas</small>
        </article>
        <article className="cat-kpi">
          <span>Activas</span>
          <strong>{resumen.activas}</strong>
          <small>Disponibles en catálogo</small>
        </article>
        <article className="cat-kpi">
          <span>Inactivas</span>
          <strong>{resumen.inactivas}</strong>
          <small>Ocultas al asignar</small>
        </article>
        <article className="cat-kpi">
          <span>En vista</span>
          <strong>{categoriasVista.length}</strong>
          <small>Resultado filtrado</small>
        </article>
      </div>

      <CatalogTabs
        active={tab}
        onChange={(valor) => { setTab(valor); cargar({ tab: valor }); }}
        tabs={[
          { id: 'todas', label: 'Todas las categorías' },
          { id: 'activas', label: 'Solo activas' },
          { id: 'inactivas', label: 'Inactivas' },
        ]}
      />

      <CatalogToolbar
        searchValue={busqueda}
        onSearchChange={setBusqueda}
        onSubmit={() => cargar()}
        searchPlaceholder="Buscar categoría por nombre o descripción…"
        activeCount={filtrosActivos}
        onClearFilters={limpiarFiltros}
      >
        <CatalogFilterSelect
          ariaLabel="Estado"
          icon={<IconoEstado />}
          value={filtroActivo}
          onChange={(valor) => { setFiltroActivo(valor); cargar({ filtroActivo: valor }); }}
        >
          <option value="">Todos los estados</option>
          <option value="ACTIVAS">Solo activas</option>
          <option value="INACTIVAS">Solo inactivas</option>
        </CatalogFilterSelect>
      </CatalogToolbar>

      <article className="fac-list-panel card">
        <h3>Listado de categorías</h3>
        {cargando ? <p className="placeholder">Cargando…</p> : !categoriasVista.length ? (
          <p className="placeholder">Sin categorías con los filtros actuales.</p>
        ) : (
          <div className="fac-table-wrap">
            <table className="data-table fac-table cat-table">
              <thead>
                <tr>
                  <th>Categoría</th>
                  <th>Descripción</th>
                  <th>Estado</th>
                  <th>Gestión</th>
                </tr>
              </thead>
              <tbody>
                {categoriasVista.map((categoria) => (
                  <tr
                    key={categoria.id}
                    className={seleccionada?.id === categoria.id ? 'fac-row-active' : ''}
                    onClick={() => setSeleccionada(categoria)}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(e) => { if (e.key === 'Enter') setSeleccionada(categoria); }}
                  >
                    <td>
                      <div className="cat-product-cell">
                        <strong>{categoria.nombre}</strong>
                        <span className="cat-code">ID {categoria.id}</span>
                      </div>
                    </td>
                    <td>{categoria.descripcion || '—'}</td>
                    <td>
                      <span className={`cat-badge${categoria.activo ? ' cat-badge-ok' : ' cat-badge-muted'}`}>
                        {categoria.activo ? 'Activa' : 'Inactiva'}
                      </span>
                    </td>
                    <td>
                      <button
                        type="button"
                        className="chip-btn"
                        onClick={(e) => {
                          e.stopPropagation();
                          setEditando(categoria);
                          setModalForm(true);
                        }}
                      >
                        Editar
                      </button>
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
          onChange={(nueva) => cargar({}, nueva)}
        />
      </article>

      <CategoriaFormModal
        open={modalForm}
        categoria={editando}
        guardando={guardando}
        onClose={() => { setModalForm(false); setEditando(null); }}
        onConfirmar={guardarCategoria}
      />
    </section>
  );
}

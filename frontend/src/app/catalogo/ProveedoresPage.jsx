import { useEffect, useMemo, useState } from 'react';
import { mensajeError } from '../../auth/AuthContext';
import PrecioCatalogoModal from '../../components/proveedores/PrecioCatalogoModal';
import ProveedorDetalleModal from '../../components/proveedores/ProveedorDetalleModal';
import ProveedorFormModal from '../../components/proveedores/ProveedorFormModal';
import { productoService } from '../../services/productoService';
import { proveedorService } from '../../services/proveedorService';
import { listarTodos, contenidoPagina, metaPagina, TAMANO_PAGINA_DEFAULT } from '../../utils/paginaUtil';
import Pagination from '../../components/ui/Pagination';

function parseActivoFiltro(valor) {
  if (valor === 'ACTIVOS' || valor === true) return true;
  if (valor === 'INACTIVOS' || valor === false) return false;
  return undefined;
}

export default function ProveedoresPage() {
  const [proveedores, setProveedores] = useState([]);
  const [productos, setProductos] = useState([]);
  const [precios, setPrecios] = useState([]);
  const [paginaPrecios, setPaginaPrecios] = useState(0);
  const [paginaMetaPrecios, setPaginaMetaPrecios] = useState({
    totalElementos: 0, totalPaginas: 1, tamano: TAMANO_PAGINA_DEFAULT,
  });
  const [seleccionado, setSeleccionado] = useState(null);
  const [busqueda, setBusqueda] = useState('');
  const [filtroActivo, setFiltroActivo] = useState('ACTIVOS');
  const [modalForm, setModalForm] = useState(false);
  const [modalDetalle, setModalDetalle] = useState(false);
  const [modalPrecio, setModalPrecio] = useState(false);
  const [editando, setEditando] = useState(null);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [pagina, setPagina] = useState(0);
  const [paginaMeta, setPaginaMeta] = useState({
    totalElementos: 0, totalPaginas: 1, tamano: TAMANO_PAGINA_DEFAULT,
  });

  async function cargar(params = {}, paginaDestino = 0) {
    setCargando(true);
    setError('');
    try {
      const activo = params.activo !== undefined ? parseActivoFiltro(params.activo) : parseActivoFiltro(filtroActivo);
      const respuesta = await proveedorService.listar({
        busqueda: params.busqueda ?? busqueda,
        activo,
        pagina: paginaDestino,
        tamano: TAMANO_PAGINA_DEFAULT,
      });
      setProveedores(contenidoPagina(respuesta));
      setPaginaMeta(metaPagina(respuesta));
      setPagina(paginaDestino);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  }

  async function cargarPrecios(proveedorId, paginaDestino = 0) {
    try {
      const respuesta = await proveedorService.listarPrecios(proveedorId, false, {
        pagina: paginaDestino,
        tamano: TAMANO_PAGINA_DEFAULT,
      });
      setPrecios(contenidoPagina(respuesta));
      setPaginaMetaPrecios(metaPagina(respuesta));
      setPaginaPrecios(paginaDestino);
    } catch {
      setPrecios([]);
    }
  }

  useEffect(() => {
    cargar();
    listarTodos((p) => productoService.listar({ activo: true, ...p }))
      .then(setProductos)
      .catch(() => setProductos([]));
  }, []);

  const resumen = useMemo(() => {
    const activos = proveedores.filter((p) => p.activo);
    const conPrecios = proveedores.filter((p) => (p.cantidadPrecios || 0) > 0);
    return { total: paginaMeta.totalElementos, activos: activos.length, conPrecios: conPrecios.length };
  }, [proveedores, paginaMeta.totalElementos]);

  async function ver(proveedor) {
    setError('');
    try {
      const detalle = await proveedorService.obtener(proveedor.id);
      setSeleccionado(detalle);
      await cargarPrecios(proveedor.id);
    } catch {
      setSeleccionado(proveedor);
      await cargarPrecios(proveedor.id);
    }
    setModalDetalle(true);
  }

  function buscar(event) {
    event.preventDefault();
    cargar({ busqueda, activo: filtroActivo });
  }

  async function guardarProveedor(payload) {
    setGuardando(true);
    setError('');
    setOk('');
    try {
      const guardado = editando
        ? await proveedorService.actualizar(editando.id, payload)
        : await proveedorService.crear(payload);
      setModalForm(false);
      setEditando(null);
      setSeleccionado(guardado);
      setOk(editando ? 'Proveedor actualizado.' : 'Proveedor creado.');
      await cargar();
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setGuardando(false);
    }
  }

  async function guardarPrecio(payload) {
    if (!seleccionado) return;
    setGuardando(true);
    setError('');
    try {
      await proveedorService.guardarPrecio(seleccionado.id, payload);
      setModalPrecio(false);
      setOk('Precio agregado al catálogo.');
      await Promise.all([cargarPrecios(seleccionado.id), cargar()]);
      setSeleccionado(await proveedorService.obtener(seleccionado.id));
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <section className="proveedores-page">
      <header className="page-header fac-header">
        <div>
          <h1>Proveedores</h1>
          <p>Quienes fijan precios de compra. El catálogo alimenta las recepciones en Inventario → Compras.</p>
        </div>
        <button type="button" className="btn primary" onClick={() => { setEditando(null); setModalForm(true); }}>
          Nuevo proveedor
        </button>
      </header>

      {error ? <p className="pos-alert" role="alert">{error}</p> : null}
      {ok ? <p className="ok-banner" role="status">{ok}</p> : null}

      <div className="fac-stats">
        <article className="pos-stat accent">
          <span>Proveedores</span>
          <strong>{resumen.total}</strong>
          <small>{resumen.activos} activos</small>
        </article>
        <article className="pos-stat">
          <span>Con catálogo</span>
          <strong>{resumen.conPrecios}</strong>
          <small>Precios para compras</small>
        </article>
      </div>

      <form className="fac-filters" onSubmit={buscar}>
        <label>
          Buscar
          <input value={busqueda} onChange={(e) => setBusqueda(e.target.value)} placeholder="Nombre, RUC o contacto" />
        </label>
        <div className="chips-row">
          {['ACTIVOS', 'TODOS', 'INACTIVOS'].map((opcion) => (
            <button
              key={opcion}
              type="button"
              className={`chip-btn${filtroActivo === opcion ? ' active' : ''}`}
              onClick={() => { setFiltroActivo(opcion); cargar({ activo: opcion }); }}
            >
              {opcion === 'TODOS' ? 'Todos' : opcion === 'ACTIVOS' ? 'Activos' : 'Inactivos'}
            </button>
          ))}
        </div>
        <button type="submit" className="btn secondary">Filtrar</button>
      </form>

      <article className="card">
        <h3>Listado</h3>
        {cargando ? <p className="placeholder">Cargando…</p> : !proveedores.length ? (
          <p className="placeholder">Sin proveedores. Cree uno para registrar compras con precios acordados.</p>
        ) : (
          <div className="fac-table-wrap">
            <table className="data-table fac-table">
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th>RUC</th>
                  <th>Precios</th>
                  <th>Estado</th>
                  <th className="no-print">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {proveedores.map((proveedor) => (
                  <tr key={proveedor.id}>
                    <td>{proveedor.nombre}</td>
                    <td>{proveedor.documento || '—'}</td>
                    <td>{proveedor.cantidadPrecios ?? 0}</td>
                    <td>
                      <span className={`fac-estado${proveedor.activo ? ' fac-estado-ok' : ''}`}>
                        {proveedor.activo ? 'Activo' : 'Inactivo'}
                      </span>
                    </td>
                    <td className="no-print">
                      <button type="button" className="btn link" onClick={() => ver(proveedor)}>
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
          pagina={pagina}
          totalPaginas={paginaMeta.totalPaginas}
          totalElementos={paginaMeta.totalElementos}
          tamano={paginaMeta.tamano}
          cargando={cargando}
          onChange={(nueva) => cargar({}, nueva)}
        />
      </article>

      <ProveedorDetalleModal
        open={modalDetalle}
        proveedor={seleccionado}
        precios={precios}
        paginaPrecios={paginaPrecios}
        paginaMetaPrecios={paginaMetaPrecios}
        onPaginaPrecios={(nueva) => seleccionado && cargarPrecios(seleccionado.id, nueva)}
        onClose={() => setModalDetalle(false)}
        onEditar={(p) => { setEditando(p); setModalForm(true); }}
        onPrecio={() => setModalPrecio(true)}
      />

      <ProveedorFormModal
        open={modalForm}
        proveedor={editando}
        guardando={guardando}
        onClose={() => { setModalForm(false); setEditando(null); }}
        onConfirmar={guardarProveedor}
      />
      <PrecioCatalogoModal
        open={modalPrecio}
        proveedor={seleccionado}
        productos={productos}
        guardando={guardando}
        onClose={() => setModalPrecio(false)}
        onConfirmar={guardarPrecio}
      />
    </section>
  );
}
